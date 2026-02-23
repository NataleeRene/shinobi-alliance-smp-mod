package com.shinobi.shinobialliancemod;

import net.minecraft.server.level.ServerPlayer;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;

import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.player.config.api.IPlayerConfigAPI;
import xaero.pac.common.server.player.config.api.IPlayerConfigManagerAPI;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;

import java.util.Locale;

public final class ShinobiClaimBridge {

    private ShinobiClaimBridge() {}

    public static void applyClaimLimits(ServerPlayer player) {
        if (player == null || player.level() == null || player.level().getServer() == null) {
            return;
        }

        OpenPACServerAPI opac = OpenPACServerAPI.get(player.level().getServer());
        if (opac == null) {
            System.out.println("[ShinobiClaimBridge] OPAC API unavailable; skipping for " + player.getName().getString());
            return;
        }

        IPlayerConfigManagerAPI configManager = opac.getPlayerConfigs();
        if (configManager == null) {
            System.out.println("[ShinobiClaimBridge] Player config manager null; skipping for " + player.getName().getString());
            return;
        }

        IPlayerConfigAPI config = configManager.getLoadedConfig(player.getUUID());
        if (config == null) {
            System.out.println("[ShinobiClaimBridge] Player config not loaded; skipping for " + player.getName().getString());
            return;
        }

        int desiredLimit = mapLuckPermsGroupsToLimit(player);
        if (desiredLimit <= 0) {
            System.out.println("[ShinobiClaimBridge] No matching LP rank group for " + player.getName().getString() + " - applying default (2 claims)");
            desiredLimit = 2; // Default to Genin level if no rank found
        }

        // OPAC formula: Total Claims = maxPlayerClaims (server config) + bonusChunkClaims (player config)
        // Since server has maxPlayerClaims = 0, we set bonus to desired total
        Integer currentBonus = config.getEffective(PlayerConfigOptions.BONUS_CHUNK_CLAIMS);
        
        if (currentBonus != null && currentBonus == desiredLimit) {
            System.out.println("[ShinobiClaimBridge] " + player.getName().getString() + " already has correct limit: " + desiredLimit);
            return; // Already set correctly
        }

        System.out.println("[ShinobiClaimBridge] Updating " + player.getName().getString() + " from " + currentBonus + " to " + desiredLimit + " claims (Rank-based limit)");

        // Set BONUS_CHUNK_CLAIMS to desired total (since base is 0)
        IPlayerConfigAPI.SetResult result = config.tryToSet(PlayerConfigOptions.BONUS_CHUNK_CLAIMS, desiredLimit);
        
        if (result == IPlayerConfigAPI.SetResult.SUCCESS) {
            Integer verified = config.getEffective(PlayerConfigOptions.BONUS_CHUNK_CLAIMS);
            System.out.println("[ShinobiClaimBridge] ✅ Successfully set claim limit for " + player.getName().getString() + " to " + verified);
            System.out.println("[ShinobiClaimBridge] NOTE: In singleplayer/LAN worlds, OPAC may not enforce these limits properly. Test on a dedicated server for accurate claim limit enforcement.");
        } else {
            System.out.println("[ShinobiClaimBridge] ❌ Failed to set claim limit for " + player.getName().getString() + " - result: " + result);
            System.out.println("[ShinobiClaimBridge] This may indicate OPAC config is not loaded yet or permissions issue.");
        }
    }

    // Compatibility stub used by existing code paths
    public static void updateClaimLimit(ServerPlayer player, net.minecraft.server.MinecraftServer server) {
        applyClaimLimits(player);
    }

    private static int mapLuckPermsGroupsToLimit(ServerPlayer player) {
        LuckPerms lp;
        try {
            lp = LuckPermsProvider.get();
        } catch (Throwable t) {
            System.out.println("[ShinobiClaimBridge] LuckPerms unavailable.");
            return 0;
        }

        User user = lp.getUserManager().getUser(player.getUUID());
        if (user == null) {
            System.out.println("[ShinobiClaimBridge] LuckPerms user not found for " + player.getName().getString());
            return 0;
        }

        // Debug: print all groups
        System.out.println("[ShinobiClaimBridge] Checking groups for " + player.getName().getString() + ":");
        int best = 0;
        int nodeCount = 0;
        for (Node node : user.getNodes()) {
            String key = node.getKey();
            if (key == null) continue;
            String k = key.toLowerCase(Locale.ROOT);
            if (k.startsWith("group.")) {
                nodeCount++;
                String group = k.substring("group.".length());
                System.out.println("[ShinobiClaimBridge]   - Found group: " + group);
                int mapped = mapGroupNameToLimit(group);
                if (mapped > 0) {
                    System.out.println("[ShinobiClaimBridge]     -> Mapped to " + mapped + " claims");
                }
                if (mapped > best) best = mapped;
            }
        }
        
        if (nodeCount == 0) {
            System.out.println("[ShinobiClaimBridge]   - No group nodes found in user data");
        }
        
        return best;
    }

    private static int mapGroupNameToLimit(String group) {
        if (group.endsWith("_genin")) return 2;
        if (group.endsWith("_chunin")) return 4;
        if (group.endsWith("_jonin")) return 6;
        if (group.endsWith("_anbu")) return 8;
        if (group.endsWith("_kage")) return 75;
        return 0;
    }
}
