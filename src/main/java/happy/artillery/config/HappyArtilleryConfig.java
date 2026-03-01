package happy.artillery.config;

/**
 * Configurable values for Happy Artillery.
 * Loaded from config/happy-artillery.json in the game directory.
 * All defaults match the original hardcoded values exactly.
 */
public class HappyArtilleryConfig {

    // ── Ammo ────────────────────────────────────────────────────────────────
    public int fireballAmmoMax = 200;
    public int fireballAmmoCost = 1;
    public int ammoDeliveryIntervalMin = 5;

    // ── Shoot / cooldown ────────────────────────────────────────────────────
    public double shootCooldownSeconds = 0.25;
    public double fireRestartDelaySeconds = 0.5;
    public double cryCooldownSeconds = 10.0;

    // ── Heat — BASE / normal biomes ─────────────────────────────────────────
    public int    baseOverheatLimit        = 60;
    public double baseHeatPerShot          = 1.0;   // CooldownTracker.BiomeType.BASE
    public double baseCoolIntervalSeconds  = 3.0;

    // ── Heat — HOT biomes (temp >= 1.5) ────────────────────────────────────
    public int    hotBiomeOverheatLimit       = 60;
    public double hotBiomeHeatPerShot         = 2.0;  // CooldownTracker.BiomeType.HOT
    public double hotBiomeCoolIntervalSeconds = 6.0;

    // ── Heat — COLD biomes (temp <= 0.0 / End) ─────────────────────────────
    public int    coldBiomeOverheatLimit       = 60;
    public double coldBiomeHeatPerShot         = 0.5;  // CooldownTracker.BiomeType.COLD / END
    public double coldBiomeCoolIntervalSeconds = 1.5;

    // ── Heat — Nether ───────────────────────────────────────────────────────
    public int    netherOverheatLimit  = 60;
    public double netherHeatPerShot    = 3.0;   // CooldownTracker.BiomeType.NETHER
    public boolean netherNoCooldown    = true;  // Nether = no cooling (positive infinity interval)

    // ── Water cooling ───────────────────────────────────────────────────────
    public int waterCooldownRate  = 8;
    public int waterCooldownLimit = 5;

    // ── Explosions ──────────────────────────────────────────────────────────
    public int     fireballExplosionPower        = 2;
    public float   overheatExplosionPower        = 4.0f;
    public boolean overheatExplosionCreatesFire  = true;

    // ── Sounds ──────────────────────────────────────────────────────────────
    public float cryVolume = 3.0f;
}
