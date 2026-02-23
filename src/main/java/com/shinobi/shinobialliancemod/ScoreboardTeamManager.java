package com.shinobi.shinobialliancemod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public class ScoreboardTeamManager {
    private static boolean teamsInitialized = false;

    /**
     * Initialize all rank teams for all villages
     * Creates teams like: leaf_genin, leaf_chunin, etc.
     */
    public static void initializeTeams(MinecraftServer server) {
        if (teamsInitialized) return;

        Scoreboard scoreboard = server.getScoreboard();

        // Create a team for each village + rank combination
        for (Village village : Village.values()) {
            for (Rank rank : Rank.values()) {
                String teamName = village.getId() + "_" + rank.getId();
                PlayerTeam team = scoreboard.getPlayerTeam(teamName);
                
                if (team == null) {
                    team = scoreboard.addPlayerTeam(teamName);
                }

                // Set the rank prefix (NOT the village tag)
                String prefix = village.getRankPrefix(rank);
                team.setPlayerPrefix(Component.literal(prefix));
                
                // Enable name tag visibility
                team.setNameTagVisibility(PlayerTeam.Visibility.ALWAYS);
                
                // Set team color based on village
                ChatFormatting color = getColorFromVillage(village);
                team.setColor(color);

                System.out.println("[ShinobiAllianceMod] Initialized team: " + teamName + " with prefix " + prefix);
            }
        }

        teamsInitialized = true;
    }

    /**
     * Assign player to a rank-specific team
     * This is the NEW method that should be used for the achievement system
     */
    public static void assignPlayerRankTeam(ServerPlayer player, Village village, Rank rank, MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        String playerName = player.getScoreboardName();
        String teamName = village.getId() + "_" + rank.getId();

        // Remove from ALL teams first (all villages, all ranks)
        for (Village v : Village.values()) {
            for (Rank r : Rank.values()) {
                String oldTeamName = v.getId() + "_" + r.getId();
                PlayerTeam oldTeam = scoreboard.getPlayerTeam(oldTeamName);
                if (oldTeam != null && oldTeam.getPlayers().contains(playerName)) {
                    scoreboard.removePlayerFromTeam(playerName, oldTeam);
                }
            }
        }

        // Add to the new rank team
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            // Team doesn't exist yet, create it on the fly
            team = scoreboard.addPlayerTeam(teamName);
            team.setPlayerPrefix(Component.literal(village.getRankPrefix(rank)));
            team.setNameTagVisibility(PlayerTeam.Visibility.ALWAYS);
            team.setColor(getColorFromVillage(village));
        }

        scoreboard.addPlayerToTeam(playerName, team);
        System.out.println("[ShinobiAllianceMod] Assigned " + playerName + " to team " + teamName);
    }

    /**
     * DEPRECATED: Use assignPlayerRankTeam instead
     * Legacy method for old village-only system
     */
    @Deprecated
    public static void assignPlayerToTeam(ServerPlayer player, Village village, MinecraftServer server) {
        // Default to Genin rank for backwards compatibility
        assignPlayerRankTeam(player, village, Rank.GENIN, server);
    }

    /**
     * DEPRECATED: Use assignPlayerRankTeam with Rank.KAGE instead
     * Legacy method for old kage system
     */
    @Deprecated
    public static void updateKagePrefix(ServerPlayer player, Village village, MinecraftServer server) {
        assignPlayerRankTeam(player, village, Rank.KAGE, server);
    }

    private static ChatFormatting getColorFromVillage(Village village) {
        return switch (village) {
            case LEAF -> ChatFormatting.GREEN;
            case SAND -> ChatFormatting.GOLD;
            case MIST -> ChatFormatting.AQUA;
            case STONE -> ChatFormatting.GRAY;
            case CLOUD -> ChatFormatting.BLUE;
        };
    }
}
