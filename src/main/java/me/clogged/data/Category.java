package me.clogged.data;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// Represents a top-level category (e.g., "Bosses", "Raids")
@Getter
public class Category {
    private final String name;
    private final Map<String, SubCategory> subCategories; // Map subcategory name to SubCategory object

    public Category(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be null or empty.");
        }
        this.name = name.trim();
        this.subCategories = new HashMap<>();
    }

    public void addSubCategory(SubCategory subCategory) {
        if (subCategory == null) {
            throw new IllegalArgumentException("SubCategory cannot be null.");
        }

        this.subCategories.put(subCategory.getName(), subCategory);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(name, category.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Category{" +
                "name='" + name + '\'' +
                ", subCategoryCount=" + subCategories.size() +
                '}';
    }
}
