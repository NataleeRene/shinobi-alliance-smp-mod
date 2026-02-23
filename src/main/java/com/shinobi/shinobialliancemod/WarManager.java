package com.shinobi.shinobialliancemod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Updated War System - Multiple wars, player-based declarations
 * Wars are tracked between individual Kage players (by UUID)
 */
public class WarManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long GRACE_PERIOD_MILLIS = 60L * 60L * 1000L; // 1 hour
    private static final Map<UUID, Set<UUID>> activeWars = new HashMap<>(); // attackerUUID -> Set of defenderUUIDs
    private static final Map<String, Long> warStartTimes = new HashMap<>(); // warKey -> epoch millis declared
    private static final Set<String> appliedBypass = new HashSet<>(); // warKey where bypass already applied
    // Allies per war key attacker:defender (attacker side allies & defender side allies)
    private static final Map<String, WarAllies> warAllies = new HashMap<>();
    // Ally-specific grace starts: warKey -> (allyUUID -> epoch millis when ally became eligible)
    private static final Map<String, Map<UUID, Long>> allyGraceStarts = new HashMap<>();
    // Pending allies needing opt-in per side
    private static final Map<String, PendingAllies> pendingAllies = new HashMap<>();
    // Allies who have already been granted bypass/permissions per war key
    private static final Map<String, Set<UUID>> appliedAllyBypass = new HashMap<>();
    // Grace bypass votes per war key; both Kage must agree
    private static final Map<String, Set<UUID>> bypassVotes = new HashMap<>();
    private static Path warDataFile;

    /**
     * Initialize war system and load persisted data
     */
    public static void initialize(MinecraftServer server) {
        warDataFile = server.getServerDirectory().resolve("shinobi_wars.json");
        loadWars();
        System.out.println("[ShinobiAllianceMod] War system initialized");
    }

    /**
     * Declare war from one Kage to another
     * @param attacker The attacking Kage
     * @param defenderUUID UUID of the defending Kage
     * @param server MinecraftServer instance
     * @return true if war was declared successfully
     */
    public static boolean declareWar(ServerPlayer attacker, UUID defenderUUID, MinecraftServer server) {
        UUID attackerUUID = attacker.getUUID();
        
        // Get defender's current name
        String defenderName = getPlayerNameFromUUID(defenderUUID, server);
        if (defenderName == null) {
            attacker.sendSystemMessage(Component.literal("§cTarget player not found!"));
            return false;
        }
        
        // Validate attacker is Kage
        Rank attackerRank = PlayerPointsManager.getRank(attacker);
        if (attackerRank != Rank.KAGE) {
            attacker.sendSystemMessage(Component.literal("§cOnly Kages can declare war!"));
            return false;
        }
        
        // Validate defender is Kage
        ServerPlayer defender = server.getPlayerList().getPlayer(defenderUUID);
        Rank defenderRank;
        if (defender != null) {
            defenderRank = PlayerPointsManager.getRank(defender);
        } else {
            // Try to load from saved data - if offline, assume they're still Kage
            // In production, you'd load from persistent storage
            attacker.sendSystemMessage(Component.literal("§cTarget player must be online and have Kage rank!"));
            return false;
        }
        
        if (defenderRank != Rank.KAGE) {
            attacker.sendSystemMessage(Component.literal("§cYou can only declare war on other Kages!"));
            return false;
        }
        
        // Check if war already exists
        if (isAtWar(attackerUUID, defenderUUID)) {
            attacker.sendSystemMessage(Component.literal("§cYou are already at war with " + defenderName + "!"));
            return false;
        }
        
        // Prevent self-war
        if (attackerUUID.equals(defenderUUID)) {
            attacker.sendSystemMessage(Component.literal("§cYou cannot declare war on yourself!"));
            return false;
        }
        
        // Add war to active wars
        activeWars.computeIfAbsent(attackerUUID, k -> new HashSet<>()).add(defenderUUID);
        String key = warKey(attackerUUID, defenderUUID);
        warAllies.computeIfAbsent(key, k -> new WarAllies());
        warStartTimes.put(key, System.currentTimeMillis());
        appliedBypass.remove(key);

        // Save wars to disk (includes grace timer)
        saveWars();
        
        // Get villages and ranks for broadcast
        Village attackerVillage = getPlayerVillage(attacker, server);
        Village defenderVillage = getPlayerVillage(defender, server);
        
        String attackerDisplay = formatKageDisplay(attacker, attackerVillage);
        String defenderDisplay = formatKageDisplay(defender, defenderVillage);
        
        // Broadcast war declaration with grace period info
        Component message = Component.literal("⚔ " + attackerDisplay + " §rhas declared war on " + defenderDisplay + "§r!")
            .append(Component.literal(" §7(Claims bypass unlocks in 60 minutes when both Kage are online.)"));
        server.getPlayerList().broadcastSystemMessage(message, false);
        
        System.out.println("[ShinobiAllianceMod] War declared: " + attacker.getName().getString() + 
            " (" + attackerUUID + ") vs " + defenderName + " (" + defenderUUID + ")");
        // Auto-register party members: non-Kage auto grouped; Kage must opt-in
        autoRegisterPartyMembers(attacker, defender, server);
        
        return true;
    }

    /**
     * End a war between two Kages
     */
    public static boolean endWar(UUID attackerUUID, UUID defenderUUID, MinecraftServer server) {
        if (!isAtWar(attackerUUID, defenderUUID)) {
            return false;
        }
        
        // Remove war
        Set<UUID> defenders = activeWars.get(attackerUUID);
        if (defenders != null) {
            defenders.remove(defenderUUID);
            if (defenders.isEmpty()) {
                activeWars.remove(attackerUUID);
            }
        }
        String key = warKey(attackerUUID, defenderUUID);
        warAllies.remove(key);
        warStartTimes.remove(key);
        appliedBypass.remove(key);
        appliedAllyBypass.remove(key);
        bypassVotes.remove(key);
        
        // Remove OPAC bypass permissions
        removeWarBypass(attackerUUID, defenderUUID, server);
        
        // Save wars to disk
        saveWars();
        
        // Get player names for broadcast
        String attackerName = getPlayerNameFromUUID(attackerUUID, server);
        String defenderName = getPlayerNameFromUUID(defenderUUID, server);
        
        // Broadcast peace message
        Component message = Component.literal("🕊 Peace restored between §6" + attackerName + " §rand §6" + defenderName + "§r.");
        server.getPlayerList().broadcastSystemMessage(message, false);
        
        System.out.println("[ShinobiAllianceMod] War ended: " + attackerName + " vs " + defenderName);
        
        return true;
    }

    /**
     * Check if a specific war is active
     */
    public static boolean isAtWar(UUID attackerUUID, UUID defenderUUID) {
        Set<UUID> defenders = activeWars.get(attackerUUID);
        return defenders != null && defenders.contains(defenderUUID);
    }

    /**
     * Check if a player is involved in ANY war
     */
    public static boolean isInvolvedInWar(UUID playerUUID) {
        // Check if they're an attacker
        if (activeWars.containsKey(playerUUID)) {
            return true;
        }
        
        // Check if they're a defender in any war
        for (Set<UUID> defenders : activeWars.values()) {
            if (defenders.contains(playerUUID)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Check if two players are at war AND bypass is active (grace expired)
     */
    public static boolean isAtWarWithBypassActive(UUID player1, UUID player2) {
        boolean atWar = isAtWar(player1, player2) || isAtWar(player2, player1);
        if (!atWar) return false;
        
        // Determine attacker/defender
        UUID attacker = isAtWar(player1, player2) ? player1 : player2;
        UUID defender = isAtWar(player1, player2) ? player2 : player1;
        
        String key = warKey(attacker, defender);
        return appliedBypass.contains(key);
    }

    /**
     * Get all wars involving a specific player
     */
    public static List<WarInfo> getWarsForPlayer(UUID playerUUID) {
        List<WarInfo> wars = new ArrayList<>();
        
        // Wars where they're the attacker
        Set<UUID> asAttacker = activeWars.get(playerUUID);
        if (asAttacker != null) {
            for (UUID defenderUUID : asAttacker) {
                wars.add(new WarInfo(playerUUID, defenderUUID));
            }
        }
        
        // Wars where they're the defender
        for (Map.Entry<UUID, Set<UUID>> entry : activeWars.entrySet()) {
            if (entry.getValue().contains(playerUUID)) {
                wars.add(new WarInfo(entry.getKey(), playerUUID));
            }
        }
        
        return wars;
    }

    /**
     * Get all active wars
     */
    public static List<WarInfo> getAllWars() {
        List<WarInfo> wars = new ArrayList<>();
        
        for (Map.Entry<UUID, Set<UUID>> entry : activeWars.entrySet()) {
            UUID attackerUUID = entry.getKey();
            for (UUID defenderUUID : entry.getValue()) {
                wars.add(new WarInfo(attackerUUID, defenderUUID));
            }
        }
        
        return wars;
    }

    /**
     * Request immediate bypass (skip grace) for a war. Both Kage must agree.
     * Returns human-readable status.
     */
    public static String requestGraceBypass(UUID requester, UUID target, MinecraftServer server) {
        boolean direct = isAtWar(requester, target);
        boolean reverse = isAtWar(target, requester);
        if (!direct && !reverse) {
            return "No active war with that player.";
        }

        UUID attacker = direct ? requester : target;
        UUID defender = direct ? target : requester;
        String key = warKey(attacker, defender);

        if (appliedBypass.contains(key)) {
            return "Bypass already active for this war.";
        }

        Set<UUID> votes = bypassVotes.computeIfAbsent(key, k -> new HashSet<>());
        if (votes.contains(requester)) {
            return "You already agreed to bypass this war's grace.";
        }
        votes.add(requester);

        // Notify the other Kage
        UUID other = requester.equals(attacker) ? defender : attacker;
        ServerPlayer otherPlayer = server.getPlayerList().getPlayer(other);
        if (otherPlayer != null) {
            otherPlayer.sendSystemMessage(Component.literal("§e" + getPlayerNameFromUUID(requester, server) + " wants to skip the grace period. Type §a/war bypass " + getPlayerNameFromUUID(requester, server) + " §eto agree."));
        }

        if (votes.contains(attacker) && votes.contains(defender)) {
            boolean applied = applyWarBypass(attacker, defender, server);
            if (applied) {
                appliedBypass.add(key);
                // Clear grace period by setting start time to far past (instant expiration)
                warStartTimes.put(key, 0L);
                // Clear all ally grace times for this war
                if (allyGraceStarts.containsKey(key)) {
                    Map<UUID, Long> allyTimes = allyGraceStarts.get(key);
                    for (UUID allyUUID : allyTimes.keySet()) {
                        allyTimes.put(allyUUID, 0L);
                    }
                }
                ensureAlliesAccess(attacker, defender, server);
                bypassVotes.remove(key);
                return "Both Kage agreed. Claims bypass is now active for this war.";
            } else {
                return "Both agreed, but bypass could not be applied (are both online?).";
            }
        }

        return "Your vote is recorded. The other Kage must also run /war bypass to activate.";
    }

    /**
     * Apply war bypass using OPAC PlayerConfig API
     */
    private static boolean applyWarBypass(UUID attackerUUID, UUID defenderUUID, MinecraftServer server) {
        ServerPlayer attacker = server.getPlayerList().getPlayer(attackerUUID);
        ServerPlayer defender = server.getPlayerList().getPlayer(defenderUUID);

        if (attacker == null || defender == null) {
            System.out.println("[ShinobiAllianceMod] Cannot apply bypass - attacker online: " + (attacker != null) + ", defender online: " + (defender != null));
            return false;
        }

        try {
            xaero.pac.common.server.api.OpenPACServerAPI opac = xaero.pac.common.server.api.OpenPACServerAPI.get(server);
            if (opac == null) {
                System.out.println("[ShinobiAllianceMod] OPAC API unavailable");
                return false;
            }

            var configManager = opac.getPlayerConfigs();
            if (configManager == null) {
                System.out.println("[ShinobiAllianceMod] Player config manager unavailable");
                return false;
            }

            var attackerConfig = configManager.getLoadedConfig(attackerUUID);
            var defenderConfig = configManager.getLoadedConfig(defenderUUID);

            if (attackerConfig == null || defenderConfig == null) {
                System.out.println("[ShinobiAllianceMod] Player configs not loaded");
                return false;
            }

            // Disable claim protection for both players during war
            // WARNING: This means ANYONE can break their claims, not just each other
            // This is a limitation - OPAC doesn't have per-player protection exceptions in the API
            var result1 = attackerConfig.tryToSet(xaero.pac.common.server.player.config.api.PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS, false);
            var result2 = defenderConfig.tryToSet(xaero.pac.common.server.player.config.api.PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS, false);

            System.out.println("[ShinobiAllianceMod] War bypass activated for " + attacker.getName().getString() + " <-> " + defender.getName().getString());
            System.out.println("[ShinobiAllianceMod] Protection disabled - attacker: " + result1 + ", defender: " + result2);
            
            return (result1 == xaero.pac.common.server.player.config.api.IPlayerConfigAPI.SetResult.SUCCESS &&
                    result2 == xaero.pac.common.server.player.config.api.IPlayerConfigAPI.SetResult.SUCCESS);
        } catch (Exception e) {
            System.out.println("[ShinobiAllianceMod] Error applying bypass: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

    }

    /**
     * Remove war bypass by re-enabling claim protection for both players
     */
    private static void removeWarBypass(UUID attackerUUID, UUID defenderUUID, MinecraftServer server) {
        ServerPlayer attacker = server.getPlayerList().getPlayer(attackerUUID);
        ServerPlayer defender = server.getPlayerList().getPlayer(defenderUUID);

        String attackerName = getPlayerNameFromUUID(attackerUUID, server);
        String defenderName = getPlayerNameFromUUID(defenderUUID, server);

        if (attacker == null && defender == null) {
            System.out.println("[ShinobiAllianceMod] Both players offline - cannot re-enable protection now");
            return;
        }

        try {
            xaero.pac.common.server.api.OpenPACServerAPI opac = xaero.pac.common.server.api.OpenPACServerAPI.get(server);
            if (opac == null) return;

            var configManager = opac.getPlayerConfigs();
            if (configManager == null) return;

            // Re-enable protection for both players
            if (attacker != null) {
                var attackerConfig = configManager.getLoadedConfig(attackerUUID);
                if (attackerConfig != null) {
                    attackerConfig.tryToSet(xaero.pac.common.server.player.config.api.PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS, true);
                }
            }

            if (defender != null) {
                var defenderConfig = configManager.getLoadedConfig(defenderUUID);
                if (defenderConfig != null) {
                    defenderConfig.tryToSet(xaero.pac.common.server.player.config.api.PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS, true);
                }
            }

            System.out.println("[ShinobiAllianceMod] Re-enabled protection for " + attackerName + " <-> " + defenderName);
        } catch (Exception e) {
            System.out.println("[ShinobiAllianceMod] Error removing bypass: " + e.getMessage());
        }
    }

    /**
     * Get player name from UUID
     */
    private static String getPlayerNameFromUUID(UUID uuid, MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            return player.getName().getString();
        }
        
        // If player is offline, return UUID as string
        // In production, you'd want to query Mojang API or cache
        return uuid.toString();
    }

    /**
     * Get player's village
     */
    private static Village getPlayerVillage(ServerPlayer player, MinecraftServer server) {
        var scoreboard = server.getScoreboard();
        var team = scoreboard.getPlayersTeam(player.getScoreboardName());
        
        if (team != null) {
            String teamName = team.getName();
            // Team names are like "leaf_kage", "sand_jonin", etc.
            for (Village village : Village.values()) {
                if (teamName.startsWith(village.getId())) {
                    return village;
                }
            }
        }
        
        return null;
    }

    /**
     * Format Kage display with rank color and village
     */
    private static String formatKageDisplay(ServerPlayer player, Village village) {
        if (village != null) {
            return village.getRankPrefix(Rank.KAGE) + player.getName().getString();
        }
        return "§c[Kage] §f" + player.getName().getString();
    }

    /**
     * Save wars to disk
     */
    private static void saveWars() {
        try {
            // Convert to serializable format
            Map<String, Set<String>> serializable = new HashMap<>();
            for (Map.Entry<UUID, Set<UUID>> entry : activeWars.entrySet()) {
                String attackerStr = entry.getKey().toString();
                Set<String> defendersStr = entry.getValue().stream()
                    .map(UUID::toString)
                    .collect(Collectors.toSet());
                serializable.put(attackerStr, defendersStr);
            }

            Map<String, Long> starts = new HashMap<>();
            for (Map.Entry<String, Long> entry : warStartTimes.entrySet()) {
                starts.put(entry.getKey(), entry.getValue());
            }

            WarSave save = new WarSave(serializable, starts);
            String json = GSON.toJson(save);
            Files.writeString(warDataFile, json);
            System.out.println("[ShinobiAllianceMod] Saved wars to disk");
        } catch (IOException e) {
            System.err.println("[ShinobiAllianceMod] Failed to save wars: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load wars from disk
     */
    private static void loadWars() {
        if (!Files.exists(warDataFile)) {
            System.out.println("[ShinobiAllianceMod] No war data file found, starting fresh");
            return;
        }
        
        try {
            String json = Files.readString(warDataFile);

            // New format: WarSave wrapper
            try {
                WarSave save = GSON.fromJson(json, WarSave.class);
                if (save != null && save.wars != null) {
                    activeWars.clear();
                    warStartTimes.clear();
                    for (Map.Entry<String, Set<String>> entry : save.wars.entrySet()) {
                        UUID attackerUUID = UUID.fromString(entry.getKey());
                        Set<UUID> defenders = entry.getValue().stream()
                            .map(UUID::fromString)
                            .collect(Collectors.toSet());
                        activeWars.put(attackerUUID, defenders);
                    }
                    if (save.starts != null) {
                        warStartTimes.putAll(save.starts);
                    }
                    System.out.println("[ShinobiAllianceMod] Loaded " + getAllWars().size() + " wars from disk (with grace timers)");
                    return;
                }
            } catch (Exception ignored) {
                // Fallback to legacy format below
            }

            Type type = new TypeToken<Map<String, Set<String>>>(){}.getType();
            Map<String, Set<String>> serializable = GSON.fromJson(json, type);
            
            if (serializable != null) {
                activeWars.clear();
                for (Map.Entry<String, Set<String>> entry : serializable.entrySet()) {
                    UUID attackerUUID = UUID.fromString(entry.getKey());
                    Set<UUID> defenders = entry.getValue().stream()
                        .map(UUID::fromString)
                        .collect(Collectors.toSet());
                    activeWars.put(attackerUUID, defenders);
                    long legacyStart = System.currentTimeMillis() - GRACE_PERIOD_MILLIS; // allow immediate activation after restart
                    for (UUID def : defenders) {
                        warStartTimes.put(warKey(attackerUUID, def), legacyStart);
                    }
                }
                System.out.println("[ShinobiAllianceMod] Loaded " + getAllWars().size() + " wars from disk (legacy, no grace times)");
            }
        } catch (IOException e) {
            System.err.println("[ShinobiAllianceMod] Failed to load wars: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Simple data class for war information
     */
    public static class WarInfo {
        public final UUID attackerUUID;
        public final UUID defenderUUID;
        
        public WarInfo(UUID attackerUUID, UUID defenderUUID) {
            this.attackerUUID = attackerUUID;
            this.defenderUUID = defenderUUID;
        }
    }

    /** Allies structure */
    private static class WarAllies {
        final Set<UUID> attackerSide = new HashSet<>();
        final Set<UUID> defenderSide = new HashSet<>();
    }
    private static class PendingAllies {
        final Set<UUID> attackerSide = new HashSet<>();
        final Set<UUID> defenderSide = new HashSet<>();
    }

    private record WarSave(Map<String, Set<String>> wars, Map<String, Long> starts) {}

    private static String warKey(UUID attacker, UUID defender) {
        return attacker.toString() + ":" + defender.toString();
    }

    /**
     * Add ally to all wars where kageUUID is attacker or defender; returns number of wars affected.
     */
    public static int addAllyToWars(UUID kageUUID, UUID allyUUID, MinecraftServer server) {
        int count = 0;
        // As attacker
        Set<UUID> defenders = activeWars.get(kageUUID);
        if (defenders != null) {
            for (UUID def : defenders) {
                WarAllies wa = warAllies.computeIfAbsent(warKey(kageUUID, def), k -> new WarAllies());
                if (wa.attackerSide.add(allyUUID)) {
                    String key = warKey(kageUUID, def);
                    startAllyGraceIfEligible(key, allyUUID);
                    if (isGracePeriodOver(kageUUID, def) && isAllyGraceOver(key, allyUUID)) {
                        applyAllyPermissions(allyUUID, def, key, server);
                    }
                    count++;
                }
            }
        }
        // As defender
        for (Map.Entry<UUID, Set<UUID>> entry : activeWars.entrySet()) {
            if (entry.getValue().contains(kageUUID)) {
                UUID attacker = entry.getKey();
                WarAllies wa = warAllies.computeIfAbsent(warKey(attacker, kageUUID), k -> new WarAllies());
                if (wa.defenderSide.add(allyUUID)) {
                    String key = warKey(attacker, kageUUID);
                    startAllyGraceIfEligible(key, allyUUID);
                    if (isGracePeriodOver(attacker, kageUUID) && isAllyGraceOver(key, allyUUID)) {
                        applyAllyPermissions(allyUUID, attacker, key, server);
                    }
                    count++;
                }
            }
        }
        return count;
    }

    private static void applyAllyPermissions(UUID allyUUID, UUID enemyKageUUID, String warKey, MinecraftServer server) {
        Set<UUID> applied = appliedAllyBypass.computeIfAbsent(warKey, k -> new HashSet<>());
        if (applied.contains(allyUUID)) return; // already granted

        ServerPlayer ally = server.getPlayerList().getPlayer(allyUUID);
        ServerPlayer enemy = server.getPlayerList().getPlayer(enemyKageUUID);
        if (ally == null || enemy == null) return;

        System.out.println("[ShinobiAllianceMod] Ally bypass activated: " + ally.getName().getString() + " in war against " + enemy.getName().getString());
        ally.sendSystemMessage(Component.literal("§eYou have joined the war. You may now break/interact with enemy claims."));
        applied.add(allyUUID);
    }

    /**
     * Called every server tick to activate bypasses once grace expires.
     */
    public static void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        for (WarInfo war : getAllWars()) {
            String key = warKey(war.attackerUUID, war.defenderUUID);
            Long start = warStartTimes.getOrDefault(key, now);
            if (now - start < GRACE_PERIOD_MILLIS) continue; // still in grace
            if (appliedBypass.contains(key)) {
                // Even if marked applied, ensure allies get access if they log in later
                ensureAlliesAccess(war.attackerUUID, war.defenderUUID, server);
                continue;
            }
            boolean applied = applyWarBypass(war.attackerUUID, war.defenderUUID, server);
            if (applied) {
                appliedBypass.add(key);
                ensureAlliesAccess(war.attackerUUID, war.defenderUUID, server);
            }
        }
    }

    /**
     * Called when any player joins to grant bypass if war is active and grace expired.
     */
    public static void handlePlayerLogin(ServerPlayer player, MinecraftServer server) {
        UUID uuid = player.getUUID();
        // Wars as attacker
        Set<UUID> defenders = activeWars.get(uuid);
        if (defenders != null) {
            for (UUID def : defenders) {
                maybeApplyAfterLogin(uuid, def, server);
            }
        }
        // Wars as defender
        for (Map.Entry<UUID, Set<UUID>> entry : activeWars.entrySet()) {
            if (entry.getValue().contains(uuid)) {
                maybeApplyAfterLogin(entry.getKey(), uuid, server);
            }
        }
            // If player is an ally in any war, start their ally grace on first login
        for (WarInfo war : getAllWars()) {
            String key = warKey(war.attackerUUID, war.defenderUUID);
            WarAllies allies = warAllies.get(key);
            if (allies == null) continue;
            if (allies.attackerSide.contains(uuid) || allies.defenderSide.contains(uuid)) {
                startAllyGraceOnLogin(key, uuid);
                // After their grace ends, they will be granted in tick()
            }
                // If pending, notify again
                PendingAllies pending = pendingAllies.get(key);
                if (pending != null && (pending.attackerSide.contains(uuid) || pending.defenderSide.contains(uuid))) {
                    player.sendSystemMessage(Component.literal("§eYou have a pending war invite. Use §a/war optin §eto participate."));
                }
        }
    }

    private static void maybeApplyAfterLogin(UUID attacker, UUID defender, MinecraftServer server) {
        if (!isGracePeriodOver(attacker, defender)) return;
        String key = warKey(attacker, defender);
        boolean applied = applyWarBypass(attacker, defender, server);
        if (applied) {
            appliedBypass.add(key);
        }
        ensureAlliesAccess(attacker, defender, server);
    }

    private static void ensureAlliesAccess(UUID attacker, UUID defender, MinecraftServer server) {
        if (!isGracePeriodOver(attacker, defender)) return;
        WarAllies allies = warAllies.get(warKey(attacker, defender));
        if (allies == null) return;
        String key = warKey(attacker, defender);
        for (UUID ally : allies.attackerSide) {
            if (isAllyGraceOver(key, ally)) applyAllyPermissions(ally, defender, key, server);
        }
        for (UUID ally : allies.defenderSide) {
            if (isAllyGraceOver(key, ally)) applyAllyPermissions(ally, attacker, key, server);
        }
    }

    private static boolean isGracePeriodOver(UUID attacker, UUID defender) {
        String key = warKey(attacker, defender);
        Long start = warStartTimes.get(key);
        if (start == null) return false;
        return System.currentTimeMillis() - start >= GRACE_PERIOD_MILLIS;
    }

    private static void startAllyGraceIfEligible(String key, UUID ally) {
        Map<UUID, Long> map = allyGraceStarts.computeIfAbsent(key, k -> new HashMap<>());
        map.putIfAbsent(ally, null); // will start on login
    }

    private static void startAllyGraceOnLogin(String key, UUID ally) {
        Map<UUID, Long> map = allyGraceStarts.computeIfAbsent(key, k -> new HashMap<>());
        if (!map.containsKey(ally) || map.get(ally) == null) {
            map.put(ally, System.currentTimeMillis());
        }
    }

    private static boolean isAllyGraceOver(String key, UUID ally) {
        Map<UUID, Long> map = allyGraceStarts.get(key);
        if (map == null) return false;
        Long start = map.get(ally);
        if (start == null) return false;
        return System.currentTimeMillis() - start >= GRACE_PERIOD_MILLIS;
    }

    /** Request ally opt-in (never auto-add allies). Returns number of wars added to pending. */
    public static int requestAllyOptIn(UUID kageUUID, UUID allyUUID, MinecraftServer server) {
        int count = 0;
        // As attacker
        Set<UUID> defenders = activeWars.get(kageUUID);
        if (defenders != null) {
            for (UUID def : defenders) {
                String key = warKey(kageUUID, def);
                PendingAllies p = pendingAllies.computeIfAbsent(key, k -> new PendingAllies());
                if (p.attackerSide.add(allyUUID)) count++;
            }
        }
        // As defender
        for (Map.Entry<UUID, Set<UUID>> entry : activeWars.entrySet()) {
            if (entry.getValue().contains(kageUUID)) {
                String key = warKey(entry.getKey(), kageUUID);
                PendingAllies p = pendingAllies.computeIfAbsent(key, k -> new PendingAllies());
                if (p.defenderSide.add(allyUUID)) count++;
            }
        }
        if (count > 0) {
            ServerPlayer ally = server.getPlayerList().getPlayer(allyUUID);
            if (ally != null) ally.sendSystemMessage(Component.literal("§eYou were invited to join a war. Use §a/war optin §eto participate."));
        }
        return count;
    }

    /** Ally executes opt-in; adds them to war allies and starts their grace. */
    public static int optInAlly(UUID allyUUID, MinecraftServer server) {
        int joined = 0;
        for (WarInfo war : getAllWars()) {
            String key = warKey(war.attackerUUID, war.defenderUUID);
            PendingAllies p = pendingAllies.get(key);
            if (p == null) continue;
            boolean onAttacker = p.attackerSide.remove(allyUUID);
            boolean onDefender = p.defenderSide.remove(allyUUID);
            if (!onAttacker && !onDefender) continue;
            WarAllies wa = warAllies.computeIfAbsent(key, k -> new WarAllies());
            if (onAttacker) wa.attackerSide.add(allyUUID);
            if (onDefender) wa.defenderSide.add(allyUUID);
            startAllyGraceIfEligible(key, allyUUID);
            startAllyGraceOnLogin(key, allyUUID);
            joined++;
        }
        return joined;
    }

    private static void autoRegisterPartyMembers(ServerPlayer attacker, ServerPlayer defender, MinecraftServer server) {
        try {
            xaero.pac.common.server.api.OpenPACServerAPI api = xaero.pac.common.server.api.OpenPACServerAPI.get(server);
            if (api == null) return;
            var pm = api.getPartyManager();
            if (pm == null) return;
            // Collect members for both sides
            registerSidePartyMembers(attacker, defender.getUUID(), server, pm, true);
            registerSidePartyMembers(defender, attacker.getUUID(), server, pm, false);
        } catch (Throwable ignored) {}
    }

    private static void registerSidePartyMembers(ServerPlayer kage, UUID enemyUUID, MinecraftServer server,
                                                 Object partyManager, boolean attackerSide) {
        try {
            java.lang.reflect.Method getPartyOfPlayer = partyManager.getClass().getMethod("getPartyOfPlayer", java.util.UUID.class);
            Object party = getPartyOfPlayer.invoke(partyManager, kage.getUUID());
            if (party == null) return;
            java.lang.reflect.Method getMembers = party.getClass().getMethod("getMembers");
            java.util.Collection<?> members = (java.util.Collection<?>) getMembers.invoke(party);
            if (members == null) return;
            WarAllies wa = warAllies.computeIfAbsent(warKey(attackerSide ? kage.getUUID() : enemyUUID,
                                                            attackerSide ? enemyUUID : kage.getUUID()), k -> new WarAllies());
            for (Object memberObj : members) {
                java.lang.reflect.Method getUUID = memberObj.getClass().getMethod("getUUID");
                UUID memberUUID = (UUID) getUUID.invoke(memberObj);
                if (memberUUID == null || memberUUID.equals(kage.getUUID())) continue;
                ServerPlayer member = server.getPlayerList().getPlayer(memberUUID);
                Rank r = (member != null) ? PlayerPointsManager.getRank(member) : Rank.GENIN; // default if offline
                if (r == Rank.KAGE) {
                    // Require opt-in
                    String wk = warKey(attackerSide ? kage.getUUID() : enemyUUID,
                                       attackerSide ? enemyUUID : kage.getUUID());
                    PendingAllies p = pendingAllies.computeIfAbsent(wk, k -> new PendingAllies());
                    if (attackerSide) p.attackerSide.add(memberUUID); else p.defenderSide.add(memberUUID);
                    if (member != null) {
                        member.sendSystemMessage(Component.literal("§eWar declared by your party's Kage. Use §a/war optin §eto participate after a 1h grace."));
                    }
                } else {
                    // Auto-group non-Kage; ally grace starts on first login
                    if (attackerSide) wa.attackerSide.add(memberUUID); else wa.defenderSide.add(memberUUID);
                    startAllyGraceIfEligible(warKey(attackerSide ? kage.getUUID() : enemyUUID,
                                                    attackerSide ? enemyUUID : kage.getUUID()), memberUUID);
                }
            }
        } catch (Throwable ignored) {}
    }
}
