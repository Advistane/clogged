package me.clogged.data.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DisplayMethod {
    TEXT("Text"),
    ICON("Icons");

    private final String name;

    @Override
    public String toString() {
        return name;
    }
}
