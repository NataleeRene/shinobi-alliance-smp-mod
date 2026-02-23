# Shinobi Alliance Mod - Complete Documentation

**Version:** 1.0.0  
**Minecraft Version:** 1.21.10  
**Mod Loader:** Fabric  
**Last Updated:** November 24, 2025

---

## Table of Contents

1. [Overview](#overview)
2. [Features](#features)
3. [Installation](#installation)
4. [Village System](#village-system)
5. [Rank System](#rank-system)
6. [War System](#war-system)
7. [Commands](#commands)
8. [Permissions & Integration](#permissions--integration)
9. [Technical Details](#technical-details)
10. [Troubleshooting](#troubleshooting)

---

## Overview

The **Shinobi Alliance Mod** is a server-side Fabric mod designed for Naruto-themed Minecraft SMPs. It implements a comprehensive village-based faction system with automatic rank progression, achievement-based advancement, and a player-vs-player war system with OPAC (Open Parties and Claims) integration.

### Key Highlights
- **5 Villages**: Leaf, Sand, Mist, Stone, Cloud
- **5 Ranks**: Genin, Chunin, Jonin, Anbu, Kage
- **122 Vanilla Advancements**: Tracked for automatic rank progression
- **Multi-War System**: Kages can declare war on multiple opponents simultaneously
- **LuckPerms Integration**: Automatic group management
- **OPAC Integration**: Temporary claim bypass during wars

---

## Features

### Village Selection System
- **One-time selection**: Players choose their village on first join
- **Immobilization**: Players are frozen until village selection is complete
- **Village Scroll**: Special NBT-tagged paper item with custom texture
  - **Cannot be dropped** until village is selected
  - Opens GUI on right-click with 5 village options
- **Persistent**: Village choice is permanent per player

### Achievement Point System
- **122 Vanilla Advancements** tracked and assigned point values:
  - **Basic advancements**: 1 point (e.g., "Minecraft", "Stone Age")
  - **Medium advancements**: 2 points (e.g., "Eye Spy", "Return to Sender")
  - **Hidden/Challenge advancements**: 3 points (e.g., "How Did We Get Here?", "Adventuring Time")
- **Maximum possible points**: ~250 points
- **Manual scanning**: Use `/shinobi checkrank` to update points and rank

### Rank Progression
| Rank | Point Range | LuckPerms Group | Permissions |
|------|-------------|-----------------|-------------|
| **Genin** | 0 - 50 | `genin` | Basic village member |
| **Chunin** | 51 - 120 | `chunin` | Intermediate rank |
| **Jonin** | 121 - 180 | `jonin` | Advanced rank |
| **Anbu** | 181 - 219 | `anbu` | Elite rank |
| **Kage** | 220+ | `kage` | Village leader, war powers, OPAC claim management |

### War System
- **Player-based wars**: Kage vs Kage (not village vs village)
- **Multiple simultaneous wars**: No limit on active wars per Kage
- **OPAC bypass**: During war, each Kage can bypass the other's **personal claims only**
  - **Never bypasses**: Server claims, admin claims, spawn regions, protected zones
- **Persistent**: Wars save to disk and reload on server restart
- **Broadcast announcements**: Server-wide notifications for war declarations and peace treaties

---

## Installation

### Prerequisites
1. **Minecraft 1.21.10**
2. **Fabric Loader 0.17.3+**
3. **Fabric API 0.136.0+1.21.10**
4. **LuckPerms** (optional, for group management)
5. **Open Parties and Claims (OPAC)** (optional, for war claim bypass)

### Installation Steps
1. Download the mod JAR file
2. Place in your server's `mods/` folder
3. Start the server
4. Configure LuckPerms groups (if using):
   ```
   /lp creategroup leaf
   /lp creategroup sand
   /lp creategroup mist
   /lp creategroup stone
   /lp creategroup cloud
   /lp creategroup genin
   /lp creategroup chunin
   /lp creategroup jonin
   /lp creategroup anbu
   /lp creategroup kage
   ```

### Server Files Generated
- `shinobi_wars.json` - Active war data (auto-created in server root)

---

## Village System

### The 5 Villages

#### 1. **Leaf Village** (Hidden Leaf)
- **ID**: `leaf`
- **Rank Color**: §c (Red)
- **Bracket Color**: §f (White)
- **Prefix Format**: `§c[Rank] §f`
- **Example**: `§c[Genin] §fPlayerName`

#### 2. **Sand Village** (Hidden Sand)
- **ID**: `sand`
- **Rank Color**: §e (Yellow)
- **Bracket Color**: §7 (Gray)
- **Prefix Format**: `§7[§eRank§7]`
- **Example**: `§7[§eGenin§7] PlayerName`

#### 3. **Mist Village** (Hidden Mist)
- **ID**: `mist`
- **Rank Color**: §9 (Dark Blue)
- **Bracket Color**: §7 (Gray)
- **Prefix Format**: `§7[§9Rank§7]`
- **Example**: `§7[§9Genin§7] PlayerName`

#### 4. **Stone Village** (Hidden Stone)
- **ID**: `stone`
- **Rank Color**: §8 (Dark Gray)
- **Bracket Color**: §7 (Gray)
- **Prefix Format**: `§7[§8Rank§7]`
- **Example**: `§7[§8Genin§7] PlayerName`

#### 5. **Cloud Village** (Hidden Cloud)
- **ID**: `cloud`
- **Rank Color**: §f (White)
- **Bracket Color**: §f (White)
- **Prefix Format**: `§f[Rank] §f`
- **Example**: `§f[Genin] §fPlayerName`

### Village Selection Process
1. Player joins server for the first time
2. Player is **frozen in place** (cannot move)
3. Village scroll (paper with NBT tag) appears in inventory
4. Player **cannot drop the scroll** (protected)
5. Right-click scroll to open village GUI
6. Click on desired village button
7. Player is assigned to village, receives Genin rank
8. Player is **unfrozen** and can play

---

## Rank System

### Rank Overview
Ranks are automatically updated based on achievement points. Use `/shinobi checkrank` after completing advancements to trigger rank updates.

### Rank Details

#### **Genin** (0-50 points)
- **Starting rank** for all players
- **LuckPerms Group**: `genin`
- **Scoreboard Team**: `{village}_genin` (e.g., `leaf_genin`)
- **Promotion Message**: Village-only announcement

#### **Chunin** (51-120 points)
- **Requirements**: Complete ~51 advancements (varies by difficulty)
- **LuckPerms Group**: `chunin`
- **Scoreboard Team**: `{village}_chunin`
- **Promotion Message**: Village-only announcement

#### **Jonin** (121-180 points)
- **Requirements**: Complete ~121+ advancements
- **LuckPerms Group**: `jonin`
- **Scoreboard Team**: `{village}_jonin`
- **Promotion Message**: Village-only announcement

#### **Anbu** (181-219 points)
- **Requirements**: Complete ~181+ advancements
- **LuckPerms Group**: `anbu`
- **Scoreboard Team**: `{village}_anbu`
- **Promotion Message**: Village-only announcement

#### **Kage** (220+ points)
- **Requirements**: Complete nearly all vanilla advancements
- **LuckPerms Group**: `kage`
- **Scoreboard Team**: `{village}_kage`
- **Promotion Message**: **Server-wide** broadcast: `⚔ A new Kage has risen!`
- **Special Permissions**:
  - Can declare war on other Kages (`/declarewar`)
  - Can end wars (`/endwar`)
  - Receives OPAC claim management permissions:
    - `openpartiesandclaims.manage`
    - `openpartiesandclaims.claim.{village}`

### Advancement Point Values

**Story Advancements** (1pt each):
- Minecraft, Stone Age, Getting an Upgrade, Acquire Hardware, Smelt!
- Hot Stuff, Isn't It Iron Pick, Not Today Thank You, Ice Bucket Challenge
- We Need to Go Deeper, Cover Me With Diamonds, Enchanter, Zombie Doctor
- Eye Spy, The End

**Nether Advancements** (mixed):
- Suit Up (1pt), Very Very Frightening (2pt), Spooky Scary Skeleton (2pt)
- Into Fire (1pt), Not Quite "Nine" Lives (2pt), Hot Tourist Destinations (2pt)
- Withering Heights (3pt), Local Brewery (1pt), Bring Home the Beacon (2pt)
- A Furious Cocktail (2pt), How Did We Get Here? (3pt), Uneasy Alliance (3pt)

**End Advancements** (medium/hidden):
- Free the End (2pt), The Next Generation (2pt), Remote Getaway (2pt)
- You Need a Mint (2pt), The City at the End of the Game (3pt)
- Sky's the Limit (3pt), Great View From Up Here (3pt)

**Adventure Advancements** (varied):
- Adventure (1pt), Kill a Mob (1pt), Sweet Dreams (1pt), What a Deal (1pt)
- Postmortal (2pt), Hired Help (1pt), Two by Two (1pt), Who's the Pillager Now? (2pt)
- Arbalistic (3pt), Adventuring Time (3pt), Very Very Frightening (2pt)
- Sniper Duel (2pt), Monsters Hunted (3pt), Ol' Betsy (2pt), Surge Protector (2pt)

**Husbandry Advancements** (basic/medium):
- Husbandry (1pt), Best Friends Forever (1pt), The Parrots and the Bats (1pt)
- You've Got a Friend in Me (1pt), Whatever Floats Your Goat (1pt)
- Wax On (1pt), Wax Off (1pt), Two by Two (1pt), Little Sniffs (2pt)
- A Seedy Place (1pt), Serious Dedication (2pt), A Complete Catalogue (2pt)
- Tactical Fishing (1pt), Fishy Business (1pt), Total Beelocation (2pt)
- A Balanced Diet (3pt), The Cutest Predator (2pt), With Our Powers Combined (2pt)

---

## War System

### War Declaration

#### Command
```
/declarewar <playerName>
```

#### Requirements
- **Attacker must be a Kage**
- **Defender must be a Kage**
- **Defender must be online**
- Cannot declare war on yourself
- Cannot declare duplicate war (war already exists)

#### Effects
1. **Server Broadcast**: `⚔ <RankColor><Rank> <Name> has declared war on <RankColor><Rank> <Name>!`
2. **OPAC Bypass Applied**:
   - Attacker can bypass defender's personal claims
   - Defender can bypass attacker's personal claims
   - **NO bypass on server/admin/spawn claims**
3. **War Saved to Disk**: Persists across server restarts

#### Example
```
/declarewar Steve
```
Output: `⚔ §c[Kage] §fAlexHas declared war on §7[§eKage§7] Steve!`

### Ending Wars

#### Command
```
/endwar <playerName>
```

#### Requirements
- You must be at war with the specified player
- War exists in either direction (attacker→defender or defender→attacker)

#### Effects
1. **Peace Broadcast**: `🕊 Peace restored between <Name> and <Name>.`
2. **OPAC Bypass Removed**: Claim protections restored
3. **War Removed from Disk**

#### Example
```
/endwar Steve
```
Output: `🕊 Peace restored between §6Alex§r and §6Steve§r.`

### War Status

#### Command
```
/warstatus
```

#### Output
Lists all active wars:
```
§6⚔ Active Wars:
§c  • §6Kage Alex §cvs §6Kage Steve
§c  • §6Kage Bob §cvs §6Kage Charlie
```

If no wars:
```
🕊 There are no active wars.
```

### War Lookup

#### Command
```
/warwho <playerName>
```

#### Output
Shows all wars involving a specific player:
```
⚔ §6Steve§r is at war with: §cAlex, Bob, Charlie
```

If not in any wars:
```
🕊 §6Steve§r is not involved in any wars.
```

### War System Rules

#### Multiple Wars
- Kages can be in **unlimited wars** simultaneously
- Each war is tracked separately
- Each war has independent OPAC bypass permissions

#### OPAC Safety
**NEVER bypasses:**
- Server claims (admin-created)
- Admin claims (protected regions)
- Spawn safety regions
- Protected zones
- OPAC admin-level flags

**ONLY bypasses:**
- Personal claims of the two Kages at war
- Village-level claims associated with the warring Kages

#### Persistence
- All wars saved to `shinobi_wars.json`
- Automatically reloads on server restart
- OPAC permissions reapplied on player login if war is active

---

## Commands

### Player Commands

#### `/shinobi points`
**Description**: Show your current achievement points and rank  
**Permission**: None (all players)  
**Example Output**:
```
§6Your Stats:
§ePoints: §f85
§eRank: §6Chunin
§730 points until §6Jonin
```

#### `/shinobi checkrank`
**Description**: Scan completed advancements, award points, and update rank  
**Permission**: None (all players)  
**Example Output**:
```
§6=== Shinobi Rank Status ===
§eAdvancements Completed: §f56/122
§eTotal Points: §f85/~250
§eCurrent Rank: §6Chunin
§7Next: §6Jonin §7(§f36 points§7)
```

If rank changed:
```
§a✦ Rank updated from Genin to Chunin!
```

### War Commands

#### `/declarewar <playerName>`
**Description**: Declare war on another Kage  
**Permission**: Must be a Kage  
**Example**: `/declarewar Steve`

#### `/endwar <playerName>`
**Description**: End war with another Kage  
**Permission**: Must be at war with the target  
**Example**: `/endwar Steve`

#### `/warstatus`
**Description**: List all active wars  
**Permission**: None (all players)

#### `/warwho <playerName>`
**Description**: Show wars involving a specific player  
**Permission**: None (all players)  
**Example**: `/warwho Steve`

### Admin Commands

#### `/shinobi setkage <player> <village>`
**Description**: Manually set a player as Kage of a village  
**Permission**: OP level 2+  
**Villages**: `leaf`, `sand`, `mist`, `stone`, `cloud`  
**Example**: `/shinobi setkage Steve leaf`  
**Effects**:
- Assigns player to village
- Sets rank to Kage
- Updates scoreboard team to `{village}_kage`
- Broadcasts server-wide message
- Grants OPAC claim management permissions

---

## Permissions & Integration

### LuckPerms Integration

#### Village Groups
```
leaf, sand, mist, stone, cloud
```
- Assigned on village selection
- Mutually exclusive (player can only be in one village)

#### Rank Groups
```
genin, chunin, jonin, anbu, kage
```
- Assigned based on achievement points
- Automatically updated on rank progression
- Old rank group removed when promoted

#### Kage Permissions
When promoted to Kage, the following permissions are automatically granted:
```
openpartiesandclaims.manage
openpartiesandclaims.claim.{village}
```

### OPAC Integration

#### War Bypass Commands
The mod executes OPAC commands during wars:

**On War Declaration**:
```
/opac admin bypassPlayer <attacker> <defender> true
/opac admin bypassPlayer <defender> <attacker> true
```

**On War End**:
```
/opac admin bypassPlayer <attacker> <defender> false
/opac admin bypassPlayer <defender> <attacker> false
```

#### Claim Safety
The mod **only affects personal claim bypass**. It does NOT:
- Bypass server claims
- Bypass admin claims
- Bypass spawn protection
- Grant OPAC admin permissions
- Modify claim ownership
- Delete or create claims

---

## Technical Details

### Scoreboard System

#### Objectives
- `shinobi_points`: Stores player's achievement points
- `shinobi_rank`: Stores player's rank as ordinal (0=Genin, 1=Chunin, etc.)

#### Teams
25 total teams (5 villages × 5 ranks):
```
leaf_genin, leaf_chunin, leaf_jonin, leaf_anbu, leaf_kage
sand_genin, sand_chunin, sand_jonin, sand_anbu, sand_kage
mist_genin, mist_chunin, mist_jonin, mist_anbu, mist_kage
stone_genin, stone_chunin, stone_jonin, stone_anbu, stone_kage
cloud_genin, cloud_chunin, cloud_jonin, cloud_anbu, cloud_kage
```

Each team has a prefix showing rank and village color above player's head.

### Data Storage

#### War Data (`shinobi_wars.json`)
```json
{
  "attacker-uuid": ["defender-uuid-1", "defender-uuid-2"],
  "another-attacker-uuid": ["defender-uuid-3"]
}
```

**Format**: Map of attacker UUID to set of defender UUIDs  
**Location**: Server root directory  
**Auto-save**: On every war declaration/end  
**Auto-load**: On server start

### Village Scroll NBT
```json
{
  "CustomModelData": 7777,
  "ShinobiScroll": 1b
}
```

**Item**: `minecraft:paper`  
**Detection**: Checks for `ShinobiScroll` NBT tag  
**Texture**: Custom model override via resource pack  
**Protection**: Cannot be dropped until village is selected

### Advancement Tracking
- **Manual system**: No automatic point award on advancement completion
- **Player-triggered**: Use `/shinobi checkrank` to scan and update
- **Scan process**: Iterates all 1,574 server advancements, checks completion status
- **Point calculation**: Sums points from 122 tracked vanilla advancements
- **Rank update**: Automatically promotes if point threshold reached

---

## Troubleshooting

### Common Issues

#### **"Village scroll disappeared"**
- **Before Fix**: Players could accidentally drop the scroll
- **Current**: Scroll cannot be dropped until village is selected
- **Solution**: Automatic - scroll is protected

#### **"Rank not updating"**
- **Cause**: Advancements must be manually scanned
- **Solution**: Use `/shinobi checkrank` after completing advancements
- **Note**: Rank updates are not automatic on advancement completion

#### **"War declaration fails"**
- **Check**: Both players must be Kages
- **Check**: Defender must be online
- **Check**: War doesn't already exist
- **Solution**: Verify ranks with `/shinobi points`

#### **"OPAC bypass not working"**
- **Check**: OPAC mod installed and running
- **Check**: War is active (use `/warstatus`)
- **Note**: Only bypasses personal claims, not server/admin claims
- **Check**: Verify with OPAC's claim info commands

#### **"LuckPerms commands showing errors"**
- **Expected**: Commands show "Unknown command" in dev environment
- **Solution**: Install LuckPerms on production server
- **Note**: Mod works without LuckPerms, just no group management

#### **"Player stuck frozen"**
- **Cause**: Village not selected or selection failed
- **Solution**: Admin can manually assign village:
  ```
  /shinobi setkage <player> <village>
  ```
- **Prevention**: Ensure player right-clicks scroll and selects village

#### **"Custom texture not showing"**
- **Cause**: Resource pack not loaded
- **Solution**: Ensure `shinobi-alliance-pack.zip` in `run/resourcepacks/`
- **Note**: Server can prompt clients to download resource pack
- **Format**: Pack format 69 for MC 1.21.10

### Debug Information

#### Log Prefixes
All mod logs start with `[ShinobiAllianceMod]`:
```
[ShinobiAllianceMod] War declared: Alex vs Steve
[ShinobiAllianceMod] Initialized team: leaf_genin with prefix...
[ShinobiAllianceMod] War system initialized
```

#### Initialization Messages
On server start, you should see:
```
[ShinobiAllianceMod] Server-only mod initializing for Minecraft 1.21.10...
[ShinobiAllianceMod] Initialized team: leaf_genin with prefix §c[Genin] §f
... (25 team initialization messages)
[ShinobiAllianceMod] No war data file found, starting fresh
[ShinobiAllianceMod] War system initialized
[ShinobiAllianceMod] Achievement point system initialized
```

#### Player Join Messages
When a new player joins:
```
[ShinobiAllianceMod] New player detected: PlayerName
[ShinobiAllianceMod] Froze player PlayerName at position (x, y, z)
[ShinobiAllianceMod] Village scroll created - texture via built-in resource pack
[ShinobiAllianceMod] Created village scroll with texture override: 7777
[ShinobiAllianceMod] Scroll NBT: {CustomModelData:7777,ShinobiScroll:1b}
```

#### Village Selection Messages
When a player selects a village:
```
[ShinobiAllianceMod] Player PlayerName selected village: Leaf
[ShinobiAllianceMod] Assigned PlayerName to village: Leaf
[ShinobiAllianceMod] Set PlayerName points to 0
[ShinobiAllianceMod] Set PlayerName rank to Genin
[ShinobiAllianceMod] Assigned PlayerName to team leaf_genin
[ShinobiAllianceMod] Initialized PlayerName with Genin rank
[ShinobiAllianceMod] Unfroze player PlayerName
```

---

## Developer Notes

### File Structure
```
src/main/java/com/shinobi/shinobialliancemod/
├── ShinobiAllianceMod.java          # Main mod class, event handlers
├── ShinobiCommands.java             # All command implementations
├── ShinobiItems.java                # Village scroll NBT system
├── Village.java                     # Village enum and rank prefixes
├── Rank.java                        # Rank enum with point thresholds
├── AdvancementPointSystem.java      # 122 advancement → point mappings
├── PlayerPointsManager.java         # Scoreboard objective management
├── RankManager.java                 # Automatic promotion logic
├── ScoreboardTeamManager.java       # Team creation and assignment
├── WarManager.java                  # Multi-war system with persistence
├── LuckPermsService.java            # LP command execution
└── PlayerFreezeManager.java         # Player immobilization system
```

### Key Classes

**ShinobiAllianceMod**
- Main entry point
- Registers event handlers
- Prevents village scroll drops

**WarManager**
- UUID-based war tracking
- JSON persistence
- OPAC command execution
- Multi-war support

**AdvancementPointSystem**
- Static HashMap of advancement → points
- 122 vanilla advancements categorized
- Returns 0 for non-tracked advancements

**PlayerPointsManager**
- Scoreboard objective interface
- Stores points and rank per player
- Server-side only (no client sync needed)

**RankManager**
- Checks point thresholds
- Promotes players automatically
- Broadcasts promotions
- Integrates with LuckPerms and OPAC

---

## Credits

**Mod Author**: Created for Naruto-themed SMP servers  
**Minecraft Version**: 1.21.10  
**Mod Loader**: Fabric  
**Dependencies**: Fabric API  
**Optional Integrations**: LuckPerms, Open Parties and Claims (OPAC)

---

## Changelog

### Version 1.0.0 (November 24, 2025)
- Initial release
- Village selection system with custom NBT-tagged scrolls
- Achievement-based rank progression (5 ranks, 122 advancements)
- Multi-war system with OPAC integration
- LuckPerms group management
- Scoreboard team prefixes with village colors
- Player freeze during village selection
- **Village scroll drop protection** (cannot drop before selection)
- War data persistence (JSON)
- Comprehensive command system

---

## Support

For issues, questions, or feature requests:
1. Check this documentation thoroughly
2. Review the [Troubleshooting](#troubleshooting) section
3. Verify LuckPerms and OPAC are properly configured
4. Check server logs for `[ShinobiAllianceMod]` messages

---

## License

All rights reserved. This mod is for private server use.

---

**End of Documentation**
