package com.shinobi.shinobialliancemod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/**
 * Manages player achievement points using Minecraft scoreboard objectives
 */
public class PlayerPointsManager {
    
    private static final String POINTS_OBJECTIVE = "shinobi_points";
    private static final String RANK_OBJECTIVE = "shinobi_rank";

    /**
     * Initialize scoreboard objectives for tracking points and ranks
     */
    public static void initialize(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        
        // Create points objective if it doesn't exist
        if (scoreboard.getObjective(POINTS_OBJECTIVE) == null) {
            scoreboard.addObjective(
                POINTS_OBJECTIVE,
                ObjectiveCriteria.DUMMY,
                net.minecraft.network.chat.Component.literal("Shinobi Points"),
                ObjectiveCriteria.RenderType.INTEGER,
                true,
                null
            );
            System.out.println("[ShinobiAllianceMod] Created scoreboard objective: " + POINTS_OBJECTIVE);
        }

        // Create rank objective if it doesn't exist
        if (scoreboard.getObjective(RANK_OBJECTIVE) == null) {
            scoreboard.addObjective(
                RANK_OBJECTIVE,
                ObjectiveCriteria.DUMMY,
                net.minecraft.network.chat.Component.literal("Shinobi Rank"),
                ObjectiveCriteria.RenderType.INTEGER,
                true,
                null
            );
            System.out.println("[ShinobiAllianceMod] Created scoreboard objective: " + RANK_OBJECTIVE);
        }
    }

    /**
     * Get player's current points
     */
    public static int getPoints(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return 0;
        
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(POINTS_OBJECTIVE);
        
        if (objective == null) {
            return 0;
        }

        var scoreAccess = scoreboard.getPlayerScoreInfo(player, objective);
        if (scoreAccess == null) {
            return 0;
        }

        return scoreboard.getOrCreatePlayerScore(player, objective).get();
    }

    /**
     * Set player's points
     */
    public static void setPoints(ServerPlayer player, int points) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(POINTS_OBJECTIVE);
        
        if (objective == null) {
            initialize(server);
            objective = scoreboard.getObjective(POINTS_OBJECTIVE);
        }

        scoreboard.getOrCreatePlayerScore(player, objective).set(points);
        
        System.out.println("[ShinobiAllianceMod] Set " + player.getName().getString() + " points to " + points);
    }

    /**
     * Add points to player's total
     */
    public static int addPoints(ServerPlayer player, int pointsToAdd) {
        int currentPoints = getPoints(player);
        int newPoints = currentPoints + pointsToAdd;
        setPoints(player, newPoints);
        return newPoints;
    }

    /**
     * Get player's current rank (stored as ordinal)
     */
    public static Rank getRank(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return Rank.GENIN;
        
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(RANK_OBJECTIVE);
        
        if (objective == null) {
            return Rank.GENIN;
        }

        var scoreAccess = scoreboard.getPlayerScoreInfo(player, objective);
        if (scoreAccess == null) {
            return Rank.GENIN;
        }

        int rankOrdinal = scoreboard.getOrCreatePlayerScore(player, objective).get();
        
        try {
            return Rank.values()[rankOrdinal];
        } catch (ArrayIndexOutOfBoundsException e) {
            return Rank.GENIN;
        }
    }

    /**
     * Set player's rank (stores as ordinal for easy comparison)
     */
    public static void setRank(ServerPlayer player, Rank rank) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(RANK_OBJECTIVE);
        
        if (objective == null) {
            initialize(server);
            objective = scoreboard.getObjective(RANK_OBJECTIVE);
        }

        scoreboard.getOrCreatePlayerScore(player, objective).set(rank.ordinal());
        
        System.out.println("[ShinobiAllianceMod] Set " + player.getName().getString() + " rank to " + rank.getDisplayName());
    }

    /**
     * Calculate and return the rank that corresponds to a point value
     */
    public static Rank calculateRankFromPoints(int points) {
        return Rank.fromPoints(points);
    }

    /**
     * Recalculate a player's total points from completed advancements and update their rank.
     * Does not change point or rank logic; only recomputes totals using AdvancementPointSystem.
     */
    public static void recalcPointsForPlayer(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        int totalPoints = 0;

        for (var advancement : server.getAdvancements().getAllAdvancements()) {
            if (AdvancementPointSystem.isTracked(advancement)) {
                var progress = player.getAdvancements().getOrStartProgress(advancement);
                if (progress.isDone()) {
                    totalPoints += AdvancementPointSystem.getPoints(advancement);
                }
            }
        }

        // Store recomputed total
        setPoints(player, totalPoints);

        // Update rank based on new total
        RankManager.checkAndUpdateRank(player, server);
    }

    /**
     * Reset player's points and rank (useful for testing or village changes)
     */
    public static void reset(ServerPlayer player) {
        setPoints(player, 0);
        setRank(player, Rank.GENIN);
    }
    
    /**
     * Completely wipe player's advancement data from scoreboard.
     * This removes them from objectives entirely, not just setting to 0.
     * Use this for full resets where player should start completely fresh.
     */
    public static void completeWipe(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        
        Scoreboard scoreboard = server.getScoreboard();
        
        // Remove from points objective
        Objective pointsObj = scoreboard.getObjective(POINTS_OBJECTIVE);
        if (pointsObj != null) {
            scoreboard.resetSinglePlayerScore(player, pointsObj);
            System.out.println("[ShinobiAllianceMod] Wiped " + player.getName().getString() + " from shinobi_points objective");
        }
        
        // Remove from rank objective
        Objective rankObj = scoreboard.getObjective(RANK_OBJECTIVE);
        if (rankObj != null) {
            scoreboard.resetSinglePlayerScore(player, rankObj);
            System.out.println("[ShinobiAllianceMod] Wiped " + player.getName().getString() + " from shinobi_rank objective");
        }
    }
}
