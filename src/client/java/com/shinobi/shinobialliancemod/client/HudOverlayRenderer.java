package com.shinobi.shinobialliancemod.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Renders the Shinobi HUD overlay on the top-right corner of the screen.
 * Two lines:
 * - Line 1: Village: <name>  Rank: <rank>
 * - Line 2: Points: <points>  Claims: <claimed>/<limit>
 */
public class HudOverlayRenderer {
    private static String village = "";
    private static String rank = "";
    private static int points = 0;
    private static int claimedCount = 0;
    private static int claimLimit = 0;
    private static boolean visible = false;
    // Colors (ARGB) per spec
    private static final int COLOR_HEADER = 0xFFFF9AA5;  // header pink (#ff9aa5)
    private static final int COLOR_RESULT = 0xFF7CFC00;  // lawn green for result values
    private static final int COLOR_RANK = 0xFFFF9AA5;    // headers include rank label/value styling

    // Village name -> color mapping (ARGB) - matches Village.java enum colors
    private static int colorForVillage(String v) {
        if (v == null) return COLOR_RESULT;
        String s = v.trim().toLowerCase();
        switch (s) {
            case "leaf":
                return 0xFFFF5555; // §c = red
            case "sand":
                return 0xFFFFFF55; // §e = yellow
            case "mist":
                return 0xFF5555FF; // §9 = dark blue
            case "stone":
                return 0xFF555555; // §8 = dark gray
            case "cloud":
                return 0xFFFFFFFF; // §f = white
            default:
                return COLOR_RESULT; // fallback to result color
        }
    }

    public static void register() {
        HudRenderCallback.EVENT.register(HudOverlayRenderer::render);
    }

    public static void setVisible(boolean show) {
        visible = show;
    }

    public static void updateData(String villageVal, String rankVal, int pointsVal, int claimedVal, int limitVal) {
        village = villageVal;
        rank = rankVal;
        points = pointsVal;
        claimedCount = claimedVal;
        claimLimit = limitVal;
    }

    private static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        if (!visible) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Position: middle-right side with 10px padding from edge
        int x = screenWidth - 10;
        int y = (screenHeight / 2) - 10; // Center vertically
        int lineHeight = 10;

        // Line 1 rendered in segments for color control
        Component lblVillage = Component.literal("Village: ");
        Component valVillage = Component.literal(village);
        Component lblRank = Component.literal("  Rank: ");
        Component valRank = Component.literal(rank);

        int line1Width = mc.font.width(lblVillage) + mc.font.width(valVillage)
            + mc.font.width(lblRank) + mc.font.width(valRank);
        int cx = x - line1Width;
        graphics.drawString(mc.font, lblVillage, cx, y, COLOR_HEADER, true);
        cx += mc.font.width(lblVillage);
        graphics.drawString(mc.font, valVillage, cx, y, colorForVillage(village), true);
        cx += mc.font.width(valVillage);
        graphics.drawString(mc.font, lblRank, cx, y, COLOR_HEADER, true);
        cx += mc.font.width(lblRank);
        graphics.drawString(mc.font, valRank, cx, y, COLOR_RESULT, true);

        // Line 2: Points and Claims with colors
        Component lblPoints = Component.literal("Points: ");
        Component valPoints = Component.literal(Integer.toString(points));
        Component lblClaims = Component.literal("  Claims: ");
        Component valClaims = Component.literal(claimedCount + "/" + claimLimit);

        int line2Width = mc.font.width(lblPoints) + mc.font.width(valPoints)
            + mc.font.width(lblClaims) + mc.font.width(valClaims);
        int cx2 = x - line2Width;
        graphics.drawString(mc.font, lblPoints, cx2, y + lineHeight, COLOR_HEADER, true);
        cx2 += mc.font.width(lblPoints);
        graphics.drawString(mc.font, valPoints, cx2, y + lineHeight, COLOR_RESULT, true);
        cx2 += mc.font.width(valPoints);
        graphics.drawString(mc.font, lblClaims, cx2, y + lineHeight, COLOR_HEADER, true);
        cx2 += mc.font.width(lblClaims);
        graphics.drawString(mc.font, valClaims, cx2, y + lineHeight, COLOR_RESULT, true);
    }
}
