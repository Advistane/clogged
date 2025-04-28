package me.clogged.data;

import lombok.Getter;

import java.util.Objects;

// Represents a single unique item in the collection log
@Getter
public class CollectionLogItem {
    private final int id; // Unique ID for the item
    private final String name;
    private final int subCategoryId;

    public CollectionLogItem(int id, String name, int subCategory) {
        if (id <= 0) {
            throw new IllegalArgumentException("Item ID must be positive.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be null or empty.");
        }
        this.id = id;
        this.name = name.trim();
        this.subCategoryId = subCategory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CollectionLogItem item = (CollectionLogItem) o;
        return id == item.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Item{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
