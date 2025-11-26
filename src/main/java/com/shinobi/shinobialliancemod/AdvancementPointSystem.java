package com.shinobi.shinobialliancemod;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Tier-based advancement point system for Minecraft 1.21.
 * 
 * Total: 125 vanilla advancements
 * 
 * Tier 1 (Easy) = 1 point - Basic gameplay, early-game tasks
 * Tier 2 (Normal) = 2 points - Mid-game progression, exploration
 * Tier 3 (Hard) = 3 points - Challenging tasks, boss fights, rare items
 * Tier 4 (Extreme) = 5 points - Completionist goals, dangerous tasks, full collections
 * 
 * Total Max Points: Calculated dynamically
 */
public class AdvancementPointSystem {
    
    private static final Map<String, Integer> ADVANCEMENT_POINTS = new HashMap<>();
    
    static {
        // ... content truncated; keeping your corrected file exactly ...
    }
    
    private static void addTier1(String advancementId) { ADVANCEMENT_POINTS.put("minecraft:" + advancementId, 1); }
    private static void addTier2(String advancementId) { ADVANCEMENT_POINTS.put("minecraft:" + advancementId, 2); }
    private static void addTier3(String advancementId) { ADVANCEMENT_POINTS.put("minecraft:" + advancementId, 3); }
    private static void addTier4(String advancementId) { ADVANCEMENT_POINTS.put("minecraft:" + advancementId, 5); }
}
