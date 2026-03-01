package happy.artillery.config;

/**
 * Central constants for Happy Artillery.
 * Non-configurable identifiers remain as static final fields.
 * All gameplay values delegate to {@link ConfigManager} so they respect happy-artillery.json.
 */
public class HAConstants {

    // ── Not configurable ────────────────────────────────────────────────────
    public static final String HAPPY_GHAST_ENTITY_ID = "minecraft:happy_ghast";
    public static final String FIRE_STICK_ITEM        = "minecraft:fire_charge";
    public static final String CRY_STICK_ITEM         = "minecraft:ghast_tear";
    public static final String FIRE_STICK_NAME        = "§cFire";
    public static final String CRY_STICK_NAME         = "§bCry";

    // ── Ammo ────────────────────────────────────────────────────────────────
    public static int    FIREBALL_AMMO_MAX()           { return ConfigManager.get().fireballAmmoMax; }
    public static int    FIREBALL_AMMO_COST()          { return ConfigManager.get().fireballAmmoCost; }
    public static int    AMMO_DELIVERY_INTERVAL_MIN()  { return ConfigManager.get().ammoDeliveryIntervalMin; }

    // ── Shoot / cooldown ────────────────────────────────────────────────────
    public static double SHOOT_COOLDOWN_SECONDS()     { return ConfigManager.get().shootCooldownSeconds; }
    public static double FIRE_RESTART_DELAY_SECONDS() { return ConfigManager.get().fireRestartDelaySeconds; }
    public static double CRY_COOLDOWN_SECONDS()       { return ConfigManager.get().cryCooldownSeconds; }

    // ── Heat — Base ─────────────────────────────────────────────────────────
    public static int    BASE_OVERHEAT_LIMIT()        { return ConfigManager.get().baseOverheatLimit; }
    public static double BASE_HEAT_PER_SHOT()         { return ConfigManager.get().baseHeatPerShot; }
    public static double BASE_COOL_INTERVAL_SECONDS() { return ConfigManager.get().baseCoolIntervalSeconds; }

    // ── Heat — Hot biome ────────────────────────────────────────────────────
    public static int    HOT_BIOME_OVERHEAT_LIMIT()          { return ConfigManager.get().hotBiomeOverheatLimit; }
    public static int    HOT_BIOME_LIMIT()                   { return ConfigManager.get().hotBiomeOverheatLimit; }
    public static double HOT_BIOME_HEAT_PER_SHOT()           { return ConfigManager.get().hotBiomeHeatPerShot; }
    public static double HOT_BIOME_COOL_INTERVAL_SECONDS()   { return ConfigManager.get().hotBiomeCoolIntervalSeconds; }

    // ── Heat — Cold biome ───────────────────────────────────────────────────
    public static int    COLD_BIOME_OVERHEAT_LIMIT()         { return ConfigManager.get().coldBiomeOverheatLimit; }
    public static int    COLD_BIOME_LIMIT()                  { return ConfigManager.get().coldBiomeOverheatLimit; }
    public static double COLD_BIOME_HEAT_PER_SHOT()          { return ConfigManager.get().coldBiomeHeatPerShot; }
    public static double COLD_BIOME_COOL_INTERVAL_SECONDS()  { return ConfigManager.get().coldBiomeCoolIntervalSeconds; }

    // ── Heat — Nether ───────────────────────────────────────────────────────
    public static int     NETHER_OVERHEAT_LIMIT() { return ConfigManager.get().netherOverheatLimit; }
    public static double  NETHER_HEAT_PER_SHOT()  { return ConfigManager.get().netherHeatPerShot; }
    public static boolean NETHER_NO_COOLDOWN()    { return ConfigManager.get().netherNoCooldown; }

    // ── Water ───────────────────────────────────────────────────────────────
    public static int WATER_COOLDOWN_RATE()  { return ConfigManager.get().waterCooldownRate; }
    public static int WATER_COOLDOWN_LIMIT() { return ConfigManager.get().waterCooldownLimit; }

    // ── Explosions ──────────────────────────────────────────────────────────
    public static int     FIREBALL_EXPLOSION_POWER()        { return ConfigManager.get().fireballExplosionPower; }
    public static float   OVERHEAT_EXPLOSION_POWER()        { return ConfigManager.get().overheatExplosionPower; }
    public static boolean OVERHEAT_EXPLOSION_CREATES_FIRE() { return ConfigManager.get().overheatExplosionCreatesFire; }

    // ── Sounds ──────────────────────────────────────────────────────────────
    public static float CRY_VOLUME() { return ConfigManager.get().cryVolume; }
}
