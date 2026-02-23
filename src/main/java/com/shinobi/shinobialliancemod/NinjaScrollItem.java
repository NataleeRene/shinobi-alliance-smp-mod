package com.shinobi.shinobialliancemod;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class NinjaScrollItem {

    /**
     * Create a village item for the selection GUI.
     * This is a static helper that creates the display items.
     */
    public static ItemStack createVillageItem(Village village) {
        ItemStack item;
        String displayName;
        net.minecraft.ChatFormatting color;
        
        switch (village) {
            case LEAF:
                item = new ItemStack(Items.FIRE_CHARGE);
                displayName = "Leaf Village";
                color = net.minecraft.ChatFormatting.RED; // §c - matches Leaf rank color
                break;
            case SAND:
                item = new ItemStack(Items.SAND);
                displayName = "Sand Village";
                color = net.minecraft.ChatFormatting.GOLD; // §e - matches Sand rank color (yellow/gold)
                break;
            case MIST:
                item = new ItemStack(Items.TRIDENT);
                displayName = "Mist Village";
                color = net.minecraft.ChatFormatting.DARK_BLUE; // §9 - matches Mist rank color
                break;
            case STONE:
                item = new ItemStack(Items.STONE);
                displayName = "Stone Village";
                color = net.minecraft.ChatFormatting.DARK_GRAY; // §8 - matches Stone rank color
                break;
            case CLOUD:
                item = new ItemStack(Items.LIGHTNING_ROD);
                displayName = "Cloud Village";
                color = net.minecraft.ChatFormatting.WHITE; // §f - matches Cloud rank color
                break;
            default:
                item = new ItemStack(Items.BARRIER);
                displayName = "Unknown";
                color = net.minecraft.ChatFormatting.RED;
        }
        
        item.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
            Component.literal(displayName).withStyle(color)); // No bold formatting
        
        return item;
    }
}
