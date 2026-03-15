# Shinobi Alliance SMP Mod

A full-featured competitive progression mod built for Fabric servers, designed to power structured SMP gameplay with advancement-based ranking, village factions, war events, and real-time player tracking.

Supports **Minecraft 1.21.11 (Fabric)**.

Current mod version: **1.0.1**.

---

## 📥 Download

Modrinth: https://modrinth.com/mod/shinobi-alliance  

GitHub Releases: (coming soon)

---

## 📝 Update Notes (1.0.1)

- Updated Minecraft compatibility from **1.21.10** to **1.21.11**.
- Updated Fabric and integration dependencies for 1.21.11 compatibility.
- Applied minimal mapping/API compatibility fixes required for compilation.
- No gameplay, rank, village, command, or progression behavior changes.

---

## ✨ Features

### ⚔️ Advancement-Based Progression
- Tracks **124+ vanilla advancements**
- Dynamic point system tied to player achievements
- Real-time stat tracking and progression
- Designed for long-term SMP engagement

### 🏯 Village & Faction System
- 5 unique villages
- Player alignment and faction identity
- Structured progression and rewards
- Expandable framework for additional factions

### 🔥 Server War Events
- Competitive, server-wide war mechanics
- PvP objectives and event-driven gameplay
- Built for large multiplayer communities

### 🧬 Player Rank System
- Rank tiers based on advancement progress
- Custom rewards and unlockables
- Balanced for survival and competitive servers

### 🛠️ Custom Commands & Items
- Administrative and gameplay command system
- Unique server tools
- Extensible architecture for future updates

---

## 🖥️ Server Focused Design

This mod is optimized for:
- Competitive SMP servers  
- Long-term progression communities  
- Custom Fabric modpacks  
- Structured multiplayer gameplay  

---

## 📦 Installation

### Server Installation
1. Install Fabric Server for Minecraft 1.21.11.
2. Download the latest release.
3. Place the `.jar` file in the server `mods` folder.
4. Start the server.

### Client Installation
1. Install Fabric Loader.
2. Place the mod in your client `mods` folder.
3. Launch the game.

---

## 🧪 Development Setup

Clone and build the project:

git clone https://github.com/NataleeRene/shinobi-alliance-smp-mod
cd shinobi-alliance-smp-mod
./gradlew build

---

## ⚠️ Local Development Dependencies

This project uses a local dependency during development:

- `ForgeConfigAPIPort-v21.11.1-mc1.21.11-Fabric.jar`

For development, place this file inside a `/libs` directory.

The `/libs` folder is intentionally excluded from version control to keep the repository clean and professional.

---

## 🔗 Compatibility

This release targets **Minecraft 1.21.11** and is built for Fabric.

- **Fabric Loader**: 0.17.3 (or compatible newer version)
- **Fabric API**: 0.141.3+1.21.11
- **Open Parties and Claims (OPAC)**: fabric-1.21.11-0.25.10
- **ForgeConfigAPIPort**: v21.11.1 for mc1.21.11 (local `/libs` jar in this project setup)
- **LuckPerms**: API 5.4 integration support (optional integration)

---

## 📚 Documentation

See:
- `DOCUMENTATION.md`
- `SERVER_SETUP.md`
- `CHANGES.md`

---

## 🔮 Future Roadmap

- GUI and visual progression tracking
- Data-driven configuration
- Public API for plugin integration
- Expanded faction mechanics
- Additional war systems

---

## 🤝 Contributions

Pull requests, feedback, and suggestions are welcome.

---

## 📄 License

MIT License.