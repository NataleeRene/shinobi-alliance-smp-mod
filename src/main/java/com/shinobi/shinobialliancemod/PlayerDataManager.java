package com.shinobi.shinobialliancemod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.UUID;

/**
 * Manages persistent storage of player Shinobi data.
 * Stores data in world/shinobialliancemod/playerdata/<UUID>.json
 */
public class PlayerDataManager {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Player data structure for JSON serialization.
     */
    public static class PlayerData {
        public boolean hasSelectedVillage = false;
        public String selectedVillage = "None";
        public String shinobiRank = "Genin";
        public int achievementPoints = 0;
        
        public PlayerData() {}
        
        public PlayerData(boolean hasSelectedVillage, String selectedVillage, String shinobiRank, int achievementPoints) {
            this.hasSelectedVillage = hasSelectedVillage;
            this.selectedVillage = selectedVillage;
            this.shinobiRank = shinobiRank;
            this.achievementPoints = achievementPoints;
        }
    }
    
    /**
     * Get the player data directory.
     */
    private static File getPlayerDataDirectory(MinecraftServer server) {
        File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
        File modDataDir = new File(worldDir, "shinobialliancemod");
        File playerDataDir = new File(modDataDir, "playerdata");
        
        if (!playerDataDir.exists()) {
            playerDataDir.mkdirs();
        }
        
        return playerDataDir;
    }
    
    /**
     * Get the player data file.
     */
    private static File getPlayerDataFile(UUID playerUUID, MinecraftServer server) {
        return new File(getPlayerDataDirectory(server), playerUUID.toString() + ".json");
    }
    
    /**
     * Load player data from disk.
     * Returns new PlayerData() if file doesn't exist.
     */
    public static PlayerData loadPlayerData(UUID playerUUID, MinecraftServer server) {
        File dataFile = getPlayerDataFile(playerUUID, server);
        
        if (!dataFile.exists()) {
            System.out.println("[ShinobiAllianceMod] No data file for " + playerUUID + ", creating new player data");
            return new PlayerData();
        }
        
        try (FileReader reader = new FileReader(dataFile)) {
            PlayerData data = GSON.fromJson(reader, PlayerData.class);
            System.out.println("[ShinobiAllianceMod] Loaded data for " + playerUUID + ": hasVillage=" + data.hasSelectedVillage + ", village=" + data.selectedVillage);
            return data;
        } catch (Exception e) {
            System.out.println("[ShinobiAllianceMod] ⚠️ Error loading player data for " + playerUUID + ": " + e.getMessage());
            e.printStackTrace();
            return new PlayerData();
        }
    }
    
    /**
     * Save player data to disk.
     */
    public static void savePlayerData(UUID playerUUID, PlayerData data, MinecraftServer server) {
        File dataFile = getPlayerDataFile(playerUUID, server);
        
        try (FileWriter writer = new FileWriter(dataFile)) {
            GSON.toJson(data, writer);
            System.out.println("[ShinobiAllianceMod] ✅ Saved data for " + playerUUID + ": hasVillage=" + data.hasSelectedVillage + ", village=" + data.selectedVillage + ", rank=" + data.shinobiRank + ", points=" + data.achievementPoints);
        } catch (Exception e) {
            System.out.println("[ShinobiAllianceMod] ❌ Error saving player data for " + playerUUID + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Save player data from current game state.
     */
    public static void savePlayerData(ServerPlayer player, MinecraftServer server) {
        UUID playerUUID = player.getUUID();
        
        // Get current game state
        boolean hasVillage = ShinobiAllianceMod.hasSelectedVillage(playerUUID);
        String village = "None";
        
        // Try to get village from LuckPerms teams
        var scoreboard = server.getScoreboard();
        var team = scoreboard.getPlayersTeam(player.getName().getString());
        if (team != null) {
            String teamName = team.getName();
            // Parse village from team name (format: village_rank)
            if (teamName.contains("_")) {
                String[] parts = teamName.split("_");
                if (parts.length >= 1) {
                    village = capitalizeFirst(parts[0]);
                }
            }
        }
        
        // Get rank and points
        int points = PlayerPointsManager.getPoints(player);
        Rank rank = PlayerPointsManager.getRank(player);
        
        // Create and save data
        PlayerData data = new PlayerData(hasVillage, village, rank.getId(), points);
        savePlayerData(playerUUID, data, server);
    }
    
    /**
     * Delete player data file (for resets).
     */
    public static void deletePlayerData(UUID playerUUID, MinecraftServer server) {
        File dataFile = getPlayerDataFile(playerUUID, server);
        
        if (dataFile.exists()) {
            if (dataFile.delete()) {
                System.out.println("[ShinobiAllianceMod] Deleted player data file for " + playerUUID);
            } else {
                System.out.println("[ShinobiAllianceMod] ⚠️ Failed to delete player data file for " + playerUUID);
            }
        }
    }
    
    /**
     * Check if player has existing data file.
     */
    public static boolean hasPlayerData(UUID playerUUID, MinecraftServer server) {
        return getPlayerDataFile(playerUUID, server).exists();
    }
    
    /**
     * Capitalize first letter of a string.
     */
    private static String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
