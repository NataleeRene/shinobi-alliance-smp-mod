package com.shinobi.shinobialliancemod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerFreezeManager {
    private static final Map<UUID, FreezeData> frozenPlayers = new HashMap<>();
    
    // Fixed spawn location for village selection
    private static final double SPAWN_X = -32.0;
    private static final double SPAWN_Y = 111.0;
    private static final double SPAWN_Z = -3.0;
    private static final Vec3 SPAWN_POSITION = new Vec3(SPAWN_X, SPAWN_Y, SPAWN_Z);
    
    private static class FreezeData {
        Vec3 position;
        boolean frozen;
        
        FreezeData(Vec3 position) {
            this.position = position;
            this.frozen = true;
        }
    }

    /**
     * Teleport player to fixed spawn location and freeze them
     */
    public static void freezePlayer(ServerPlayer player) {
        // Teleport to fixed spawn location
        player.teleportTo(SPAWN_X, SPAWN_Y, SPAWN_Z);
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        
        // Store freeze data at spawn position
        frozenPlayers.put(player.getUUID(), new FreezeData(SPAWN_POSITION));
        System.out.println("[ShinobiAllianceMod] Teleported and froze player " + player.getName().getString() + 
            " at spawn position (" + SPAWN_X + ", " + SPAWN_Y + ", " + SPAWN_Z + ")");
    }

    public static void unfreezePlayer(ServerPlayer player) {
        frozenPlayers.remove(player.getUUID());
        System.out.println("[ShinobiAllianceMod] Unfroze player " + player.getName().getString());
    }

    public static boolean isFrozen(ServerPlayer player) {
        return frozenPlayers.containsKey(player.getUUID()) && frozenPlayers.get(player.getUUID()).frozen;
    }

    public static Vec3 getLockPosition(ServerPlayer player) {
        FreezeData data = frozenPlayers.get(player.getUUID());
        return data != null ? data.position : null;
    }

    public static void enforceFreeze(ServerPlayer player) {
        if (!isFrozen(player)) {
            return;
        }

        Vec3 lockPos = getLockPosition(player);
        if (lockPos == null) {
            return;
        }

        Vec3 currentPos = player.position();
        double distance = lockPos.distanceTo(currentPos);

        if (distance > 0.1) {
            player.teleportTo(lockPos.x, lockPos.y, lockPos.z);
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
        }
    }
}
