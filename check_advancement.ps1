$authList = @(
"story/mine_stone","story/upgrade_tools","story/smelt_iron","story/obtain_armor",
"story/lava_bucket","story/iron_tools","story/deflect_arrow","story/form_obsidian",
"story/mine_diamond","story/enter_the_nether","story/shiny_gear","story/enchant_item",
"story/cure_zombie_villager","story/follow_ender_eye","story/enter_the_end",
"nether/obtain_blaze_rod","nether/find_bastion","nether/obtain_ancient_debris",
"nether/fast_travel","nether/find_fortress","nether/return_to_sender",
"nether/uneasy_alliance","nether/brew_potion","nether/create_beacon",
"nether/all_potions","nether/create_full_beacon","nether/summon_wither",
"nether/all_effects","nether/explore_nether","nether/get_wither_skull",
"nether/obtain_crying_obsidian","nether/distract_piglin","nether/ride_strider",
"nether/loot_bastion","nether/ride_strider_in_overworld_lava","nether/netherite_armor",
"nether/charge_respawn_anchor","nether/obtain_ghast_tear",
"end/kill_dragon","end/dragon_egg","end/enter_end_gateway","end/respawn_dragon",
"end/dragon_breath","end/find_end_city","end/elytra","end/levitate",
"adventure/voluntary_exile","adventure/is_it_a_balloon","adventure/spyglass_at_ghast",
"adventure/hero_of_the_village","adventure/shoot_arrow","adventure/throw_trident",
"adventure/kill_a_mob","adventure/trade","adventure/honey_block_slide",
"adventure/postmortal","adventure/summon_iron_golem","adventure/two_birds_one_arrow",
"adventure/sniper_duel","adventure/adventure","adventure/kill_all_mobs",
"adventure/respect_remnants","adventure/adventuring_time","adventure/arbalistic",
"adventure/sneak_100","adventure/avoid_vibration","adventure/very_very_frightening",
"adventure/lighten_up","adventure/spyglass_at_parrot","adventure/spyglass_at_dragon",
"adventure/under_lock_and_key","adventure/lightning_rod_with_villager_no_fire",
"adventure/fall_from_world_height","adventure/smithing_with_style",
"adventure/trim_with_any_armor_pattern","adventure/smithing_with_all_exclusive_armor_patterns",
"adventure/it_spreads","adventure/play_jukebox_in_meadows","adventure/whos_the_pillager_now",
"adventure/trade_at_world_height","adventure/read_power_of_chiseled_bookshelf",
"adventure/use_lodestone","adventure/sleep_in_bed","adventure/craft_decorated_pot_using_only_sherds",
"adventure/crafters_crafting_crafters","adventure/trial_edition","adventure/revaulting",
"adventure/blowback","adventure/crafting_a_new_look","adventure/gold_minable",
"adventure/obtain_heavy_core","adventure/blaze_rod_kill","adventure/swim_lava",
"husbandry/safely_harvest_honey","husbandry/breed_an_animal","husbandry/tame_an_animal",
"husbandry/fishy_business","husbandry/silk_touch_nest","husbandry/plant_seed",
"husbandry/wax_on","husbandry/wax_off","husbandry/ride_a_boat_with_a_goat",
"husbandry/complete_catalogue","husbandry/tactical_fishing","husbandry/kill_axolotl_target",
"husbandry/axolotl_in_a_bucket","husbandry/make_a_sign_glow","husbandry/allay_deliver_item_to_player",
"husbandry/allay_deliver_cake_to_note_block","husbandry/leash_all_frog_variants",
"husbandry/froglights","husbandry/obtain_sniffer_egg","husbandry/plant_any_sniffer_seed",
"husbandry/tadpole_in_a_bucket","husbandry/feed_snifflet","husbandry/two_by_two",
"husbandry/whole_pack","husbandry/balanced_diet","husbandry/sniffer_sniffed_item",
"husbandry/total_beelocation","husbandry/wolf_armor","husbandry/remove_wolf_armor",
"husbandry/repair_wolf_armor","husbandry/place_dried_ghast_in_water","husbandry/grow_up_friend"
)

Write-Host "Authoritative list count: $($authList.Count)"

$javaFile = "c:\Users\LT\Desktop\ShinobiAllianceMod\src\main\java\com\shinobi\shinobialliancemod\AdvancementPointSystem.java"
$javaContent = Get-Content $javaFile
$javaIds = $javaContent | Select-String 'addTier[1-4]\("([^"]+)"' | ForEach-Object { $_.Matches.Groups[1].Value }

Write-Host "Java file count: $($javaIds.Count)"

$missing = $authList | Where-Object { $_ -notin $javaIds }
$extra = $javaIds | Where-Object { $_ -notin $authList }

if ($missing.Count -eq 0 -and $extra.Count -eq 0) {
    Write-Host "`n✓ PERFECT MATCH!"
} else {
    if ($missing) {
        Write-Host "`n=== MISSING FROM JAVA ==="
        $missing | ForEach-Object { Write-Host $_ }
    }
    if ($extra) {
        Write-Host "`n=== EXTRA IN JAVA (SHOULD BE REMOVED) ==="
        $extra | ForEach-Object { Write-Host $_ }
    }
}
