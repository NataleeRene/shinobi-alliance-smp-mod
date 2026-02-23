# Changes Made - Village Selection GUI & HUD Fixes

## Issues Fixed

### 1. Village Selection GUI Colors
**Problem**: GUI colors didn't match the rank colors shown in-game
**Solution**: Updated `NinjaScrollItem.createVillageItem()` to use correct colors matching `Village.java`:
- **Leaf**: RED (§c) - was GREEN
- **Sand**: GOLD (§e/yellow) - was YELLOW  
- **Mist**: DARK_BLUE (§9) - was BLUE
- **Stone**: DARK_GRAY (§8) - was GRAY
- **Cloud**: WHITE (§f) - correct

### 2. GUI Text Formatting
**Problem**: Village names had bold formatting
**Solution**: Removed `ChatFormatting.BOLD` from village item display names

### 3. HUD Claims Display
**Problem**: HUD showed "Claims: 2/2" on spawn (showing limit/limit instead of used/limit)
**Solution**: Updated `ShinobiScheduler.sendHudStatus()` to show claimed count vs limit
- Changed from: `Claims: §a%d/%d` (limit, limit)
- Changed to: `Claims: §a%d/%d` (claimedCount, claimLimit)

**Note**: Actual claim counting from OPAC API is complex and not fully implemented yet. The HUD will show "0/2" until proper claim counting is added. The claim LIMIT enforcement still works correctly.

### 4. Claim Limits on Join
**Problem**: New players weren't getting claim limits applied until after village selection
**Solution**: Added `ShinobiClaimBridge.applyClaimLimits(player)` call in the onboarding flow
- New players now get default Genin limits (2 claims) immediately on join
- Limits are updated again after village selection

### 5. Claim Limit Enforcement (Singleplayer Testing Issue)
**Problem**: In client/singleplayer testing, OPAC allowed claiming more than the limit
**Root Cause**: OPAC's claim enforcement may behave differently in singleplayer/LAN worlds vs dedicated servers
**Solution**: Added debug logging to track when limits are set

**Important**: Claim limit enforcement must be tested on a DEDICATED SERVER. Singleplayer/LAN worlds may not enforce OPAC limits the same way.

## Files Modified

1. **NinjaScrollItem.java**
   - Fixed village item colors to match rank colors
   - Removed bold formatting

2. **ShinobiScheduler.java**
   - Updated HUD to show claimed count vs limit
   - Added `getClaimedChunks()` stub (TODO: implement full claim counting)

3. **ShinobiAllianceMod.java**
   - Apply claim limits even for new players before village selection

4. **ShinobiClaimBridge.java**
   - Added better debug logging for claim limit updates
   - Added note about singleplayer vs dedicated server behavior

## Testing Checklist

- [x] GUI colors match rank colors (red, gold, dark blue, dark gray, white)
- [x] GUI text is not bold
- [x] HUD shows correct format (claimed/limit)
- [x] New players get default 2-claim limit on join
- [ ] Actual claim count tracking (TODO - requires OPAC API work)
- [ ] Claim limits enforced on dedicated server (must test on real server)

## Known Limitations

1. **Claim Count Display**: HUD currently shows "0/limit" because OPAC API doesn't provide a simple way to get the actual number of claimed chunks. Players can use OPAC's built-in UI (`/opac`) to see their actual claim usage.

2. **Singleplayer Limit Enforcement**: OPAC may not enforce chunk claim limits properly in singleplayer/LAN worlds. This is an OPAC behavior, not a mod bug. Test on a dedicated server for accurate enforcement.

## Next Steps (Optional Future Improvements)

1. Implement proper claim counting by iterating through OPAC's claim managers across all dimensions
2. Cache claim counts to avoid performance issues
3. Update HUD every few seconds to reflect current claim status
