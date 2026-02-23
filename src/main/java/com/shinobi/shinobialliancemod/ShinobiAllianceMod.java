package com.shinobi.shinobialliancemod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ShinobiAllianceMod implements ModInitializer {
    // Track players who still need to pick a village
    private static final Set<UUID> NEED_VILLAGE = new HashSet<>();

    public static boolean hasSelectedVillage(UUID playerUUID) { return !NEED_VILLAGE.contains(playerUUID); }
    public static void clearPlayerCache(UUID playerUUID) { NEED_VILLAGE.remove(playerUUID); }
    public static void markPlayerAsNeedingVillageSelection(UUID playerUUID) { NEED_VILLAGE.add(playerUUID); }

    private static boolean hasAnyVillageTeam(ServerPlayer player) {
        var scoreboard = player.level().getServer().getScoreboard();
        var team = scoreboard.getPlayersTeam(player.getScoreboardName());
        if (team == null) return false;
        String name = team.getName();
        for (Village v : Village.values()) {
            if (name.startsWith(v.getId() + "_")) return true;
            if (name.equals(v.getId())) return true;
        }
        return false;
    }
    @Override
    public void onInitialize() {
        // Register war event handlers to bypass OPAC claims during active wars
        WarEventHandler.register();
        
        // Register custom packet payloads
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(
            com.shinobi.shinobialliancemod.network.HudUpdatePayload.TYPE,
            com.shinobi.shinobialliancemod.network.HudUpdatePayload.CODEC
        );
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(
            com.shinobi.shinobialliancemod.network.HudVisibilityPayload.TYPE,
            com.shinobi.shinobialliancemod.network.HudVisibilityPayload.CODEC
        );
        
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            if (player == null) return;

            // Initialize scoreboard teams once
            ScoreboardTeamManager.initializeTeams(server);
            // Determine onboarding state: no village team & not previously selected
            boolean needsVillage = !hasAnyVillageTeam(player) || NEED_VILLAGE.contains(player.getUUID());
            if (needsVillage) {
                // Mark and start onboarding (freeze + scroll)
                markPlayerAsNeedingVillageSelection(player.getUUID());
                PlayerFreezeManager.freezePlayer(player);
                var scroll = ShinobiItems.createVillageScroll();
                if (!player.getInventory().add(scroll)) player.drop(scroll, false);
                player.sendSystemMessage(Component.literal("§eWelcome! Use §a/village choose <leaf|sand|mist|stone|cloud>§e to begin."));
                player.sendSystemMessage(Component.literal("§7You are frozen until you pick a village."));
                System.out.println("[ShinobiAllianceMod] Onboarding (needs village) for " + player.getName().getString());
                // Apply default Genin claim limits even before village selection
                ShinobiClaimBridge.applyClaimLimits(player);
            } else {
                // Already selected a village previously
                LuckPermsService.syncPlayerGroups(player, server);
                ShinobiClaimBridge.applyClaimLimits(player);
                ShinobiScheduler.initializePlayerState(player);
                WarManager.handlePlayerLogin(player, server);
            }
        });

        // Register Brigadier commands (/shinobi, /declarewar, etc.)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Only register on dedicated/server environments; dispatcher is shared
            ShinobiCommands.register(dispatcher);
        });

        // Handle right-click on village scroll to open GUI
        UseItemCallback.EVENT.register((player, world, hand) -> {
            // Only act on server side; let client and all other items proceed normally
            if (world.isClientSide()) {
                return net.minecraft.world.InteractionResult.PASS;
            }

            var stack = player.getItemInHand(hand);
            if (player instanceof ServerPlayer sp 
                && ShinobiItems.isVillageScroll(stack)
                && NEED_VILLAGE.contains(sp.getUUID())) { // Only intercept during onboarding
                // Create GUI container with 5 village items
                var container = new SimpleContainer(27);
                container.setItem(11, NinjaScrollItem.createVillageItem(Village.LEAF));
                container.setItem(12, NinjaScrollItem.createVillageItem(Village.SAND));
                container.setItem(13, NinjaScrollItem.createVillageItem(Village.MIST));
                container.setItem(14, NinjaScrollItem.createVillageItem(Village.STONE));
                container.setItem(15, NinjaScrollItem.createVillageItem(Village.CLOUD));

                // Open menu
                sp.openMenu(new SimpleMenuProvider(
                    (syncId, playerInventory, p) -> new VillageSelectionMenu(syncId, playerInventory, container),
                    Component.literal("Village Selection")
                ));

                // We handled this interaction; prevent vanilla book use and do not modify inventory
                return net.minecraft.world.InteractionResult.SUCCESS;
            }

            // Not our scroll: do not interfere
            return net.minecraft.world.InteractionResult.PASS;
        });

        // Clean up HUD bossbars on disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;
            if (player != null) {
                ShinobiScheduler.unpinHud(player);
            }
        });

        // Initialize systems when server fully started
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            WarManager.initialize(server);
            ShinobiScheduler.start();
            ShinobiItems.initialize();
            ScoreboardTeamManager.initializeTeams(server);
        });

        // Enforce freeze every server tick so players truly cannot move until selection
        // Also refresh HUD every 2 ticks for pinned players
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (PlayerFreezeManager.isFrozen(p)) {
                    PlayerFreezeManager.enforceFreeze(p);
                    
                    // Auto-respawn village scroll if lost/dropped/destroyed
                    if (NEED_VILLAGE.contains(p.getUUID())) {
                        boolean hasScroll = false;
                        var inv = p.getInventory();
                        for (int i = 0; i < inv.getContainerSize(); i++) {
                            if (ShinobiItems.isVillageScroll(inv.getItem(i))) {
                                hasScroll = true;
                                break;
                            }
                        }
                        if (!hasScroll) {
                            var scroll = ShinobiItems.createVillageScroll();
                            if (!inv.add(scroll)) {
                                p.drop(scroll, false);
                            }
                        }
                    }
                }
            }
            
            // Refresh HUD every 2 ticks (0.1 seconds) for better persistence
            ShinobiScheduler.tickHudRefresh(server);
            // Activate war bypasses after grace periods
            WarManager.tick(server);
        });
    }
}
