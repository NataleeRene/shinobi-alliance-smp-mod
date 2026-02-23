package com.shinobi.shinobialliancemod;

public enum Village {
    // Leaf Village: &c[Rank] &f (red rank tags)
    LEAF("leaf", "§c", "§f", "Leaf"),
    
    // Sand Village: &7[&eRank&7] (gray brackets, yellow rank)
    SAND("sand", "§e", "§7", "Sand"),
    
    // Mist Village: &7[&9Rank&7] (gray brackets, dark blue rank)
    MIST("mist", "§9", "§7", "Mist"),
    
    // Stone Village: &7[&8Rank&7] (gray brackets, dark gray rank)
    STONE("stone", "§8", "§7", "Stone"),
    
    // Cloud Village: &f[Rank] &f (white everything)
    CLOUD("cloud", "§f", "§f", "Cloud");

    private final String id;
    private final String rankColor;      // Color of the rank text
    private final String bracketColor;   // Color of brackets
    private final String displayName;

    Village(String id, String rankColor, String bracketColor, String displayName) {
        this.id = id;
        this.rankColor = rankColor;
        this.bracketColor = bracketColor;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    /**
     * Get the full village prefix (with village tag)
     * This is ONLY used for chat/scoreboard village identification
     * NOT for rank display above heads
     */
    public String getVillagePrefix() {
        if (this == LEAF) {
            return "§c[Leaf] §f";
        } else if (this == CLOUD) {
            return "§f[Cloud] §f";
        } else {
            // Sand, Mist, Stone use gray brackets
            return bracketColor + "[" + rankColor + displayName + bracketColor + "] ";
        }
    }

    /**
     * Get rank prefix formatted for this village
     * Example: Leaf Genin -> §c[Genin] §f
     *          Sand Jonin -> §7[§eJonin§7]
     *          Leaf Kage -> §c[Hokage] §f
     */
    public String getRankPrefix(Rank rank) {
        String rankName = rank.getDisplayName(this);
        if (this == LEAF) {
            // Leaf: &c[Rank] &f
            return "§c[" + rankName + "] §f";
        } else if (this == CLOUD) {
            // Cloud: &f[Rank] &f
            return "§f[" + rankName + "] §f";
        } else {
            // Sand/Mist/Stone: &7[&xRank&7]
            return bracketColor + "[" + rankColor + rankName + bracketColor + "] ";
        }
    }

    public String getRankColor() {
        return rankColor;
    }

    public String getBracketColor() {
        return bracketColor;
    }
    
    /**
     * Get base color for this village (used for messages)
     */
    public String getColor() {
        return rankColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getKageGroup() {
        return id + "_kage";
    }

    /**
     * Get the village-specific Kage title
     * Leaf -> Hokage, Sand -> Kazekage, etc.
     */
    public String getKageName() {
        switch (this) {
            case LEAF: return "Hokage";
            case SAND: return "Kazekage";
            case MIST: return "Mizukage";
            case STONE: return "Tsuchikage";
            case CLOUD: return "Raikage";
            default: return "Kage";
        }
    }

    public static Village fromId(String id) {
        for (Village village : values()) {
            if (village.id.equalsIgnoreCase(id)) {
                return village;
            }
        }
        return null;
    }
}
