package me.clogged.data.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SyncMethod {
    MANUAL("Manual"),
    AUTO("Automatic");

    private final String name;

    @Override
    public String toString() {
        return name;
    }
}
