package com.shinobi.shinobialliancemod;

public enum Rank {
    GENIN("Genin", "genin", 0, 49),
    CHUNIN("Chunin", "chunin", 50, 119),
    JONIN("Jonin", "jonin", 120, 179),
    ANBU("Anbu", "anbu", 180, 204),
    KAGE("Kage", "kage", 205, Integer.MAX_VALUE);

    private final String displayName;
    private final String id;
    private final int minPoints;
    private final int maxPoints;

    Rank(String displayName, String id, int minPoints, int maxPoints) {
        this.displayName = displayName;
        this.id = id;
        this.minPoints = minPoints;
        this.maxPoints = maxPoints;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get display name with village-specific Kage title if applicable
     */
    public String getDisplayName(Village village) {
        if (this == KAGE && village != null) {
            return village.getKageName();
        }
        return displayName;
    }

    public String getId() {
        return id;
    }

    public int getMinPoints() {
        return minPoints;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    /**
     * Get the LuckPerms group name for this rank
     */
    public String getLuckPermsGroup() {
        return id;
    }

    /**
     * Determine rank from total achievement points
     */
    public static Rank fromPoints(int points) {
        for (Rank rank : values()) {
            if (points >= rank.minPoints && points <= rank.maxPoints) {
                return rank;
            }
        }
        return GENIN; // Default to lowest rank
    }

    /**
     * Get rank from string ID
     */
    public static Rank fromId(String id) {
        for (Rank rank : values()) {
            if (rank.id.equalsIgnoreCase(id)) {
                return rank;
            }
        }
        return GENIN;
    }

    /**
     * Check if this rank is higher than another
     */
    public boolean isHigherThan(Rank other) {
        return this.ordinal() > other.ordinal();
    }

    /**
     * Get the next rank in progression, or null if already at max rank
     */
    public Rank getNextRank() {
        Rank[] ranks = values();
        int nextIndex = this.ordinal() + 1;
        if (nextIndex < ranks.length) {
            return ranks[nextIndex];
        }
        return null; // Already at max rank (Kage)
    }
}
