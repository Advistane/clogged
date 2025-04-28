package me.clogged.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

// Represents the entire static collection log structure
// This defines ALL possible items in the log, organized by category and subcategory.
public class CollectionLogStructure {
    @Getter
    private final Map<String, Category> categories; // Map category name to Category object
    private final Map<Integer, CollectionLogItem> itemByIdMap; // Map item ID directly to Item object for quick lookup

    public CollectionLogStructure() {
        this.categories = new HashMap<>();
        this.itemByIdMap = new HashMap<>();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CollectionLogStructure:\n");
        sb.append("Total Categories: ").append(categories.size()).append("\n");
        sb.append("Total Items: ").append(itemByIdMap.size()).append("\n");
        sb.append("Categories:\n");
        for (Map.Entry<String, Category> entry : categories.entrySet()) {
            sb.append("  - ").append(entry.getKey()).append(":\n");
            for (SubCategory subCategory : entry.getValue().getSubCategories().values()) {
                sb.append("    * ").append(subCategory.getName()).append(": ").append(subCategory.getKc()).append(":\n");
                for (CollectionLogItem item : subCategory.getItems()) {
                    sb.append("      - ").append(item.getId()).append(": ").append(item.getName()).append("\n");
                }
            }
        }
        return sb.toString();
    }

    public void addCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null.");
        }
        // Add the category
        this.categories.put(category.getName(), category);

        // Add all items within this category and its subcategories to the flat item map
        for (SubCategory subCategory : category.getSubCategories().values()) {
            for (CollectionLogItem item : subCategory.getItems()) {
                if (!this.itemByIdMap.containsKey(item.getId())) {
                    this.itemByIdMap.put(item.getId(), item);
                }
            }
        }
    }

    public CollectionLogItem findItemById(int itemId) {
        return this.itemByIdMap.get(itemId);
    }

    public SubCategory findSubCategoryByName(String subCategoryName) {
        for (Category category : categories.values()) {
            for (SubCategory subCategory : category.getSubCategories().values()) {
                if (subCategory.getName().equalsIgnoreCase(subCategoryName)) {
                    return subCategory;
                }
            }
        }
        return null; // Not found
    }

    public SubCategory findSubCategoryById(int subCategoryId) {
        for (Category category : categories.values()) {
            for (SubCategory subCategory : category.getSubCategories().values()) {
                if (subCategory.getId() == subCategoryId) {
                    return subCategory;
                }
            }
        }
        return null; // Not found
    }

    public JsonArray getCategoryJson() {
        JsonArray categoriesArray = new JsonArray();

        for (Map.Entry<String, Category> categoryEntry : categories.entrySet()) {
            JsonObject categoryObject = new JsonObject();
            categoryObject.addProperty("categoryName", categoryEntry.getKey());

            JsonArray subCategoriesArray = getJsonElements(categoryEntry);

            categoryObject.add("subCategories", subCategoriesArray);
            categoriesArray.add(categoryObject);
        }

        return categoriesArray;
    }

    private static JsonArray getJsonElements(Map.Entry<String, Category> categoryEntry) {
        JsonArray subCategoriesArray = new JsonArray();
        for (SubCategory subCategory : categoryEntry.getValue().getSubCategories().values()) {
            JsonObject subCategoryObject = new JsonObject();
            subCategoryObject.addProperty("subcategoryName", subCategory.getName());
            subCategoryObject.addProperty("subcategoryId", subCategory.getId()); // Assuming SubCategory has an `id` field
            subCategoryObject.addProperty("kc", subCategory.getKc());
            subCategoriesArray.add(subCategoryObject);
        }
        return subCategoriesArray;
    }
}

