package me.clogged.data;

import lombok.Getter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

// Represents a subcategory within a category (e.g., "Barrows Chests", "Cerberus")
@Getter
public class SubCategory {
    private final String name;
    private final Set<CollectionLogItem> items; // The unique items that belong to this subcategory
    private int kc; // Kill count for this subcategory, if applicable
    private final int id; // Unique ID for the subcategory

    public SubCategory(String name, int id, Set<CollectionLogItem> items) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("SubCategory name cannot be null or empty.");
        }
        this.name = name.trim();
        this.id = id;
        this.items = new HashSet<>(items); // Use a copy to avoid external modifications
    }

    public SubCategory(String name, int id, Set<CollectionLogItem> items, int kc) {
        this(name, id, items);
        this.kc = kc;
    }

    public void addItem(CollectionLogItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null.");
        }
        this.items.add(item);
    }

    public void setKc(int kc) {
        if (kc < 0) {
            this.kc = -1;
            return;
        }

        this.kc = kc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubCategory that = (SubCategory) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "SubCategory{" +
                "name='" + name + '\'' +
                ", itemCount=" + items.size() +
                '}';
    }
}
