package com.shinobi.shinobialliancemod;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Tier-based advancement point system for Minecraft 1.21.10
 * 
 * Total: 124 vanilla advancements (matching AA Tool official count)
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
        // ============================================================
        // STORY PROGRESSION (Minecraft Tab) - 16 advancements
        // ============================================================
        
        // Tier 1 - Early Game Basics (15 advancements)
        addTier1("story/root");                    // Minecraft
        addTier1("story/mine_stone");              // Stone Age
        addTier1("story/upgrade_tools");           // Getting an Upgrade
        addTier1("story/smelt_iron");              // Acquire Hardware
        addTier1("story/obtain_armor");            // Suit Up
        addTier1("story/lava_bucket");             // Hot Stuff
        addTier1("story/iron_tools");              // Isn't It Iron Pick
        addTier1("story/deflect_arrow");           // Not Today, Thank You
        addTier1("story/form_obsidian");           // Ice Bucket Challenge
        addTier1("story/mine_diamond");            // Diamonds!
        addTier1("story/enter_the_nether");        // We Need to Go Deeper
        addTier1("story/shiny_gear");              // Cover Me with Diamonds
        addTier1("story/enchant_item");            // Enchanter
        addTier1("story/cure_zombie_villager");    // Zombie Doctor
        addTier1("story/follow_ender_eye");        // Eye Spy
        
        // Tier 2 - Mid Game (1 advancement)
        addTier2("story/enter_the_end");           // The End?
        
        
        // ============================================================
        // NETHER - 23 advancements
        // ============================================================
        
        // Tier 1 - Easy Nether (1 advancement)
        addTier1("nether/root");                   // Nether
        
        // Tier 2 - Mid Nether (10 advancements)
        addTier2("nether/find_bastion");           // Those Were the Days
        addTier2("nether/find_fortress");          // A Terrible Fortress
        addTier2("nether/obtain_crying_obsidian"); // Who is Cutting Onions?
        addTier2("nether/distract_piglin");        // Oh Shiny
        addTier2("nether/ride_strider");           // This Boat Has Legs
        addTier2("nether/netherite_armor");        // Cover Me in Debris
        addTier2("nether/obtain_blaze_rod");       // Into Fire
        addTier2("nether/charge_respawn_anchor");  // Not Quite "Nine" Lives
        addTier2("nether/brew_potion");            // Local Brewery
        addTier2("nether/create_beacon");          // Bring Home the Beacon
        
        // Tier 3 - Hard Nether (6 advancements)
        addTier3("nether/fast_travel");            // Subspace Bubble
        addTier3("nether/obtain_ancient_debris");  // Hidden in the Depths
        addTier3("nether/ride_strider_in_overworld_lava"); // Feels Like Home
        addTier3("nether/return_to_sender");       // Return to Sender
        addTier3("nether/loot_bastion");           // War Pigs
        addTier3("nether/get_wither_skull");       // Spooky Scary Skeleton
        
        // Tier 4 - Extreme Nether (6 advancements)
        addTier4("nether/uneasy_alliance");        // Uneasy Alliance
        addTier4("nether/explore_nether");         // Hot Tourist Destinations
        addTier4("nether/summon_wither");          // Withering Heights
        addTier4("nether/all_potions");            // A Furious Cocktail
        addTier4("nether/create_full_beacon");     // Beaconator
        addTier4("nether/all_effects");            // How Did We Get Here?
        
        
        // ============================================================
        // THE END - 9 advancements
        // ============================================================
        
        // Tier 1 - Easy End (1 advancement)
        addTier1("end/root");                      // The End
        
        // Tier 3 - Hard End (6 advancements)
        addTier3("end/kill_dragon");               // Free the End
        addTier3("end/dragon_egg");                // The Next Generation
        addTier3("end/enter_end_gateway");         // Remote Getaway
        addTier3("end/find_end_city");             // The City at the End of the Game
        addTier3("end/elytra");                    // Sky's the Limit
        addTier3("end/dragon_breath");             // You Need a Mint
        
        // Tier 4 - Extreme End (2 advancements)
        addTier4("end/respawn_dragon");            // The End... Again...
        addTier4("end/levitate");                  // Great View From Up Here
        
        
        // ============================================================
        // ADVENTURE - 46 advancements
        // ============================================================
        
        // Tier 1 - Easy Adventure (10 advancements)
        addTier1("adventure/root");                    // Adventure
        addTier1("adventure/spyglass_at_parrot");      // Is It a Bird?
        addTier1("adventure/kill_a_mob");              // Monster Hunter
        addTier1("adventure/read_power_of_chiseled_bookshelf"); // The Power of Books
        addTier1("adventure/trade");                    // What a Deal!
        addTier1("adventure/honey_block_slide");        // Sticky Situation
        addTier1("adventure/salvage_sherd");           // Respecting the Remnants
        addTier1("adventure/avoid_vibration");         // Sneak 100
        addTier1("adventure/sleep_in_bed");            // Sweet Dreams
        addTier1("adventure/heart_transplanter");      // Heart Transplanter
        
        // Tier 2 - Normal Adventure (20 advancements)
        addTier2("adventure/voluntary_exile");         // Voluntary Exile
        addTier2("adventure/spyglass_at_ghast");       // Is It a Balloon?
        addTier2("adventure/throw_trident");           // A Throwaway Joke
        addTier2("adventure/shoot_arrow");             // Take Aim
        addTier2("adventure/kill_mob_near_sculk_catalyst"); // It Spreads
        addTier1("adventure/brush_armadillo");         // Isn't It Scute?
        addTier1("adventure/lighten_up");              // Lighten Up
        addTier2("adventure/summon_iron_golem");       // Hired Help
        addTier3("adventure/whos_the_pillager_now");   // Who's the Pillager Now?
        addTier3("adventure/blowback");                // Blowback
        addTier3("adventure/revaulting");              // Revaulting
        addTier2("adventure/under_lock_and_key");      // Under Lock and Key
        addTier2("adventure/crafters_crafting_crafters"); // Crafters Crafting Crafters
        addTier1("adventure/walk_on_powder_snow_with_leather_boots"); // Light as a Rabbit
        addTier1("adventure/use_lodestone");           // Country Lode, Take Me Home
        addTier2("adventure/ol_betsy");                // Ol' Betsy
        addTier3("adventure/hero_of_the_village");     // Hero of the Village
        addTier3("adventure/trade_at_world_height");   // Star Trader
        addTier2("adventure/fall_from_world_height");  // Caves & Cliffs
        addTier2("adventure/who_needs_rockets");       // Who Needs Rockets?
        
        // Tier 3 - Hard Adventure (12 advancements)
        addTier3("adventure/very_very_frightening");   // Very Very Frightening
        addTier3("adventure/sniper_duel");             // Sniper Duel
        addTier3("adventure/two_birds_one_arrow");     // Two Birds, One Arrow
        addTier2("adventure/play_jukebox_in_meadows"); // Sound of Music
        addTier3("adventure/lightning_rod_with_villager_no_fire"); // Surge Protector
        addTier4("adventure/spyglass_at_dragon");      // Is It a Plane?
        addTier4("adventure/arbalistic");              // Arbalistic
        addTier2("adventure/craft_decorated_pot_using_only_sherds"); // Careful Restoration
        addTier2("adventure/trim_with_any_armor_pattern"); // Crafting a New Look
        addTier3("adventure/overoverkill");            // Over-Overkill
        addTier3("adventure/bullseye");                // Bullseye
        addTier4("adventure/totem_of_undying");        // Postmortal
        
        // Tier 4 - Extreme Adventure (4 advancements)
        addTier4("adventure/adventuring_time");        // Adventuring Time
        addTier4("adventure/trim_with_all_exclusive_armor_patterns"); // Smithing with Style
        addTier4("adventure/kill_all_mobs");           // Monsters Hunted
        addTier4("adventure/minecraft_trials_edition"); // Minecraft: Trial(s) Edition
        
        
        // ============================================================
            // HUSBANDRY - 30 advancements
        // ============================================================
        
        // Tier 1 - Easy Husbandry (12 advancements)
        addTier1("husbandry/root");                    // Husbandry
        addTier1("husbandry/place_dried_ghast_in_water"); // Stay Hydrated!
        addTier1("husbandry/safely_harvest_honey");    // Bee Our Guest
        addTier1("husbandry/breed_an_animal");         // The Parrots and the Bats
        addTier1("husbandry/ride_a_boat_with_a_goat"); // Whatever Floats Your Goat!
        addTier1("husbandry/tame_an_animal");          // Best Friends Forever
        addTier1("husbandry/make_a_sign_glow");        // Glow and Behold!
        addTier1("husbandry/fishy_business");          // Fishy Business
        addTier1("husbandry/tactical_fishing");        // Tactical Fishing
        addTier1("husbandry/axolotl_in_a_bucket");     // The Cutest Predator
        addTier1("husbandry/plant_any_sniffer_seed");  // Planting the Past
        addTier1("husbandry/repair_wolf_armor");       // Good as New
        
        // Tier 2 - Normal Husbandry (12 advancements)
        addTier2("husbandry/plant_seed");              // A Seedy Place
        addTier2("husbandry/wax_on");                  // Wax On
        addTier2("husbandry/wax_off");                 // Wax Off
        addTier2("husbandry/silk_touch_nest");         // Total Beelocation
        addTier2("husbandry/tadpole_in_a_bucket");     // Bukkit Bukkit
        addTier2("husbandry/obtain_sniffer_egg");      // Smells Interesting
        addTier2("husbandry/feed_snifflet");           // Little Sniffs
        addTier2("husbandry/allay_deliver_item_to_player"); // You've Got a Friend in Me
        addTier2("husbandry/remove_wolf_armor");       // Shear Brilliance
        addTier2("husbandry/kill_axolotl_target");     // The Healing Power of Friendship!
        addTier2("husbandry/allay_deliver_cake_to_note_block"); // Birthday Song
        addTier2("husbandry/leash_all_frog_variants"); // When the Squad Hops into Town
        
        // Tier 3 - Hard Husbandry (4 advancements)
        addTier3("husbandry/bred_all_animals");        // Two by Two
        addTier3("husbandry/complete_catalogue");      // A Complete Catalogue
        addTier3("husbandry/whole_pack");              // The Whole Pack
        addTier3("husbandry/froglights");              // With Our Powers Combined!
        
        // Tier 4 - Extreme Husbandry (2 advancements)
        addTier4("husbandry/balanced_diet");           // A Balanced Diet
        addTier4("husbandry/obtain_netherite_hoe");    // Serious Dedication
        
        // Special override for "How Did We Get Here?" to be worth 10 points
        ADVANCEMENT_POINTS.put("minecraft:nether/all_effects", 10);
        
        System.out.println("[CHECK] Total tracked advancements: " + ADVANCEMENT_POINTS.size());
        System.out.println("[AdvancementPointSystem] Total max points: " + getMaxPoints());
    }
    
    // ============================================================
    // TIER ASSIGNMENT METHODS
    // ============================================================
    
    /**
     * Tier 1: Easy - Basic gameplay, early-game tasks (1 point)
     */
    private static void addTier1(String advancementId) {
        ADVANCEMENT_POINTS.put("minecraft:" + advancementId, 1);
    }
    
    /**
     * Tier 2: Normal - Mid-game progression, exploration (2 points)
     */
    private static void addTier2(String advancementId) {
        ADVANCEMENT_POINTS.put("minecraft:" + advancementId, 2);
    }
    
    /**
     * Tier 3: Hard - Challenging tasks, boss fights, rare items (3 points)
     */
    private static void addTier3(String advancementId) {
        ADVANCEMENT_POINTS.put("minecraft:" + advancementId, 3);
    }
    
    /**
     * Tier 4: Extreme - Completionist goals, dangerous tasks, full collections (5 points)
     */
    private static void addTier4(String advancementId) {
        ADVANCEMENT_POINTS.put("minecraft:" + advancementId, 5);
    }
    
    // ============================================================
    // PUBLIC API
    // ============================================================
    
    /**
     * Get the point value for an advancement.
     * Returns 0 if advancement is not tracked.
     */
    public static int getPoints(AdvancementHolder advancement) {
        return getPoints(advancement.id());
    }
    
    public static int getPoints(ResourceLocation advancementId) {
        return ADVANCEMENT_POINTS.getOrDefault(advancementId.toString(), 0);
    }
    
    /**
     * Check if an advancement is tracked in the point system.
     */
    public static boolean isTracked(AdvancementHolder advancement) {
        return ADVANCEMENT_POINTS.containsKey(advancement.id().toString());
    }
    
    /**
     * Get the total number of tracked advancements.
     */
    public static int getTotalAdvancements() {
        return ADVANCEMENT_POINTS.size();
    }
    
    /**
     * Get the maximum possible points (sum of all advancement points).
     */
    public static int getMaxPoints() {
        return ADVANCEMENT_POINTS.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Get all tracked advancement IDs and their point values.
     */
    public static Map<String, Integer> getAllAdvancements() {
        return new HashMap<>(ADVANCEMENT_POINTS);
    }
}
