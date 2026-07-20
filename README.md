# Settings Profiles

A [RuneLite](https://runelite.net) plugin that lets you save a snapshot of your OSRS game settings as a named profile and restore it with one click. Supports separate profiles per account so your alts can have their own keybinds, audio levels, and UI preferences.

---

## Features

- **Save / Load / Delete** named profiles directly from the plugin config panel — no extra side buttons or overlays.
- **Account-specific or global** — profiles can be stored under the logged-in player's name or shared across all characters.
- **List profiles in chat** — print all available profiles to the game chat at any time.
- **55+ tracked settings** covering every major preference category.

### Settings captured per profile

| Category | What's saved |
|---|---|
| **Audio** | Master volume, music volume, SFX volume, area sounds volume, jingles toggle |
| **Display** | Brightness, draw distance, camera zoom (fixed & resizable viewport), field of view clamp |
| **Keybinds** | All 13 sidebar tab hotkeys (Combat, Stats, Quests, Inventory, Equipment, Prayer, Magic, Clan, Friends, Options ×2, Music, Logout), Escape-to-close interface, stone-selection mode |
| **XP Drops** | Enabled, position, size, speed, duration, colour, skill grouping, counter type, progress type |
| **Chat** | Timestamps, transparent chatbox, scrollbar side, all chat colours (opaque, transparent, and split-private variants for every channel), mini-menu scrollbar |
| **Interface** | Chatbox / side-panel transparency, transparent new-menu, hide rooftops, resizable sidebar layout |
| **Gameplay** | Auto-run, Accept Aid, player & NPC attack priority, mouse button mode, profanity filter |
| **Ground Items** | Enabled, price display type, modifier key |

---

## Usage

1. Enable **Settings Profiles** in the RuneLite plugin list.
2. Open its config panel (⚙ gear icon).
3. Type a name in **Profile Name** (e.g. `main`, `skiller`, `pvp`).
4. *(Optional)* Enable **Account-Specific** to store the profile under the current player name.
5. Tick **Save Current Settings** — the checkbox resets automatically once saved.
6. Later, type the same name and tick **Load Profile** to restore it.
7. Use **List Profiles in Chat** to see all saved profiles.

> **Note:** Some settings (audio levels, brightness) may need you to briefly open the in-game Options tab once after loading for the engine to repaint the sliders correctly.

---

## Profile storage

Profiles are plain JSON files stored inside your RuneLite directory:

```
%USERPROFILE%\.runelite\settings-profiles\
├── global\
│   ├── skiller.json
│   └── pvp.json
└── PlayerName\
    ├── main.json
    └── bank-standing.json
```

Global profiles are visible to all characters. Account-specific profiles are only listed when that character is logged in.

---

## Building / Running

```bash
# Developer client (loads the plugin at startup)
./gradlew run

# Sideloadable fat JAR
./gradlew shadowJar
# → build/libs/settings-profiles-<version>-all.jar
```

Java 11 required. Targets the latest stable RuneLite release.

---

## License

BSD 2-Clause — see individual source files for the full header.
