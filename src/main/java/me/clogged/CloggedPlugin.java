package me.clogged;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

import okhttp3.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final String MISSING_KEYWORD = "missing";

    private final UserCollectionLog userCollectionLog = new UserCollectionLog();
    private final Map<Integer, Integer> loadedCollectionLogIcons = new HashMap<>();

    private int ticksToWait = 0;
    private int collectionLogScriptFiredTick = -1;
    private boolean collectionLogInterfaceOpenedAndSynced = false;
    AliasHelper aliasHelper;

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
        if (config.enableLookup()) {
            cloggedApiClient.getKCAliases(kcAliasesCallback);
        }
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

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (aliasHelper == null && event.getGroup().equals("clogged")) {
            if (Objects.equals(event.getKey(), "enableLookup") && config.enableLookup()) {
                cloggedApiClient.getKCAliases(kcAliasesCallback);
            }
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
        int quantity = (int) args[2];
        if (itemId > 0) {
            try {
                userCollectionLog.markItemAsObtained(itemId, quantity);
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
        if (commandArg.equals("sync") && Objects.equals(Text.sanitize(chatMessage.getName()), client.getLocalPlayer().getName())) {
            clientThread.invoke(this::updateUserCollectionLog);
        } else if (commandArg.equals("help")) {
            clientThread.invoke(this::showHelpMessage);
        } else if (commandArg.startsWith("join")) {
            handleGroupCommand(chatMessage, commandArg.trim(), true);
        } else if (commandArg.startsWith("leave")) {
            handleGroupCommand(chatMessage, commandArg.trim(), false);
        } else {
            handleSubcategoryLookup(chatMessage, commandArg.trim());
        }
    }

    private void handleGroupCommand(ChatMessage chatMessage, String commandArg, boolean join) {
        clientThread.invoke(() -> {
            String username = Text.sanitize(chatMessage.getName());
            if (!username.equalsIgnoreCase(client.getLocalPlayer().getName())) {
                return;
            }

            if (!config.enableSync()) {
                showSyncingDisabledMessage();
                return;
            }

            if (config.proxyEnabled() && (config.proxyHost() == null || config.proxyHost().isEmpty() || config.proxyPort() <= 0)) {
                showProxySettingsIncompleteMessage();
                return;
            }

            if (join && !config.profileVisibility()) {
                showProfileVisibilityDisabledMessage();
                return;
            }

            String[] parts = commandArg.split(" ", 2);
            if (parts.length > 1) {
                String groupName = parts[1].trim();
                if (!groupName.isEmpty()) {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("accountHash", client.getAccountHash());
                    cloggedApiClient.handleGroup(jsonObject.toString(), groupName, join, handleGroupCallback(chatMessage));
                } else {
                    showIncorrectGroupCommandMessage();
                }
            } else {
                showIncorrectGroupCommandMessage();
            }
        });
    }

    private void handleSubcategoryLookup(ChatMessage chatMessage, String commandArg) {
        clientThread.invokeLater(() -> {
            if (!config.enableLookup()) {
                showLookupDisabledMessage();
                return;
            }

            if (config.proxyEnabled() && (config.proxyHost() == null || config.proxyHost().isEmpty() || config.proxyPort() <= 0)) {
                showProxySettingsIncompleteMessage();
                return;
            }

            boolean isOtherLookup = false;
            String username = Text.sanitize(chatMessage.getName());
            String query = commandArg;

            if (chatMessage.getType().equals(ChatMessageType.PRIVATECHATOUT))
            {
                username = client.getLocalPlayer().getName();
            }

            String otherPlayerName = getStringBetweenQuotes(query);
            if (otherPlayerName != null) {
                username = Text.sanitize(otherPlayerName);
                isOtherLookup = true;
                query = query.replace("\"" + otherPlayerName + "\"", "").trim();
            }

            // Check if the user wants to see missing items or not
            boolean isMissingSearch = isMissingSearch(query);
            String bossName = query;
            if (isMissingSearch) {
                if (!config.showMissing()) {
                    return;
                }

                bossName = query.substring(MISSING_KEYWORD.length()).trim();
            }

            cloggedApiClient.getUserCollectionLog(username, bossName, isMissingSearch, isOtherLookup, subcategoryLookupCallback(chatMessage, isMissingSearch, isOtherLookup));
        });
    }

    private static boolean isMissingSearch(String commandArg) {
        return commandArg.toLowerCase().startsWith(MISSING_KEYWORD + " ");
    }

    // Check if the command argument contains a string between quotes (a username to lookup)
    public String getStringBetweenQuotes(String commandArg) {
        Pattern pattern = Pattern.compile("\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(commandArg);
        if (matcher.find()) {
            return matcher.group(1); // Return the string inside the quotes
        }
        return null; // Return null if no match is found
    }

    private Callback handleGroupCallback(ChatMessage chatMessage) {
        return new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                log.error("Failed to join group: {}", e.getMessage());
                showLookupFailedMessage();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                assert response.body() != null;
                String responseBody = response.body().string();
                GroupResponse groupResponse = gson.fromJson(responseBody, GroupResponse.class);

                clientThread.invoke(() -> {
                    String message = groupResponse.getMessage();
                    updateChatMessage(chatMessage, message);
                });

                response.close();
            }
        };
    }

    private Callback subcategoryLookupCallback(ChatMessage chatMessage, boolean isMissingSearch, boolean isOtherLookup) {
        return new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                log.error("Failed to get user collection log: {}", e.getMessage());
                showLookupFailedMessage();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                assert response.body() != null;
                String responseBody = response.body().string();
                if (response.code() == 404) {
                    boolean isOriginalSender = chatMessage.getType().equals(ChatMessageType.PRIVATECHATOUT) || Objects.equals(Text.sanitize(chatMessage.getName()), client.getLocalPlayer().getName());
                    if (isOriginalSender) {
                        clientThread.invoke(() -> {
                            updateChatMessage(chatMessage, responseBody);
                        });
                    }
                    return;
                }

                if (!response.isSuccessful()) {
                    response.close();
                    return;
                }

                CollectionLogLookupResponse lookupResponse = gson.fromJson(responseBody, CollectionLogLookupResponse.class);

                clientThread.invoke(() -> {
                    String replacementMessage = buildReplacementMessage(lookupResponse, isMissingSearch, isOtherLookup, lookupResponse.getTotal());
                    if (replacementMessage != null) {
                        updateChatMessage(chatMessage, replacementMessage);
                    }
                });
            }
        };
    }

    Callback kcAliasesCallback = new Callback() {
        @Override
        public void onFailure(@NonNull Call call, IOException e) {
            log.error("Failed to fetch KC aliases: {}", e.getMessage());
        }

        @Override
        public void onResponse(@NonNull Call call, Response response) throws IOException {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                List<KCAliasResponse> kcAliases = Arrays.asList(gson.fromJson(responseBody, KCAliasResponse[].class));
                aliasHelper = new AliasHelper(kcAliases);
            } else {
                // Handle unsuccessful response
                log.error("Failed to fetch KC aliases: {}", response.body());
            }
        }
    };

    private void updateChatMessage(ChatMessage chatMessage, String text) {
        String message = getFormattedMessage(text);
        chatMessage.getMessageNode().setValue(message);
        client.refreshChat();
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

        populateUserCollectionLogKcs();

        // Trigger search to load all items
        client.menuAction(-1, CLOG_SEARCH_WIDGET_ID, MenuAction.CC_OP, 1, -1, "Search", null);
        client.runScript(2240);
    }

    private void syncCollectionLog() {
        showSyncingMessage();
        String userDataJson = createUserCollectionLogJson();
        cloggedApiClient.updateUserCollectionLog(userDataJson, createUploadCallback());
    }

    private String createUserCollectionLogJson() {
        Map<String, Object> userDataMap = new HashMap<>();
        userDataMap.put("username", client.getLocalPlayer().getName());
        userDataMap.put("accountHash", client.getAccountHash());
        userDataMap.put("profileVisible", config.profileVisibility());
        userDataMap.put("collectedItems", userCollectionLog.getItemJson());
        userDataMap.put("subcategories", userCollectionLog.getSubcategoryJson());
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

    private String getFormattedMessage(String message) {
        if (!config.enableCustomColor()) {
            return message;
        }

        return ColorUtil.wrapWithColorTag(message, config.textColor());
    }

    private void showIncorrectGroupCommandMessage() {
        String message = getFormattedMessage("Clogged: Incorrect command. Please use '!clog join group_name' to join a group.");
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage(message)
                .build());
    }

    private void showProfileVisibilityDisabledMessage() {
        String message = getFormattedMessage("Clogged: 'Make profile visible on Clogged.me' must be enabled to join a group.");
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage(message)
                .build());
    }

    private void showHelpMessage() {
        String message = getFormattedMessage("Ensure all plugin options are configured to your liking and open the collection log interface to sync. If method is set to manual, you must type '!clog sync' with the interface open.");
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage(message)
                .build());
    }

    private void showSyncingDisabledMessage() {
        String message = getFormattedMessage("Clogged: Syncing is disabled in the plugin options.");
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage(message)
                .build());
    }

    private void showLookupDisabledMessage() {
        String message = getFormattedMessage("Clogged: Lookups are disabled in the plugin options.");
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage(message)
                .build());
    }

    private void showProxySettingsIncompleteMessage() {
        String message = getFormattedMessage("Clogged: Proxy settings are incomplete. Please check the plugin settings.");
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage(message)
                .build());
    }

    private void showCollectionLogClosedMessage() {
        String message = getFormattedMessage("Clogged: The collection log interface must be open to sync.");
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage(message)
                .build());
    }

    private void showSyncingMessage() {
        String message = getFormattedMessage("Clogged: Syncing collection log...");
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage(message)
                .build());
    }

    private void showSyncSuccessMessage() {
        String message = getFormattedMessage("Clogged: Collection log synced successfully.");
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage(message)
                .build());
    }

    private void showSyncFailedMessage() {
        String message = getFormattedMessage("Clogged: Failed to sync collection log. Try again or reach out to Advistane on Discord.");
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage(message)
                .build());
    }

    private void showLookupFailedMessage() {
        String message = getFormattedMessage("Clogged: Failed to lookup collection log. Try again or reach out to Advistane on Discord.");
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage(message)
                .build());
    }

    private String buildReplacementMessage(CollectionLogLookupResponse response, boolean isMissingSearch, boolean isOtherLookup, int total) {
        int kc = response.getKc();
        String subcategoryName = response.getSubcategoryName();
        List<CollectionLogItem> items = response.getItems();

        if (items.isEmpty()) {
            return null;
        }

        StringBuilder messageBuilder = new StringBuilder();
        if (isOtherLookup) {
            messageBuilder.append(response.getUsername()).append(" - ");
        }

        messageBuilder.append(subcategoryName);

        if (kc > 0 || (config.showMissing() && isMissingSearch)) {
            messageBuilder.append(" (");
            if (kc > 0) {
                messageBuilder.append(kc).append(" KC");
                if (config.showMissing() && isMissingSearch) {
                    messageBuilder.append(", Missing");
                } else if (config.showTotal() && !isMissingSearch) {
                    messageBuilder.append(", ").append(items.size()).append("/").append(total);
                }
            } else { // kc is 0, but we are showing missing for a missing search
                messageBuilder.append("Missing");
            }
            messageBuilder.append("): ");
        } else {
            messageBuilder.append(": ");
        }

        if (config.displayMethod() == DisplayMethod.ICON) {
            loadClogIcons(items);
        }

        appendItems(messageBuilder, items);
        return messageBuilder.toString();
    }

    // Appends items to the message builder based on the display method
    private void appendItems(StringBuilder builder, List<CollectionLogItem> items) {
        for (CollectionLogItem item : items) {
            String itemName = client.getItemDefinition(item.getItemId()).getName();

            if (config.displayMethod() == DisplayMethod.TEXT) {
                builder.append(itemName);
                if (!addItemQuantity(builder, item.getQuantity(), true)) {
                    builder.append(", ");
                }

                continue;
            }

            try {
                builder.append("<img=").append(loadedCollectionLogIcons.get(item.getItemId())).append(">");
                if (!addItemQuantity(builder, item.getQuantity(), false)) {
                    builder.append(", ");
                }

            } catch (NullPointerException e) {
                log.warn("Failed to load icon for item ID: {}", item.getItemId());
                builder.append(itemName);
                if (!addItemQuantity(builder, item.getQuantity(), true)) {
                    builder.append(", ");
                }
            }
        }

        builder.setLength(builder.length() - 2);
    }

    private boolean addItemQuantity(StringBuilder builder, int quantity, boolean leadingSpace) {
        if (config.showQuantity() && quantity > 1) {
            if (leadingSpace) {
                builder.append(" ");
            }
            builder.append("x").append(quantity).append(", ");
            return true;
        }
        return false;
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
            final IndexedSprite sprite = createSpriteForItem(item.getItemId());
            final int spriteIndex = modIconIdx + i;

            newModIcons[spriteIndex] = sprite;
            loadedCollectionLogIcons.put(item.getItemId(), spriteIndex);
        }

        client.setModIcons(newModIcons);
    }

    private List<CollectionLogItem> filterItemsToLoad(List<CollectionLogItem> collectionLogItems) {
        return collectionLogItems.stream()
                .filter(item -> !loadedCollectionLogIcons.containsKey(item.getItemId()))
                .collect(Collectors.toList());
    }

    private IndexedSprite createSpriteForItem(int itemId) {
        final ItemComposition itemComposition = itemManager.getItemComposition(itemId);
        final BufferedImage itemImage = itemManager.getImage(itemComposition.getId());
        final BufferedImage croppedImage = cropImageToContent(itemImage);
        final BufferedImage resizedImage = ImageUtil.resizeImage(croppedImage != null ? croppedImage : itemImage, 18, 15);

        return ImageUtil.getImageIndexedSprite(resizedImage, client);
    }

    // Crops an image to its content while maintaining the aspect ratio
    private BufferedImage cropImageToContent(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();
        int border = 1; // Add a border of 1 pixel

        // Step 1: Detect content bounds
        int top = height, left = width, right = 0, bottom = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = src.getRGB(x, y);
                if ((pixel >> 24) != 0x00) { // Not transparent
                    if (x < left) left = x;
                    if (x > right) right = x;
                    if (y < top) top = y;
                    if (y > bottom) bottom = y;
                }
            }
        }

        if (left > right || top > bottom) {
            // No visible content
            return null;
        }

        // Step 2: Add border
        left = Math.max(0, left - border);
        right = Math.min(width - 1, right + border);
        top = Math.max(0, top - border);
        bottom = Math.min(height - 1, bottom + border);

        int contentWidth = right - left + 1;
        int contentHeight = bottom - top + 1;

        // Step 3: Adjust to maintain original aspect ratio
        float originalAspect = (float) width / height;
        float contentAspect = (float) contentWidth / contentHeight;

        int newWidth = contentWidth;
        int newHeight = contentHeight;

        if (contentAspect > originalAspect) {
            // Too wide — pad height
            newHeight = Math.round(contentWidth / originalAspect);
        } else {
            // Too tall — pad width
            newWidth = Math.round(contentHeight * originalAspect);
        }

        // Center the new crop box
        int cx = left + contentWidth / 2;
        int cy = top + contentHeight / 2;

        int newLeft = Math.max(0, cx - newWidth / 2);
        int newTop = Math.max(0, cy - newHeight / 2);

        // Clamp to image size
        newLeft = Math.min(newLeft, width - newWidth);
        newTop = Math.min(newTop, height - newHeight);

        // Step 4: Crop and return
        return src.getSubimage(newLeft, newTop, newWidth, newHeight);
    }

    private boolean isSyncEnabled() {
        return config.enableSync();
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
    private void setSubcategoryKc(int subcategoryId) {
        StructComposition subcategoryStruct = client.getStructComposition(subcategoryId);
        String subcategoryName = subcategoryStruct.getStringValue(689);

        int kc = getSummedKcForBoss(subcategoryName);
        userCollectionLog.markSubcategoryAsObtained(subcategoryId, kc);
    }

    private void populateUserCollectionLogKcs() {
        Map<String, Integer> categoryEnumMap = createCategoryEnumMap();
        for (Map.Entry<String, Integer> entry : categoryEnumMap.entrySet()) {
            int enumId = entry.getValue();
            int[] subcategoryIds = client.getEnum(enumId).getIntVals();
            for (int subcategoryId : subcategoryIds) {
                setSubcategoryKc(subcategoryId);
            }
        }
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
                return getSimpleKcForBoss("Theatre of Blood") + getSimpleKcForBoss("Theatre of Blood Hard Mode");
            case "Tombs of Amascut":
                return getSimpleKcForBoss("Tombs of Amascut") + getSimpleKcForBoss("Tombs of Amascut Entry Mode") + getSimpleKcForBoss("Tombs of Amascut Expert Mode");
            default:
                return getSimpleKcForBoss(boss);
        }
    }

    private int getSimpleKcForBoss(String boss) {
        Integer killCount = configManager.getRSProfileConfiguration("killcount", aliasHelper.getFullNameByAlias(boss), int.class);
        return killCount != null ? killCount : -1;
    }
}