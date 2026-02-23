package com.shinobi.shinobialliancemod;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;

import java.util.*;

/**
 * Async scheduler for periodic tasks:
 * - Refresh pinned HUD every ~2s
 * - Auto resync rank/groups/claims every 5 minutes if points changed
 */
public final class ShinobiScheduler {
    private static final Map<UUID, Integer> lastPoints = new HashMap<>();
    private static final Set<UUID> pinnedHud = new HashSet<>();
    private static final Map<UUID, Integer> cachedClaimCounts = new HashMap<>();
    private static final Map<UUID, SidebarState> sidebarStates = new HashMap<>();
    private static long lastScanTime = 0;
    private static boolean started = false;
    private static java.util.concurrent.ScheduledExecutorService executor;
    private static MinecraftServer serverRef;
    private static int tickCounter = 0;

    private ShinobiScheduler() {}

    public static void start() {
        if (started) return;
        started = true;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverRef = server;
            executor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ShinobiScheduler");
                t.setDaemon(true);
                return t;
            });
            executor.scheduleAtFixedRate(ShinobiScheduler::autoResync, 300, 300, java.util.concurrent.TimeUnit.SECONDS);
            System.out.println("[ShinobiAllianceMod] Scheduler started (async)");
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (executor != null) {
                executor.shutdownNow();
                System.out.println("[ShinobiAllianceMod] Scheduler stopped");
            }
        });
    }

    public static void initializePlayerState(ServerPlayer player) {
        lastPoints.put(player.getUUID(), PlayerPointsManager.getPoints(player));
    }

    public static void pinHud(ServerPlayer player) {
        pinnedHud.add(player.getUUID());
        BossHudManager.ensure(player);
        updateBossHud(player, true);
    }
    
    public static void unpinHud(ServerPlayer player) {
        pinnedHud.remove(player.getUUID());
        BossHudManager.hide(player);
    }
    
    public static boolean isHudPinned(ServerPlayer player) { 
        return pinnedHud.contains(player.getUUID()); 
    }

    public static void toggleProgress(ServerPlayer player, boolean enabled) {
        SidebarState st = sidebarStates.get(player.getUUID());
        if (st == null) {
            st = new SidebarState();
            sidebarStates.put(player.getUUID(), st);
        }
        st.showProgress = enabled;
        if (isHudPinned(player)) {
            updateBossHud(player, true);
        }
    }

    public static void tickHudRefresh(MinecraftServer server) {
        tickCounter++;
        if (tickCounter >= 2) { // Refresh every 2 ticks (0.1 seconds)
            tickCounter = 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (isHudPinned(player)) {
                    sendHudStatus(player);
                }
            }
        }
    }

    public static void sendHudStatus(ServerPlayer player) {
        SidebarState current = sidebarStates.get(player.getUUID());
        if (current == null) current = new SidebarState();

        String village = "Unassigned";
        String rank = "";
        int points = 0;
        int claimed = 0;
        int limit = 0;

        var server = player.level().getServer();
        if (server != null) {
            Village playerVillage = RankManager.getPlayerVillage(player, server);
            if (playerVillage != null) {
                village = playerVillage.getDisplayName();
                Rank playerRank = PlayerPointsManager.getRank(player);
                rank = playerRank != null ? playerRank.getDisplayName(playerVillage) : "Genin";
                points = PlayerPointsManager.getPoints(player);
                limit = getBonusClaims(player);
                claimed = OpacNbtReader.getUsedClaims(player);
            }
        }

        // Update BossBar on change
        if (!village.equals(current.village) || !rank.equals(current.rank) ||
            points != current.points || claimed != current.claimed || limit != current.limit) {
            current.village = village;
            current.rank = rank;
            current.points = points;
            current.claimed = claimed;
            current.limit = limit;
            sidebarStates.put(player.getUUID(), current);
            updateBossHud(player, false);
        }
    }

    // ===== BossBar HUD (Server-only per-player) =====
    private static void updateBossHud(ServerPlayer player, boolean force) {
        SidebarState st = sidebarStates.get(player.getUUID());
        if (st == null) return;
        var srv = player.level().getServer();
        if (srv == null) return;

        // Build colored lines
        int HEADER_PINK_RGB = 0xFF9AA5; // exact hex per spec
        int RESULT_GREEN_RGB = 0x7CFC00; // lawn green
        net.minecraft.network.chat.TextColor headerPink = net.minecraft.network.chat.TextColor.fromRgb(HEADER_PINK_RGB);
        net.minecraft.network.chat.TextColor resultGreen = net.minecraft.network.chat.TextColor.fromRgb(RESULT_GREEN_RGB);
        net.minecraft.network.chat.TextColor villageColor = rgbForVillage(st.village);

        Component lineVillage = Component.empty()
            .append(Component.literal("Village: ").withStyle(style -> style.withColor(headerPink)))
            .append(Component.literal(st.village).withStyle(style -> style.withColor(villageColor)));
        Component lineRank = Component.empty()
            .append(Component.literal("Rank: ").withStyle(style -> style.withColor(headerPink)))
            .append(Component.literal(st.rank).withStyle(style -> style.withColor(resultGreen)));
        Component linePoints = Component.empty()
            .append(Component.literal("Points: ").withStyle(style -> style.withColor(headerPink)))
            .append(Component.literal(Integer.toString(st.points)).withStyle(style -> style.withColor(resultGreen)));
        Component lineClaims = Component.empty()
            .append(Component.literal("Claims: ").withStyle(style -> style.withColor(headerPink)))
            .append(Component.literal(st.claimed + "/" + st.limit).withStyle(style -> style.withColor(resultGreen)));

        // Boss bar title combines all lines
        Component title = Component.empty()
            .append(Component.literal("Shinobi HUD").withStyle(s -> s.withColor(resultGreen).withUnderlined(true)))
            .append(Component.literal("  "))
            .append(lineVillage)
            .append(Component.literal("  "))
            .append(lineRank)
            .append(Component.literal("  "))
            .append(linePoints)
            .append(Component.literal("  "))
            .append(lineClaims);
        
        // Max points is 288 (total achievable points in the system)
        int maxPoints = 288;
        BossHudManager.showOrUpdate(player, title, st.points, maxPoints, st.showProgress);
    }
    private static net.minecraft.network.chat.TextColor rgbForVillage(String villageDisplayName) {
        if (villageDisplayName == null) return net.minecraft.network.chat.TextColor.fromRgb(0xFFFFFF);
        String s = villageDisplayName.trim().toLowerCase();
        return switch (s) {
            case "leaf" -> net.minecraft.network.chat.TextColor.fromRgb(0xFF5555);   // red
            case "sand" -> net.minecraft.network.chat.TextColor.fromRgb(0xFFFF55);   // yellow
            case "mist" -> net.minecraft.network.chat.TextColor.fromRgb(0x5555FF);   // dark blue
            case "stone" -> net.minecraft.network.chat.TextColor.fromRgb(0x555555);  // dark gray
            case "cloud" -> net.minecraft.network.chat.TextColor.fromRgb(0xFFFFFF);  // white
            default -> net.minecraft.network.chat.TextColor.fromRgb(0xFFFFFF);
        };
    }

    private static class SidebarState {
        String village = "Unassigned";
        String rank = "";
        int points = 0;
        int claimed = 0;
        int limit = 0;
        boolean showProgress = true;
    }

    private static void autoResync() {
        if (serverRef == null) return;
        for (ServerPlayer player : serverRef.getPlayerList().getPlayers()) {
            int current = PlayerPointsManager.getPoints(player);
            int previous = lastPoints.getOrDefault(player.getUUID(), -1);
            if (current != previous) {
                RankManager.checkAndUpdateRank(player, serverRef);
                LuckPermsService.syncPlayerGroups(player, serverRef);
                ShinobiClaimBridge.applyClaimLimits(player);
                lastPoints.put(player.getUUID(), current);
                System.out.println("[ShinobiAllianceMod] Auto-resync for " + player.getName().getString());
            }
        }
    }

    public static int getBonusClaims(ServerPlayer player) {
        try {
            xaero.pac.common.server.api.OpenPACServerAPI api = xaero.pac.common.server.api.OpenPACServerAPI.get(player.level().getServer());
            if (api != null) {
                var cfg = api.getPlayerConfigs().getLoadedConfig(player.getUUID());
                if (cfg != null) {
                    Integer val = cfg.getEffective(xaero.pac.common.server.player.config.api.PlayerConfigOptions.BONUS_CHUNK_CLAIMS);
                    return val != null ? val : 0;
                }
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    public static int getClaimedChunks(ServerPlayer player) {
        // First, read directly from OPAC's per-player NBT file for accuracy
        int fromNbt = OpacNbtReader.getUsedClaims(player);
        if (fromNbt > 0) return fromNbt;

        // Fallback: attempt OPAC API/reflection if NBT is missing or empty
        try {
            xaero.pac.common.server.api.OpenPACServerAPI api = xaero.pac.common.server.api.OpenPACServerAPI.get(player.level().getServer());
            if (api == null) return 0;
            var partyManager = api.getPartyManager();
            if (partyManager == null) return 0;
            var playerParty = partyManager.getPartyByOwner(player.getUUID());
            if (playerParty == null) return 0;

            try {
                var partyClass = playerParty.getClass();
                try { var m = partyClass.getMethod("getUsedClaimsCount"); Object r = m.invoke(playerParty); if (r instanceof Integer i) return i; } catch (Throwable ignored) {}
                try { var m = partyClass.getMethod("getCurrentClaimCount"); Object r = m.invoke(playerParty); if (r instanceof Integer i) return i; } catch (Throwable ignored) {}
                try { var f = partyClass.getDeclaredField("claimedChunks"); f.setAccessible(true); Object s = f.get(playerParty); if (s instanceof java.util.Set<?> set) return set.size(); } catch (Throwable ignored) {}
                try { var f = partyClass.getDeclaredField("usedClaims"); f.setAccessible(true); Object r = f.get(playerParty); if (r instanceof Integer i) return i; } catch (Throwable ignored) {}
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
        return 0;
    }
}
