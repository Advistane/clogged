package me.clogged.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class UserCollectionLog {
    // Item Id : Quantity
    private final HashMap<Integer, Integer> items = new HashMap<>();

    // Subcategory Id : KC
    @Getter
    private final HashMap<Integer, Integer> kcs = new HashMap<>();

    public JsonArray getItemJson() {
        JsonArray json = new JsonArray();
        for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
            JsonObject item = new JsonObject();
            item.addProperty("id", entry.getKey());
            item.addProperty("quantity", entry.getValue());
            json.add(item);
        }
        return json;
    }

    public JsonArray getSubcategoryJson() {
        JsonArray json = new JsonArray();
        for (Map.Entry<Integer, Integer> entry : kcs.entrySet()) {
            JsonObject subcategory = new JsonObject();
            subcategory.addProperty("id", entry.getKey());
            subcategory.addProperty("kc", entry.getValue());
            json.add(subcategory);
        }
        return json;
    }

    public void markItemAsObtained(int itemId, int quantity) {
        if (itemId <= 0) {
            throw new IllegalArgumentException("Item ID must be positive.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }

        items.put(itemId, quantity);
    }

    public void markSubcategoryAsObtained(int subcategoryId, int kc) {
        if (subcategoryId <= 0) {
            throw new IllegalArgumentException("Subcategory ID must be positive.");
        }
        if (kc < 0) {
            return;
        }

        kcs.put(subcategoryId, kc);
    }
}
