package com.shinobi.shinobialliancemod;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public class ShinobiItems {

    // NBT tags for identifying village books
    public static final String SHINOBI_SCROLL_TAG = "ShinobiScroll";
    public static final String CUSTOM_MODEL_DATA_TAG = "CustomModelData";
    public static final int VILLAGE_SCROLL_MODEL_DATA = 7777;

    /**
     * Initialize village scroll item system.
     */
    public static void initialize() {
        System.out.println("[ShinobiAllianceMod] Village selection scroll system ready (using written book with NBT)");
    }
    
    /**
     * Create a Village Selection Scroll using written book with NBT tag.
     * Right-clicking is handled by UseItemCallback event.
     */
    public static ItemStack createVillageScroll() {
        ItemStack scroll = new ItemStack(Items.WRITTEN_BOOK);
        
        // Mark with NBT so we can detect it
        CompoundTag nbtData = new CompoundTag();
        nbtData.putByte(SHINOBI_SCROLL_TAG, (byte) 1);
        scroll.set(DataComponents.CUSTOM_DATA, CustomData.of(nbtData));
        
        // Set custom display name
        scroll.set(DataComponents.CUSTOM_NAME, 
            net.minecraft.network.chat.Component.literal("Village Selection Scroll")
                .withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD)
        );
        
        System.out.println("[ShinobiAllianceMod] Created Village Selection Scroll (written book with NBT)");
        
        return scroll;
    }
    
    /**
     * Check if an ItemStack is a village scroll.
     */
    public static boolean isVillageScroll(ItemStack stack) {
        if (stack.getItem() != Items.WRITTEN_BOOK) return false;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag nbt = customData.copyTag();
            return nbt.contains(SHINOBI_SCROLL_TAG);
        }
        return false;
    }
    
    /**
     * System is always ready since we use vanilla items.
     */
    public static boolean isRegistrationComplete() {
        return true; // Always ready - no registration needed
    }
}
