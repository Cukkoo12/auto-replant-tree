# Auto Replant Tree

Automatically replants saplings when you break a log block. Works on the server — no client mod needed.

## Features

- **All vanilla tree types**: Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Mangrove, Cherry, Pale Oak (1.21.4+)
- **Nether trees**: Crimson and Warped stems → fungi replanting on their respective nylium
- **2x2 tree support**: Dark Oak and Jungle trees are replanted as a 2x2 grid (requires 4 saplings)
- **Sneak to cancel**: Hold sneak/crouch while breaking to skip auto-replant
- **No-sapling mode** (`requireSapling: false`): Auto-plant without consuming saplings — only ground check required
- **Stripped/wood variants**: Recognizes stripped logs and wood blocks
- **Full inventory search**: Finds saplings anywhere in your inventory, including offhand
- **Config file**: Per-tree toggle via `config/autoreplant.json`

## Supported Versions

| Minecraft | Fabric | Forge | NeoForge |
|-----------|--------|-------|----------|
| 26.1.x | ✅ | ✅ | ✅ |
| 1.21.4 | ✅ | — | ✅ |
| 1.20.1 | ✅ | ✅ | — |

*26.1.x covers 26.1.0 through 26.1.2*

## How It Works

1. Player breaks a log block
2. Sneaking? (if `sneakToDisable: true`) → Skip
3. Matching sapling found in inventory? (skipped if `requireSapling: false`)
4. Ground below is valid? (dirt, grass, nylium for fungi, etc.)
5. Sapling is planted and one is consumed from inventory (no consumption if `requireSapling: false`)

## Supported Trees

| Log Type | Sapling Planted | 2x2 |
|----------|----------------|-----|
| Oak | Oak Sapling | No |
| Spruce | Spruce Sapling | No |
| Birch | Birch Sapling | No |
| Jungle | Jungle Sapling | **Yes** |
| Acacia | Acacia Sapling | No |
| Dark Oak | Dark Oak Sapling | **Yes** |
| Mangrove | Mangrove Propagule | No |
| Cherry | Cherry Sapling | No |
| Pale Oak (1.21.4+) | Pale Oak Sapling | No |
| Crimson Stem/Hyphae | Crimson Fungus | No |
| Warped Stem/Hyphae | Warped Fungus | No |

*Pale Oak is only available on versions 1.21.4 and above (added in 1.21.2). 1.20.1 does not include it.*

## Configuration

On first launch, `config/autoreplant.json` is created:

```json
{
  "sneakToDisable": true,
  "requireSapling": true,
  "trees": {
    "oak": true,
    "spruce": true,
    "birch": true,
    "jungle": true,
    "acacia": true,
    "dark_oak": true,
    "mangrove": true,
    "cherry": true,
    "pale_oak": true,
    "crimson": true,
    "warped": true
  }
}
```

- Set any tree to `false` to disable its auto-replant.
- Set `sneakToDisable` to `false` to always replant regardless of crouching.
- Set `requireSapling` to `false` to plant without consuming saplings — only checks for suitable ground.

## Installation

### 26.1.x

| Platform | Requirements |
|----------|-------------|
| Fabric | Fabric Loader >= 0.18.4, Fabric API, Java 25 |
| Forge | Forge >= 64.0.8, Java 25 |
| NeoForge | NeoForge >= 26.1.0, Java 25 |

### 1.21.4

| Platform | Requirements |
|----------|-------------|
| Fabric | Fabric Loader >= 0.16.9, Fabric API, Java 21 |
| NeoForge | NeoForge >= 21.4.157, Java 21 |

### 1.20.1

| Platform | Requirements |
|----------|-------------|
| Fabric | Fabric Loader >= 0.15.0, Fabric API, Java 17 |
| Forge | Forge >= 47.3.0, Java 17 |

## Building

Each version lives in its own directory as a git worktree. All are checked out locally — no need to switch branches.

```bash
# Fabric 26.1.x
cd fabric-26.1 && ./gradlew build

# Forge 26.1.x
cd forge-26.1 && ./gradlew build

# NeoForge 26.1.x
cd neoforge-26.1 && ./gradlew build

# Fabric 1.21.4
cd fabric-1.21.4 && ./gradlew build

# NeoForge 1.21.4
cd neoforge-1.21.4 && ./gradlew build

# Fabric 1.20.1
cd fabric-1.20.1 && ./gradlew build

# Forge 1.20.1
cd forge-1.20.1 && ./gradlew build
```

Output goes to `build/libs/auto-replant-tree-2.1.0.jar`.

Note: For 26.1.x builds, ensure JAVA_HOME points to Java 25. For 1.20.1 builds, use Java 17.

## License

MIT
