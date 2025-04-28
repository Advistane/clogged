package me.clogged.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Represents a user's progress in the collection log
// This tracks which specific items a user has obtained.
public class UserCollectionLog {
    private final Map<Integer, Set<Integer>> subCategoryItemIds; // Map subcategory ID to a set of item IDs

    public UserCollectionLog() {
        this.subCategoryItemIds = new HashMap<>();
    }

    public Map<Integer, Set<Integer>> getSubCategoryItemIds() {
        return new HashMap<>(subCategoryItemIds); // Return a defensive copy
    }

    public void markItemAsObtained(int subcategoryId, int itemId) {
        if (itemId <= 0) {
            throw new IllegalArgumentException("Item ID must be positive.");
        }

        this.subCategoryItemIds.computeIfAbsent(subcategoryId, k -> new HashSet<>()).add(itemId);
    }
}
