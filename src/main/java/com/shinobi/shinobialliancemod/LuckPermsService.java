package com.shinobi.shinobialliancemod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class LuckPermsService {
    
    /**
     * Automatically sync a player's LuckPerms groups with their mod data on join
     * Fixes players who are missing rank groups
     */
    public static void syncPlayerGroups(ServerPlayer player, MinecraftServer server) {
        String playerName = player.getName().getString();
        
        // Get player's current village and rank from mod data
        Village village = RankManager.getPlayerVillage(player, server);
        if (village == null) {
            System.out.println("[LuckPermsService] Player " + playerName + " has no village assigned, skipping sync");
            return;
        }
        
        Rank rank = PlayerPointsManager.getRank(player);
        if (rank == null) {
            rank = Rank.GENIN; // Default to Genin
        }
        
        // Build expected group name
        String expectedGroup = village.getId() + "_" + rank.getId();
        
        System.out.println("[LuckPermsService] Syncing " + playerName + ": village=" + village.getId() + ", rank=" + rank.getId());
        
        // Ensure village group is assigned
        executeCommand(server, "lp user " + playerName + " parent add " + village.getId());
        
        // Ensure rank group is assigned
        executeCommand(server, "lp user " + playerName + " parent add " + expectedGroup);
    }
    
    public static void assignVillage(ServerPlayer player, Village village, MinecraftServer server) {
        String playerName = player.getName().getString();

        // Remove all other village groups first
        for (Village v : Village.values()) {
            if (v != village) {
                executeCommand(server, "lp user " + playerName + " parent remove " + v.getId());
            }
        }

        // Add the selected village group
        executeCommand(server, "lp user " + playerName + " parent add " + village.getId());
        
        // Assign starting rank (Genin) for the village
        String geninGroup = village.getId() + "_genin";
        executeCommand(server, "lp user " + playerName + " parent add " + geninGroup);
        
        System.out.println("[ShinobiAllianceMod] Assigned " + player.getName().getString() + " to village: " + village.getDisplayName() + " as Genin");
        
        // Update claim limits for new player
        ShinobiClaimBridge.applyClaimLimits(player);
    }

    public static void setKage(ServerPlayer player, Village village, MinecraftServer server) {
        String playerName = player.getName().getString();
        
        // Remove any other kage groups
        for (Village v : Village.values()) {
            executeCommand(server, "lp user " + playerName + " parent remove " + v.getKageGroup());
        }

        // Add the kage group for this village
        executeCommand(server, "lp user " + playerName + " parent add " + village.getKageGroup());
        
        // Add OPAC management permissions
        executeCommand(server, "lp user " + playerName + " permission set openpartiesandclaims.manage." + village.getId() + " true");
        executeCommand(server, "lp user " + playerName + " permission set openpartiesandclaims.claim." + village.getId() + " true");
        
        System.out.println("[ShinobiAllianceMod] Set " + playerName + " as Kage of " + village.getDisplayName());
    }

    public static void removeKage(ServerPlayer player, Village village, MinecraftServer server) {
        String playerName = player.getName().getString();
        
        // Remove the kage group
        executeCommand(server, "lp user " + playerName + " parent remove " + village.getKageGroup());
        
        // Remove OPAC management permissions
        executeCommand(server, "lp user " + playerName + " permission unset openpartiesandclaims.manage." + village.getId());
        executeCommand(server, "lp user " + playerName + " permission unset openpartiesandclaims.claim." + village.getId());
        
        System.out.println("[ShinobiAllianceMod] Removed Kage status from " + playerName);
    }

    public static boolean isKage(ServerPlayer player, Village village, MinecraftServer server) {
        // Check if player's scoreboard team matches the kage team for this village
        // This is a workaround since we can't use the Permissions API at runtime
        var scoreboard = server.getScoreboard();
        var team = scoreboard.getPlayersTeam(player.getName().getString());
        if (team == null) return false;
        
        String kageTeamName = village.getId() + "_kage";
        return team.getName().equals(kageTeamName);
    }

    public static void addWarBypassPermissions(ServerPlayer player, Village enemyVillage, MinecraftServer server) {
        String playerName = player.getName().getString();
        
        // Add temporary bypass permissions for the enemy village
        executeCommand(server, "lp user " + playerName + " permission set openpartiesandclaims.bypass.claims." + enemyVillage.getId() + " true");
        executeCommand(server, "lp user " + playerName + " permission set openpartiesandclaims.bypass.container." + enemyVillage.getId() + " true");
        
        System.out.println("[ShinobiAllianceMod] Added war bypass permissions for " + playerName + " against " + enemyVillage.getDisplayName());
    }

    public static void removeWarBypassPermissions(ServerPlayer player, Village enemyVillage, MinecraftServer server) {
        String playerName = player.getName().getString();
        
        // Remove bypass permissions
        executeCommand(server, "lp user " + playerName + " permission unset openpartiesandclaims.bypass.claims." + enemyVillage.getId());
        executeCommand(server, "lp user " + playerName + " permission unset openpartiesandclaims.bypass.container." + enemyVillage.getId());
        
        System.out.println("[ShinobiAllianceMod] Removed war bypass permissions for " + playerName + " against " + enemyVillage.getDisplayName());
    }

    public static void executeCommand(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(
            server.createCommandSourceStack(),
            command
        );
    }
}
