package com.shinobi.shinobialliancemod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Manages player rank promotions/demotions based on achievement points
 * Integrates with LuckPerms for group assignments and ScoreboardTeamManager for prefixes
 */
public class RankManager {

    /**
     * Check if player should be promoted or demoted based on their points
     * Returns true if rank changed
     */
    public static boolean checkAndUpdateRank(ServerPlayer player, MinecraftServer server) {
        int points = PlayerPointsManager.getPoints(player);
        Rank currentRank = PlayerPointsManager.getRank(player);
        Rank newRank = Rank.fromPoints(points);

        if (currentRank != newRank) {
            promotePlayer(player, currentRank, newRank, server);
            return true;
        }

        return false;
    }

    /**
     * Promote or demote a player to a new rank
     */
    private static void promotePlayer(ServerPlayer player, Rank oldRank, Rank newRank, MinecraftServer server) {
        String playerName = player.getName().getString();
        
        // Update stored rank
        PlayerPointsManager.setRank(player, newRank);

        // Get player's village
        Village village = getPlayerVillage(player, server);
        if (village == null) {
            System.out.println("[ShinobiAllianceMod] WARNING: Cannot promote " + playerName + " - no village assigned");
            return;
        }

        // Build village_rank group names (e.g., "leaf_jonin")
        String newGroupName = village.getId() + "_" + newRank.getId();
        
        // Remove old rank group from LuckPerms
        if (oldRank != null) {
            String oldGroupName = village.getId() + "_" + oldRank.getId();
            LuckPermsService.executeCommand(server, "lp user " + playerName + " parent remove " + oldGroupName);
        }

        // Add new rank group to LuckPerms
        LuckPermsService.executeCommand(server, "lp user " + playerName + " parent add " + newGroupName);

        // Update scoreboard team with new rank prefix
        ScoreboardTeamManager.assignPlayerRankTeam(player, village, newRank, server);

        // Update OPAC claim limits based on new rank
        ShinobiClaimBridge.applyClaimLimits(player);

        // Handle Kage-specific permissions (OPAC)
        if (newRank == Rank.KAGE) {
            // Add OPAC management permissions
            LuckPermsService.executeCommand(server, "lp user " + playerName + " permission set openpartiesandclaims.manage." + village.getId() + " true");
            LuckPermsService.executeCommand(server, "lp user " + playerName + " permission set openpartiesandclaims.claim." + village.getId() + " true");
            
            // Broadcast server-wide message
            broadcastKagePromotion(player, village, server);
        } else if (oldRank == Rank.KAGE && newRank != Rank.KAGE) {
            // Remove OPAC permissions if demoted from Kage
            LuckPermsService.executeCommand(server, "lp user " + playerName + " permission unset openpartiesandclaims.manage." + village.getId());
            LuckPermsService.executeCommand(server, "lp user " + playerName + " permission unset openpartiesandclaims.claim." + village.getId());
        }

        // Notify player of promotion/demotion
        if (newRank.isHigherThan(oldRank)) {
            player.sendSystemMessage(Component.literal("§d✦ §fYou have been promoted to §6" + newRank.getDisplayName(village) + "§f!"));
            
            // Broadcast rank up to village members (except Kage which is server-wide)
            if (newRank != Rank.KAGE) {
                broadcastRankUp(player, newRank, village, server);
            }
        } else {
            player.sendSystemMessage(Component.literal("§cYou have been demoted to " + newRank.getDisplayName(village) + "."));
        }

        System.out.println("[ShinobiAllianceMod] " + playerName + " rank changed: " + 
            (oldRank != null ? oldRank.getDisplayName(village) : "None") + " -> " + newRank.getDisplayName(village));
        
        // PERSISTENCE FIX: Save player data after rank change
        PlayerDataManager.savePlayerData(player, server);

        // Update OPAC claim limits based on new rank
        ShinobiClaimBridge.updateClaimLimit(player, server);
    }

    /**
     * Broadcast Kage promotion to entire server
     */
    private static void broadcastKagePromotion(ServerPlayer player, Village village, MinecraftServer server) {
        Component message = Component.literal("§c⚔ §6A new Kage has risen! §f" + 
            player.getName().getString() + " §fis now the §c" + village.getDisplayName() + " Kage§f!");
        
        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            onlinePlayer.sendSystemMessage(message);
        }
    }

    /**
     * Broadcast rank promotion to village members
     */
    private static void broadcastRankUp(ServerPlayer player, Rank newRank, Village village, MinecraftServer server) {
        Component message = Component.literal(village.getColor() + player.getName().getString() + 
            " §fhas been promoted to §6" + newRank.getDisplayName(village) + "§f!");
        
        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            Village playerVillage = getPlayerVillage(onlinePlayer, server);
            if (playerVillage == village) {
                onlinePlayer.sendSystemMessage(message);
            }
        }
    }

    /**
     * Get a player's village from their scoreboard team
     */
    public static Village getPlayerVillage(ServerPlayer player, MinecraftServer server) {
        var scoreboard = server.getScoreboard();
        var team = scoreboard.getPlayersTeam(player.getName().getString());
        
        if (team == null) {
            return null;
        }

        String teamName = team.getName();
        
        // Team names are either "village_rank" or just "village"
        // Extract village ID
        for (Village village : Village.values()) {
            if (teamName.startsWith(village.getId())) {
                return village;
            }
        }

        return null;
    }

    /**
     * Initialize a new player with Genin rank
     */
    public static void initializeNewPlayer(ServerPlayer player, Village village, MinecraftServer server) {
        String playerName = player.getName().getString();
        
        // Set initial points and rank
        PlayerPointsManager.setPoints(player, 0);
        PlayerPointsManager.setRank(player, Rank.GENIN);

        // Add Genin group to LuckPerms
        LuckPermsService.executeCommand(server, "lp user " + playerName + " parent add " + Rank.GENIN.getLuckPermsGroup());

        // Assign to rank team
        ScoreboardTeamManager.assignPlayerRankTeam(player, village, Rank.GENIN, server);

        System.out.println("[ShinobiAllianceMod] Initialized " + playerName + " with Genin rank");
    }
}
