package com.shinobi.shinobialliancemod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Custom menu for village selection.
 * Detects clicks on the 5 village items and assigns the player accordingly.
 */
public class VillageSelectionMenu extends ChestMenu {
    
    public VillageSelectionMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(27));
    }
    
    public VillageSelectionMenu(int containerId, Inventory playerInventory, Container container) {
        super(MenuType.GENERIC_9x3, containerId, playerInventory, container, 3);
    }
    
    /**
     * Called when player clicks a slot in the menu.
     * Detects village selection and assigns the player.
     */
    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
        // Prevent normal item interactions - this is a selection menu only
        if (slotId < 0 || slotId >= this.getContainer().getContainerSize()) {
            return;
        }
        
        ItemStack clicked = this.getContainer().getItem(slotId);
        if (clicked.isEmpty()) {
            return;
        }
        
        // Only process on server side
        if (player.level().isClientSide()) {
            return;
        }
        
        ServerPlayer serverPlayer = (ServerPlayer) player;
        Village selectedVillage = null;
        
        // Match clicked item to village
        if (clicked.is(Items.FIRE_CHARGE) || clicked.is(Items.CAMPFIRE)) {
            selectedVillage = Village.LEAF;
        } else if (clicked.is(Items.SAND)) {
            selectedVillage = Village.SAND;
        } else if (clicked.is(Items.TRIDENT)) {
            selectedVillage = Village.MIST;
        } else if (clicked.is(Items.STONE)) {
            selectedVillage = Village.STONE;
        } else if (clicked.is(Items.LIGHTNING_ROD)) {
            selectedVillage = Village.CLOUD;
        }
        
        if (selectedVillage != null) {
            handleVillageSelection(serverPlayer, selectedVillage);
            // Close menu immediately
            this.removed(serverPlayer);
            serverPlayer.containerMenu = serverPlayer.inventoryMenu;
        }
    }
    
    /**
     * Process village selection: assign village, unfreeze, notify Kage, remove scroll.
     */
    private void handleVillageSelection(ServerPlayer player, Village village) {
        var server = player.level().getServer();
        if (server == null) return;
        
        System.out.println("[ShinobiAllianceMod] " + player.getName().getString() + " selected village: " + village.getDisplayName());
        
        // 1. Assign village via LuckPerms (adds village group + village_genin group)
        LuckPermsService.assignVillage(player, village, server);
        
        // 2. Assign to Genin team on scoreboard
        ScoreboardTeamManager.assignPlayerRankTeam(player, village, Rank.GENIN, server);
        
        // 3. Set initial rank
        PlayerPointsManager.setRank(player, Rank.GENIN);
        
        // 4. Apply initial claim limits
        ShinobiClaimBridge.applyClaimLimits(player);
        
        // 5. Mark as having selected village
        ShinobiAllianceMod.clearPlayerCache(player.getUUID());
        
        // 6. Unfreeze player
        PlayerFreezeManager.unfreezePlayer(player);
        
        // 7. Remove village scroll from inventory
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (ShinobiItems.isVillageScroll(stack)) {
                inv.removeItem(i, 1);
                break;
            }
        }
        
        // 8. Notify player
        player.sendSystemMessage(Component.literal("§aVillage selected: §6" + village.getDisplayName() + " §7(You are now a Genin)"));
        
        // 9. Broadcast to all Kage of that village
        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            Village playerVillage = RankManager.getPlayerVillage(onlinePlayer, server);
            Rank rank = PlayerPointsManager.getRank(onlinePlayer);
            if (playerVillage == village && rank == Rank.KAGE) {
                onlinePlayer.sendSystemMessage(Component.literal(
                    "§6[" + village.getDisplayName() + " Kage Notice] §f" + player.getName().getString() + " §ahas joined your village!"
                ));
            }
        }
        
        // 10. Save player data
        PlayerDataManager.PlayerData data = new PlayerDataManager.PlayerData(true, village.getDisplayName(), "Genin", 0);
        PlayerDataManager.savePlayerData(player.getUUID(), data, server);
        
        System.out.println("[ShinobiAllianceMod] Village selection complete for " + player.getName().getString());
    }
    
    /**
     * Prevent shift-clicking items out of the menu.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
    
    /**
     * Prevent taking items from the menu.
     */
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
