package me.clogged;

import com.google.gson.Gson;
import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import me.clogged.data.*;
import me.clogged.data.config.DisplayMethod;
import me.clogged.data.config.SyncMethod;
import me.clogged.helpers.AliasHelper;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

import okhttp3.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
        name = "Clogged.me",
        description = "Sync your collection log with Clogged.me and view other players' logs"
)
public class CloggedPlugin extends Plugin {
    private static final String COLLECTION_LOG_COMMAND_STRING = "!clog";
    private static final int CLOG_CONTAINER_WIDGET_ID = 40697943;
    private static final int CLOG_SEARCH_WIDGET_ID = 40697932;
    private static final int COLLECTION_LOG_SCRIPT_ID = 4100;
    private static final int TICKS_TO_WAIT_AFTER_LOAD = 2;

    private final CollectionLogStructure collectionLogStructure = new CollectionLogStructure();
    private final UserCollectionLog userCollectionLog = new UserCollectionLog();
    private final Map<Integer, Integer> loadedCollectionLogIcons = new HashMap<>();

    private int ticksToWait = 0;
    private int collectionLogScriptFiredTick = -1;
    private boolean structureCreated = false;
    private boolean collectionLogInterfaceOpenedAndSynced = false;

    @Inject private Client client;
    @Inject private CloggedConfig config;
    @Inject private ChatMessageManager chatMessageManager;
    @Inject private ChatCommandManager chatCommandManager;
    @Inject private ClientThread clientThread;
    @Inject private Gson gson;
    @Inject private ItemManager itemManager;
    @Inject private ConfigManager configManager;
    @Inject private CloggedApiClient cloggedApiClient;

    @Override
    protected void startUp() throws Exception {
        chatCommandManager.registerCommandAsync(COLLECTION_LOG_COMMAND_STRING, this::handleChatMessage);
    }

    @Override
    protected void shutDown() throws Exception {
        chatCommandManager.unregisterCommand(COLLECTION_LOG_COMMAND_STRING);
    }

    @Provides
    CloggedConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(CloggedConfig.class);
    }

    // Event Handling

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (!structureCreated && gameStateChanged.getGameState() == GameState.LOGGED_IN) {
            clientThread.invokeLater(this::createCollectionLogStructure);
        }
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded widgetLoaded) {
        if (!isSyncEnabled() || config.syncMethod() == SyncMethod.MANUAL) {
            return;
        }

        if (widgetLoaded.getGroupId() == InterfaceID.COLLECTION && !collectionLogInterfaceOpenedAndSynced) {
            ticksToWait = TICKS_TO_WAIT_AFTER_LOAD;
        }
    }

    @Subscribe
    public void onWidgetClosed(WidgetClosed widgetClosed) {
        if (widgetClosed.getGroupId() == InterfaceID.COLLECTION) {
            ticksToWait = 0;
            collectionLogInterfaceOpenedAndSynced = false;
        }
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        if (!isSyncEnabled()) {
            return;
        }

        // Wait for ticks after the collection log interface is opened
        if (ticksToWait > 0) {
            if (--ticksToWait == 0) {
                clientThread.invokeLater(this::updateUserCollectionLog);
            }
        }

        checkForCollectionLogScriptFired();
    }

    @Subscribe
    public void onScriptPreFired(ScriptPreFired preFired) {
        // Script 4100 is fired when the search interface in the collection log is opened
        if (preFired.getScriptId() == COLLECTION_LOG_SCRIPT_ID && !collectionLogInterfaceOpenedAndSynced) {
            recordScriptFiredTick();
            processScriptArguments(preFired.getScriptEvent().getArguments());
        }
    }

    private void recordScriptFiredTick() {
        collectionLogScriptFiredTick = client.getTickCount();
    }

    private void checkForCollectionLogScriptFired() {
        if (collectionLogScriptFiredTick != -1 && collectionLogScriptFiredTick + 2 > client.getTickCount() && !collectionLogInterfaceOpenedAndSynced) {
            collectionLogScriptFiredTick = -1;
            collectionLogInterfaceOpenedAndSynced = true;
            syncCollectionLog();
        }
    }

    private void processScriptArguments(Object[] args) {
        int itemId = (int) args[1];
        if (itemId > 0) {
            try {
                CollectionLogItem item = collectionLogStructure.findItemById(itemId);
                userCollectionLog.markItemAsObtained(item.getSubCategoryId(), itemId);
            } catch (NullPointerException e) {
                log.warn("Item ID {} not found in collection log structure", itemId);
            }
        }
    }

    // Chat Command Handling

    private void handleChatMessage(ChatMessage chatMessage, String message) {
        if (message.length() == COLLECTION_LOG_COMMAND_STRING.length()) {
            return;
        }

        String commandArg = message.substring(COLLECTION_LOG_COMMAND_STRING.length() + 1);

        if (commandArg.equals("sync")) {
            clientThread.invoke(this::updateUserCollectionLog);
        } else {
            handleSubCategoryLookup(chatMessage, commandArg);
        }
    }

    private void handleSubCategoryLookup(ChatMessage chatMessage, String commandArg) {
        clientThread.invoke(() -> {
            if (!config.enableLookup()) {
                showLookupDisabledMessage();
                return;
            }

            if (config.proxyEnabled() && (config.proxyHost() == null || config.proxyHost().isEmpty() || config.proxyPort() <= 0)) {
                showProxySettingsIncompleteMessage();
                return;
            }

            SubCategory subCategory = collectionLogStructure.findSubCategoryByName(AliasHelper.ClogAlias(commandArg));
            if (subCategory == null) {
                return;
            }

            String username = Text.sanitize(chatMessage.getName());
            if (chatMessage.getType().equals(ChatMessageType.PRIVATECHATOUT))
            {
                username = client.getLocalPlayer().getName();
            }

            int subCategoryId = subCategory.getId();
            cloggedApiClient.getUserCollectionLog(username, subCategoryId, createLookupCallback(subCategoryId, chatMessage));
        });
    }

    private Callback createLookupCallback(int subCategoryId, ChatMessage chatMessage) {
        return new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                log.error("Failed to get user collection log: {}", e.getMessage());
                showLookupFailedMessage();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    log.error("Failed to get user collection log: {}", response.message());
                    showLookupFailedMessage();
                    return;
                }

                assert response.body() != null;
                String responseBody = response.body().string();
                CollectionLogLookupResponse lookupResponse = gson.fromJson(responseBody, CollectionLogLookupResponse.class);

                clientThread.invoke(() -> {
                    String replacementMessage = buildReplacementMessage(lookupResponse, subCategoryId);
                    updateChatMessage(chatMessage, replacementMessage);
                });
            }
        };
    }

    private void updateChatMessage(ChatMessage chatMessage, String text) {
        chatMessage.getMessageNode().setValue(text);
        client.refreshChat();
    }

    // Collection Log Management

    private void createCollectionLogStructure() {
        Map<String, Category> categories = createCategories();
        Map<String, Integer> collectionLogCategoriesMap = createCategoryEnumMap();

        populateCategoriesFromEnums(categories, collectionLogCategoriesMap);

        // Add all categories to the structure
        for (Category category : categories.values()) {
            collectionLogStructure.addCategory(category);
        }
        structureCreated = true;
    }

    private Map<String, Category> createCategories() {
        return Map.of(
                "bosses", new Category("Bosses"),
                "raids", new Category("Raids"),
                "clues", new Category("Clues"),
                "minigames", new Category("Minigames"),
                "other", new Category("Other")
        );
    }

    private Map<String, Integer> createCategoryEnumMap() {
        return Map.of(
                "bosses", 2103,
                "raids", 2104,
                "clues", 2105,
                "minigames", 2106,
                "other", 2107
        );
    }

    private void populateCategoriesFromEnums(Map<String, Category> categories, Map<String, Integer> enumMap) {
        for (Map.Entry<String, Integer> entry : enumMap.entrySet()) {
            String categoryName = entry.getKey();
            int enumId = entry.getValue();
            Category category = categories.get(categoryName);

            int[] subcategoryIds = client.getEnum(enumId).getIntVals();
            for (int subcategoryId : subcategoryIds) {
                SubCategory subCategory = createSubCategoryFromStruct(subcategoryId);
                category.addSubCategory(subCategory);
            }
        }
    }

    private SubCategory createSubCategoryFromStruct(int subcategoryId) {
        StructComposition subcategoryStruct = client.getStructComposition(subcategoryId);
        String subcategoryName = subcategoryStruct.getStringValue(689);
        int[] items = client.getEnum(subcategoryStruct.getIntValue(690)).getIntVals();

        SubCategory subCategory = new SubCategory(
                subcategoryName,
                subcategoryId,
                new HashSet<>(),
                getSummedKcForBoss(subcategoryName)
        );

        for (int itemId : items) {
            CollectionLogItem item = new CollectionLogItem(
                    itemId,
                    client.getItemDefinition(itemId).getName(),
                    subcategoryId
            );
            subCategory.addItem(item);
        }

        // For whatever reason, these two item ids in the enum do not match
        // Special handling for Mahogany Homes
        if (subcategoryId == 1689) {
            subCategory.addItem(new CollectionLogItem(25629, "Plank sack", subcategoryId));
        }

        // Special handling for Motherlode Mine
        if (subcategoryId == 530) {
            subCategory.addItem(new CollectionLogItem(25627, "Coal bag", subcategoryId));
        }

        return subCategory;
    }

    private void updateUserCollectionLog() {
        if (!isSyncEnabled()) {
            showSyncingDisabledMessage();
            return;
        }

        Widget collectionLogContainer = client.getWidget(CLOG_CONTAINER_WIDGET_ID);
        if (collectionLogContainer == null) {
            showCollectionLogClosedMessage();
            return;
        }

        // Trigger search to load all items
        client.menuAction(-1, CLOG_SEARCH_WIDGET_ID, MenuAction.CC_OP, 1, -1, "Search", null);
        client.runScript(2240);
    }

    private void updateSubcategoryKcs() {
        for (Category category : collectionLogStructure.getCategories().values()) {
            for (SubCategory subCategory : category.getSubCategories().values()) {
                int kc = getSummedKcForBoss(subCategory.getName());
                if (kc < 1) {
                    continue;
                }

                subCategory.setKc(kc);
            }
        }
    }

    private void syncCollectionLog() {
        showSyncingMessage();
        updateSubcategoryKcs();
        String userDataJson = createUserCollectionLogJson();
        cloggedApiClient.updateUserCollectionLog(userDataJson, createUploadCallback());
    }

    private String createUserCollectionLogJson() {
        Map<String, Object> userDataMap = new HashMap<>();
        userDataMap.put("username", client.getLocalPlayer().getName());
        userDataMap.put("accountHash", client.getAccountHash());
        userDataMap.put("collectedIds", userCollectionLog.getSubCategoryItemIds());
        userDataMap.put("categories", collectionLogStructure.getCategoryJson());
        return gson.toJson(userDataMap);
    }

    private Callback createUploadCallback() {
        return new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                log.error("Unable to upload data to Clogged.me: {}", e.getMessage());
                showSyncFailedMessage();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                response.close();
                showSyncSuccessMessage();
            }
        };
    }

    // UI and Message Helpers

    private void showSyncingDisabledMessage() {
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage("Clogged: Syncing is disabled in the plugin options.")
                .build());
    }

    private void showLookupDisabledMessage() {
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage("Clogged: Lookups are disabled in the plugin options.")
                .build());
    }

    private void showProxySettingsIncompleteMessage() {
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage("Clogged: Use proxy is enabled but proxy settings are incomplete. Please check the plugin settings.")
                .build());
    }

    private void showCollectionLogClosedMessage() {
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage("Clogged: The collection log interface must be open to sync.")
                .build());
    }

    private void showSyncingMessage() {
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage("Clogged: Syncing collection log...")
                .build());
    }

    private void showSyncSuccessMessage() {
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage("Clogged: Collection log synced successfully.")
                .build());
    }

    private void showSyncFailedMessage() {
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage("Clogged: Failed to sync collection log. Try again or reach out to Advistane on Discord.")
                .build());
    }

    private void showLookupFailedMessage() {
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage("Clogged: Failed to lookup collection log. Try again or reach out to Advistane on Discord.")
                .build());
    }

    private String buildReplacementMessage(CollectionLogLookupResponse response, int subCategoryId) {
        int kc = response.getKc();
        SubCategory subCategory = collectionLogStructure.findSubCategoryById(subCategoryId);

        List<CollectionLogItem> obtainedItems = getObtainedItems(response);
        if (obtainedItems.isEmpty()) {
            return "No items found for input";
        }

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(subCategory.getName());

        if (kc > 0) {
            messageBuilder.append(" (").append(kc).append(" KC)");
        }
        messageBuilder.append(": ");

        if (config.displayMethod() == DisplayMethod.ICON) {
            loadClogIcons(obtainedItems);
        }

        appendItems(messageBuilder, obtainedItems);
        return messageBuilder.toString();
    }

    private List<CollectionLogItem> getObtainedItems(CollectionLogLookupResponse response) {
        List<CollectionLogItem> obtainedItems = new ArrayList<>();
        for (int itemId : response.getItems()) {
            CollectionLogItem item = collectionLogStructure.findItemById(itemId);
            if (item != null) {
                obtainedItems.add(item);
            }
        }
        return obtainedItems;
    }

    // Appends items to the message builder based on the display method
    private void appendItems(StringBuilder builder, List<CollectionLogItem> items) {
        for (CollectionLogItem item : items) {
            if (config.displayMethod() == DisplayMethod.TEXT) {
                builder.append(item.getName()).append(", ");
                continue;
            }

            try {
                String itemString = "<img=" + loadedCollectionLogIcons.get(item.getId()) + "> ";
                builder.append(itemString);
            } catch (NullPointerException e) {
                log.warn("Failed to load icon for item ID: {}", item.getId());
                builder.append(item.getName()).append(", ");
            }
        }

        // Remove the last comma and space
        if (config.displayMethod() == DisplayMethod.TEXT) {
            builder.setLength(builder.length() - 2);
        }
    }

    // Icon Management

    private void loadClogIcons(List<CollectionLogItem> collectionLogItems) {
        if (collectionLogItems.isEmpty()) {
            return;
        }

        List<CollectionLogItem> itemsToLoad = filterItemsToLoad(collectionLogItems);
        if (itemsToLoad.isEmpty()) {
            return;
        }

        final IndexedSprite[] modIcons = client.getModIcons();
        final IndexedSprite[] newModIcons = Arrays.copyOf(modIcons, modIcons.length + itemsToLoad.size());
        int modIconIdx = modIcons.length;

        for (int i = 0; i < itemsToLoad.size(); i++) {
            final CollectionLogItem item = itemsToLoad.get(i);
            final IndexedSprite sprite = createSpriteForItem(item.getId());
            final int spriteIndex = modIconIdx + i;

            newModIcons[spriteIndex] = sprite;
            loadedCollectionLogIcons.put(item.getId(), spriteIndex);
        }

        client.setModIcons(newModIcons);
    }

    private List<CollectionLogItem> filterItemsToLoad(List<CollectionLogItem> collectionLogItems) {
        return collectionLogItems.stream()
                .filter(item -> !loadedCollectionLogIcons.containsKey(item.getId()))
                .collect(Collectors.toList());
    }

    private IndexedSprite createSpriteForItem(int itemId) {
        final ItemComposition itemComposition = itemManager.getItemComposition(itemId);
        final BufferedImage image = ImageUtil.resizeImage(itemManager.getImage(itemComposition.getId()), 18, 15);
        return ImageUtil.getImageIndexedSprite(image, client);
    }

    private boolean isSyncEnabled() {
        return config.enableSync();
    }

    // Since the collection log doesn't separate bosses into their own subcategories, sum the kc of them
    private int getSummedKcForBoss(String boss) {
        switch (boss) {
            case "Daggonoth Kings":
                return getSimpleKcForBoss("Rex") + getSimpleKcForBoss("Prime") + getSimpleKcForBoss("Supreme");
            case "Callisto and Artio":
                return getSimpleKcForBoss("Callisto") + getSimpleKcForBoss("Artio");
            case "Chambers of Xeric":
                return getSimpleKcForBoss("Chambers of Xeric") + getSimpleKcForBoss("Chambers of Xeric challenge mode");
            case "Vet'ion and Calvar'ion":
                return getSimpleKcForBoss("Vet'ion") + getSimpleKcForBoss("Calvar'ion");
            case "Venenatis and Spindel":
                return getSimpleKcForBoss("Venenatis") + getSimpleKcForBoss("Spindel");
            case "The Gauntlet":
                return getSimpleKcForBoss("Gauntlet") + getSimpleKcForBoss("Corrupted Gauntlet");
            case "The Leviathan":
                return getSimpleKcForBoss("Leviathan") + getSimpleKcForBoss("Leviathan (awakened)");
            case "Vardovis":
                return getSimpleKcForBoss("Vardovis") + getSimpleKcForBoss("Vardovis (awakened)");
            case "The Whisperer":
                return getSimpleKcForBoss("Whisperer") + getSimpleKcForBoss("Whisperer (awakened)");
            case "The Nightmare":
                return getSimpleKcForBoss("Nightmare") + getSimpleKcForBoss("Phosani's Nightmare");
            case "Theatre of Blood":
                return getSimpleKcForBoss("Theatre of Blood") + getSimpleKcForBoss("Theatre of Blood Hard Mode") + getSimpleKcForBoss("Theatre of Blood Entry Mode");
            case "Tombs of Amascut":
                return getSimpleKcForBoss("Tombs of Amascut") + getSimpleKcForBoss("Tombs of Amascut Entry Mode") + getSimpleKcForBoss("Tombs of Amascut Expert Mode");
            default:
                return getSimpleKcForBoss(boss);
        }
    }

    private int getSimpleKcForBoss(String boss) {
        Integer killCount = configManager.getRSProfileConfiguration("killcount", AliasHelper.KCAlias(boss).toLowerCase(), int.class);
        return killCount != null ? killCount : -1;
    }
}