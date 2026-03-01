# 🔥 Happy Artillery

A Fabric mod for Minecraft 1.21.x that turns Happy Ghasts into rideable artillery. Mount up, aim, and lob fireballs.

**By OG Moo-cow / [Pyrehaven](https://pyrehaven.xyz)**

---

## What It Does

- **Shoot fireballs** — right-click with a **Fire Charge** in hand while riding a Happy Ghast
- **Ghast Cry** — right-click with a **Ghast Tear** to let out a terrifying scream
- **Ammo system** — Ghasts hold up to 200 fireballs, regenerating passively over time
- **Heat & Overheat** — rapid firing builds heat; overheat = big sphere explosion
- **Biome mechanics** — heat behaviour differs in Nether, cold, and hot biomes
- **Fully configurable** — every gameplay value is tunable via a JSON config file

---

## Requirements

- Minecraft **1.21.x** (tested on 1.21.11)
- [Fabric Loader](https://fabricmc.net/) >= 0.16.14
- [Fabric API](https://modrinth.com/mod/fabric-api)
- Java 21+

Server-side only — clients don't need it installed.

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/)
2. Drop [Fabric API](https://modrinth.com/mod/fabric-api) into your `mods/` folder
3. Drop `happy-artillery-X.X.X.jar` into your `mods/` folder
4. Launch — a default config is created at `config/happy-artillery.json`

---

## Usage

1. Find and mount a **Happy Ghast** (added in Minecraft 1.21.5)
2. Hold a **Fire Charge** and right-click to shoot a fireball
3. Hold a **Ghast Tear** and right-click to scream
4. Watch your heat — overheat = your ghast explodes

---

## Heat Mechanics

Each shot adds heat. Hit the limit and the ghast detonates in a fireball sphere.

| Biome Type | Overheat Limit | Heat/Shot | Cooling |
|---|---|---|---|
| Normal (BASE) | 60 | 1.0 | -1 every 3.0s |
| Hot (temp ≥ 1.5) | 60 | 2.0 | -1 every 6.0s |
| Cold (temp ≤ 0.0 / End) | 60 | 0.5 | -1 every 1.5s |
| Nether | 60 | 3.0 | ❌ No cooling |

Being **submerged in water** rapidly cools the ghast but prevents firing.

---

## Configuration

Config: `config/happy-artillery.json` (auto-created on first launch)

```json
{
  "fireballAmmoMax": 200,
  "fireballAmmoCost": 1,
  "ammoDeliveryIntervalMin": 5,
  "shootCooldownSeconds": 0.25,
  "fireRestartDelaySeconds": 0.5,
  "cryCooldownSeconds": 10.0,
  "baseOverheatLimit": 60,
  "baseHeatPerShot": 1.0,
  "baseCoolIntervalSeconds": 3.0,
  "hotBiomeOverheatLimit": 60,
  "hotBiomeHeatPerShot": 2.0,
  "hotBiomeCoolIntervalSeconds": 6.0,
  "coldBiomeOverheatLimit": 60,
  "coldBiomeHeatPerShot": 0.5,
  "coldBiomeCoolIntervalSeconds": 1.5,
  "netherOverheatLimit": 60,
  "netherHeatPerShot": 3.0,
  "netherNoCooldown": true,
  "waterCooldownRate": 8,
  "waterCooldownLimit": 5,
  "fireballExplosionPower": 2,
  "overheatExplosionPower": 4.0,
  "overheatExplosionCreatesFire": true,
  "cryVolume": 3.0
}
```

Changes take effect on server restart.

---

## Building From Source

```bash
git clone https://github.com/phred2026-cyber/happy-artillery
cd happy-artillery
./gradlew build
# Output: build/libs/happy-artillery-1.0.0.jar
```

---

## License

MIT

---

## Credits

- **OG Moo-cow** — author
- **Pyrehaven** — [pyrehaven.xyz](https://pyrehaven.xyz)
