# Server Resource Pack Setup for Shinobi Alliance Mod

## Overview
This setup allows your server to automatically prompt clients to download the custom village scroll texture without requiring them to manually install any mods.

## Setup Instructions

### 1. Upload Resource Pack
1. Zip the `server-resourcepack` folder contents (not the folder itself)
2. Upload the zip to a public hosting service like:
   - GitHub Releases
   - Google Drive (set to public)
   - Dropbox (public link)
   - Your own web server

### 2. Configure Your MineCraft Server
Add these lines to your `server.properties` file:

```properties
# Resource Pack Configuration
resource-pack=https://your-host.com/path/to/resourcepack.zip
resource-pack-prompt=Please accept to see custom village scroll textures
resource-pack-sha1=
require-resource-pack=false
```

**Note**: Set `require-resource-pack=true` if you want to force the resource pack.

### 3. Generate SHA1 Hash (Optional but Recommended)
To generate the SHA1 hash of your resource pack zip:

**Windows PowerShell:**
```powershell
Get-FileHash -Path "resourcepack.zip" -Algorithm SHA1
```

**Linux/Mac:**
```bash
sha1sum resourcepack.zip
```

Add the hash to your `server.properties`:
```properties
resource-pack-sha1=your-sha1-hash-here
```

## How It Works

1. **Client joins server** → Server prompts resource pack download
2. **Client clicks "Yes"** → Resource pack downloads automatically  
3. **Paper with CustomModelData:7777** → Shows custom village scroll texture
4. **All other paper items** → Show normal paper texture

## Files Included

- `pack.mcmeta` - Resource pack metadata (format 34 for MC 1.21.10)
- `assets/minecraft/models/item/paper.json` - Paper item override for CustomModelData:7777
- `assets/shinobialliancemod/models/item/village_scroll.json` - Custom model definition
- `assets/shinobialliancemod/textures/item/` - Texture folder (add village_scroll.png here)

## Adding Your Custom Texture

Replace the placeholder with your actual texture:
1. Create a 16x16 PNG file named `village_scroll.png`
2. Place it in `server-resourcepack/assets/shinobialliancemod/textures/item/`
3. Re-zip and re-upload the resource pack

## Testing

1. Put the mod JAR in your server's `mods` folder
2. Configure the resource pack as above
3. Join your server
4. Accept the resource pack prompt
5. Use `/give @s paper[custom_data={ShinobiScroll:1b,CustomModelData:7777}]` to test
6. The paper should show your custom texture!

## Troubleshooting

- **Resource pack not prompting**: Check the URL is publicly accessible
- **Texture not showing**: Ensure `village_scroll.png` exists in the correct path
- **Client rejects pack**: Set `require-resource-pack=true` if necessary
- **SHA1 mismatch**: Regenerate the hash after any changes to the pack