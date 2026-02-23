package com.shinobi.shinobialliancemod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reads Open Parties and Claims (OPAC) per-player claim counts directly from the
 * world data NBT files: data/openpartiesandclaims/playerclaims/<UUID>.nbt
 *
 * We count the total number of claimed chunks as the sum of the sizes of each
 * claim's "positions" list across all dimensions.
 */
final class OpacNbtReader {
    private static final boolean DEBUG = false;
    private static final String OPAC_RELATIVE_PATH = "data/openpartiesandclaims/player-claims";

    private static class CacheEntry {
        long lastReadMs;
        long lastModified;
        int value;
    }

    private static final Map<UUID, CacheEntry> CACHE = new HashMap<>();

    private OpacNbtReader() {}

    static int getUsedClaims(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            if (DEBUG) System.out.println("[OpacNbtReader] Server is null for " + player.getName().getString());
            return 0;
        }

        Path worldRoot = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
        Path claimsDir = worldRoot.resolve(OPAC_RELATIVE_PATH);
        Path playerFile = claimsDir.resolve(player.getUUID().toString() + ".nbt");

        if (DEBUG) System.out.println("[OpacNbtReader] Looking for claims file: " + playerFile.toAbsolutePath());
        
        File file = playerFile.toFile();
        if (!file.exists()) {
            if (DEBUG) System.out.println("[OpacNbtReader] Claims file does not exist for " + player.getName().getString());
            return 0;
        }

        long now = System.currentTimeMillis();
        CacheEntry ce = CACHE.get(player.getUUID());
        long lastMod = file.lastModified();
        if (ce != null && ce.lastModified == lastMod && (now - ce.lastReadMs) < 1000) {
            return ce.value;
        }

        int total = 0;
        try (InputStream in = Files.newInputStream(playerFile)) {
            // Detect gzip header (0x1f 0x8b); fallback to uncompressed read
            byte[] header = in.readNBytes(2);
            CompoundTag root;
            if (header.length == 2 && (header[0] & 0xFF) == 0x1F && (header[1] & 0xFF) == 0x8B) {
                // Re-open stream for compressed read
                try (InputStream gz = Files.newInputStream(playerFile)) {
                    root = NbtIo.readCompressed(gz, NbtAccounter.unlimitedHeap());
                }
            } else {
                // Use uncompressed NBT reader (DataInput required)
                try (InputStream plain = Files.newInputStream(playerFile)) {
                    var dataIn = new java.io.DataInputStream(plain);
                    root = NbtIo.read(dataIn, NbtAccounter.unlimitedHeap());
                }
            }
            if (root == null) {
                if (DEBUG) System.out.println("[OpacNbtReader] Root tag is null");
                return 0;
            }

            CompoundTag dimensions = root.getCompound("dimensions").orElse(null);
            if (dimensions == null) {
                if (DEBUG) System.out.println("[OpacNbtReader] No dimensions tag found");
                return 0;
            }
            if (DEBUG) System.out.println("[OpacNbtReader] Found dimensions tag, parsing claims...");

            String[] dimKeys = new String[] {
                "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"
            };

            for (String dimKey : dimKeys) {
                CompoundTag dim = dimensions.getCompound(dimKey).orElse(null);
                if (dim == null) continue;
                var claimsOpt = dim.getList("claims");
                if (claimsOpt.isEmpty()) continue;
                ListTag claims = claimsOpt.get();
                for (int i = 0; i < claims.size(); i++) {
                    CompoundTag claim = claims.getCompound(i).orElse(null);
                    if (claim == null) continue;
                    var positionsOpt = claim.getList("positions");
                    if (positionsOpt.isEmpty()) continue;
                    ListTag positions = positionsOpt.get();
                    total += positions.size();
                }
            }

            if (DEBUG) System.out.println("[OpacNbtReader] Total claims for " + player.getName().getString() + ": " + total);
            
            CacheEntry updated = ce == null ? new CacheEntry() : ce;
            updated.lastReadMs = now;
            updated.lastModified = lastMod;
            updated.value = total;
            CACHE.put(player.getUUID(), updated);
            return total;
        } catch (Throwable e) {
            if (DEBUG) {
                System.out.println("[OpacNbtReader] Error reading NBT: " + e.getMessage());
                e.printStackTrace();
            }
            return 0;
        }
    }
}
