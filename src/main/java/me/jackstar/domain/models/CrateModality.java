package me.jackstar.drakescrates.domain.models;

import me.jackstar.drakescraft.utils.MessageUtils;
import net.kyori.adventure.text.Component;

import java.util.Locale;

public enum CrateModality {
    ALL("all", "<gray>[GLOBAL]</gray>", "Global"),
    CLASICO("clasico", "<green>[100% VANILLA]</green>", "Classic (Vanilla)"),
    SLIMEFUN("slimefun", "<light_purple>[SLIMEFUN & TECH]</light_purple>", "Slimefun & Tech");

    private final String id;
    private final String badgeTag;
    private final String displayName;

    CrateModality(String id, String badgeTag, String displayName) {
        this.id = id;
        this.badgeTag = badgeTag;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public Component getBadge() {
        return MessageUtils.parse(badgeTag);
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CrateModality fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("clasic") || normalized.contains("vanilla") || normalized.contains("vainilla")) {
            return CLASICO;
        }
        if (normalized.contains("sf") || normalized.contains("slime") || normalized.contains("tech")) {
            return SLIMEFUN;
        }
        return ALL;
    }
}
