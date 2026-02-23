package com.shinobi.shinobialliancemod;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Simplified war event handler - only handles PvP between war participants
 * Claim protection is disabled entirely for warring players via OPAC PlayerConfig API
 */
public class WarEventHandler {
    
    public static void register() {
        // Allow PvP between war participants (optional - PvP may already be enabled server-wide)
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer attacker)) return InteractionResult.PASS;
            if (!(entity instanceof ServerPlayer defender)) return InteractionResult.PASS;
            
            // Check if they're at war with bypass active
            if (WarManager.isAtWarWithBypassActive(attacker.getUUID(), defender.getUUID())) {
                // They're at war - allow PvP
                return InteractionResult.PASS;
            }
            
            return InteractionResult.PASS;
        });
    }
}
