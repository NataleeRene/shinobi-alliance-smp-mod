package com.shinobi.shinobialliancemod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Manages complete player resets - clearing all mod data and restarting onboarding.
 * Triggered by LuckPerms rank clears or admin commands.
 */
public class PlayerResetManager {
    
    private static final Set<UUID> pendingResets = new HashSet<>();
    
    /**
     * Perform a complete reset of a player's mod profile.
     * This clears all stored data and restarts the onboarding process.
     * 
     * @param player The player to reset
     * @param server The server instance
     */
    public static void resetPlayer(ServerPlayer player, MinecraftServer server) {
        UUID playerUUID = player.getUUID();
        String playerName = player.getName().getString();
        
        System.out.println("[ShinobiAllianceMod] Starting full reset for player: " + playerName);
        
        // 1. COMPLETELY WIPE scoreboard data (removes from objectives entirely)
        PlayerPointsManager.completeWipe(player);
        System.out.println("[ShinobiAllianceMod] ✅ WIPED all scoreboard data for " + playerName + " (0 points, no rank)");
        
        // 2. Delete player data file from mod's playerdata folder (PERSISTENCE)
        PlayerDataManager.deletePlayerData(playerUUID, server);
        
        // 3. Clear vanilla advancement data for Shinobi advancements
        clearAdvancementData(playerUUID, server);
        
        // 4. Clear cached values in memory
        ShinobiAllianceMod.clearPlayerCache(playerUUID);
        System.out.println("[ShinobiAllianceMod] Cleared memory cache for " + playerName);
        
        // 5. Remove from scoreboard teams (LuckPerms integration)
        var scoreboard = server.getScoreboard();
        var team = scoreboard.getPlayersTeam(playerName);
        if (team != null) {
            scoreboard.removePlayerFromTeam(playerName, team);
            System.out.println("[ShinobiAllianceMod] Removed " + playerName + " from team: " + team.getName());
        }
        
        // 6. Teleport to village selection spawn
        player.teleportTo(-32.0, 111.0, -3.0);
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        player.hurtMarked = true;
        System.out.println("[ShinobiAllianceMod] Teleported " + playerName + " to spawn (-32, 111, -3)");
        
        // 7. Freeze the player
        PlayerFreezeManager.freezePlayer(player);
        System.out.println("[ShinobiAllianceMod] Froze " + playerName + " at spawn");
        
        // 8. Give them a fresh village book
        ItemStack villageBook = ShinobiItems.createVillageScroll();
        if (!player.getInventory().add(villageBook)) {
            player.drop(villageBook, false);
        }
        System.out.println("[ShinobiAllianceMod] Gave " + playerName + " a new village selection book");
        
        // 9. Mark that they need to select a village again
        ShinobiAllianceMod.markPlayerAsNeedingVillageSelection(playerUUID);
        
        // 10. Send message to player
        player.sendSystemMessage(Component.literal("§eYour shinobi profile has been reset. Please select your village again by right-clicking the book."));
        
        System.out.println("[ShinobiAllianceMod] ✅ Full reset complete for " + playerName);
    }
    
    /**
     * Clear vanilla advancement data for Shinobi advancements.
     * This deletes cached advancement progress from Minecraft's advancement system.
     */
    private static void clearAdvancementData(UUID playerUUID, MinecraftServer server) {
        try {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player != null) {
                var advancementHolder = player.getAdvancements();
                
                // Get all advancements from our mod
                var allAdvancements = server.getAdvancements().getAllAdvancements();
                int clearedCount = 0;
                
                for (var advancementEntry : allAdvancements) {
                    // Only clear advancements from our mod namespace
                    if (advancementEntry.id().getNamespace().equals("shinobialliancemod")) {
                        var progress = advancementHolder.getOrStartProgress(advancementEntry);
                        
                        // Revoke all criteria
                        for (String criterion : progress.getCompletedCriteria()) {
                            advancementHolder.revoke(advancementEntry, criterion);
                        }
                        clearedCount++;
                    }
                }
                
                System.out.println("[ShinobiAllianceMod] ✅ Cleared " + clearedCount + " advancement entries for " + player.getName().getString());
            } else {
                System.out.println("[ShinobiAllianceMod] ⚠️ Player not online, cannot clear advancement data");
            }
        } catch (Exception e) {
            System.out.println("[ShinobiAllianceMod] ⚠️ Could not clear advancement data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

    
    /**
     * Check if a player has any LuckPerms groups assigned.
     * If they have no groups, they've been reset via LuckPerms.
     */
    public static boolean hasNoLuckPermsGroups(ServerPlayer player, MinecraftServer server) {
        var scoreboard = server.getScoreboard();
        var team = scoreboard.getPlayersTeam(player.getName().getString());
        
        // If they're not on any scoreboard team, they have no groups
        if (team == null) {
            return true;
        }
        
        // Check if their team matches any of our village/rank teams
        String teamName = team.getName();
        
        // Check if it's a valid village_rank team
        for (Village village : Village.values()) {
            for (Rank rank : Rank.values()) {
                String validTeam = village.getId() + "_" + rank.getId();
                if (teamName.equals(validTeam)) {
                    return false; // They have a valid team
                }
            }
        }
        
        // They're on a team, but it's not one of ours - treat as no groups
        return true;
    }
    
    /**
     * Monitor a player on tick to check if they've had their LuckPerms cleared.
     */
    public static void checkForLuckPermsClear(ServerPlayer player, MinecraftServer server) {
        UUID playerUUID = player.getUUID();
        
        // Skip if they're already pending reset or haven't selected a village yet
        if (pendingResets.contains(playerUUID) || !ShinobiAllianceMod.hasSelectedVillage(playerUUID)) {
            return;
        }
        
        // Check if they have no LuckPerms groups
        if (hasNoLuckPermsGroups(player, server)) {
            System.out.println("[ShinobiAllianceMod] 🔄 Detected LuckPerms clear for " + player.getName().getString());
            pendingResets.add(playerUUID);
            
            // Perform reset on next tick to avoid concurrent modification
            server.execute(() -> {
                resetPlayer(player, server);
                pendingResets.remove(playerUUID);
            });
        }
    }
    
    /**
     * Admin command to manually reset a player.
     */
    public static void adminResetPlayer(ServerPlayer target, ServerPlayer admin, MinecraftServer server) {
        String targetName = target.getName().getString();
        String adminName = admin.getName().getString();
        
        System.out.println("[ShinobiAllianceMod] Admin " + adminName + " is resetting player " + targetName);
        
        // Perform the reset
        resetPlayer(target, server);
        
        // Notify admin
        admin.sendSystemMessage(Component.literal("§a✅ Successfully reset " + targetName + "'s shinobi profile."));
        
        // Log to console
        System.out.println("[ShinobiAllianceMod] Admin reset completed by " + adminName + " on " + targetName);
    }
}
