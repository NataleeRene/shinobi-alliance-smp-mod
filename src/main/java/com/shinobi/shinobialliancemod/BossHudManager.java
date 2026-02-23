package com.shinobi.shinobialliancemod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player BossBar HUD manager.
 * - Title uses full hex color text segments
 * - Bar color selectable (uses vanilla BossEvent.Color)
 * - Progress toggleable
 */
public final class BossHudManager {
    private static final Map<UUID, ServerBossEvent> bars = new HashMap<>();

    private static BossEvent.BossBarColor BAR_COLOR = BossEvent.BossBarColor.WHITE; // white container with progress fill
    private static BossEvent.BossBarOverlay BAR_OVERLAY = BossEvent.BossBarOverlay.PROGRESS; // solid progress style

    private BossHudManager() {}

    public static void ensure(ServerPlayer player) {
        bars.computeIfAbsent(player.getUUID(), uuid -> {
            ServerBossEvent event = new ServerBossEvent(Component.literal("Shinobi HUD"), BAR_COLOR, BAR_OVERLAY);
            event.setDarkenScreen(false);
            event.setCreateWorldFog(false);
            event.setPlayBossMusic(false);
            event.addPlayer(player);
            return event;
        });
    }

    public static void showOrUpdate(ServerPlayer player, Component title, int points, int maxPoints, boolean showProgress) {
        ensure(player);
        ServerBossEvent event = bars.get(player.getUUID());
        if (event == null) return;
        event.setName(title);
        float progress = 0f;
        if (showProgress && maxPoints > 0) {
            progress = Math.max(0f, Math.min(1f, (float) points / (float) maxPoints));
        }
        event.setProgress(progress);
        event.setVisible(true);
    }

    public static void hide(ServerPlayer player) {
        ServerBossEvent event = bars.remove(player.getUUID());
        if (event != null) {
            event.removePlayer(player);
            event.setVisible(false);
        }
    }

    public static void setBarStyle(BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
        BAR_COLOR = color;
        BAR_OVERLAY = overlay;
        // Existing bars will update style on next ensure/show
    }
}
