package happy.artillery;

import happy.artillery.config.HAConstants;
import happy.artillery.mixin.accessor.EntityWorldAccessor;
import happy.artillery.util.CooldownTracker;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages special controller stick items and hotbar swapping for riding happy ghasts.
 */
public class ModItems {
    private static final Logger logger = LoggerFactory.getLogger("happy-artillery-items");
    
    // Track players who died (for cleanup in tick handler)
    private static final java.util.concurrent.ConcurrentHashMap<java.util.UUID, Long> recentlyDeadPlayers = new java.util.concurrent.ConcurrentHashMap<>();

    public static void registerTickHandler() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try {
                // Process all ghasts in all worlds for cooling/regen
                for (var world : server.getWorlds()) {
                    processAllGhastsInWorld(world);
                }
                
                // Clean up dropped items from recently dead players
                cleanupDeadPlayerItems(server);
                
                // For each player, ensure slots 4 and 5 have correct tag/enchantment state
                List<ServerPlayerEntity> players = new ArrayList<>(server.getPlayerManager().getPlayerList());
                for (ServerPlayerEntity player : players) {
                    try {
                        // Universal cleanup check for ALL players - catches misplaced items
                        // Run less frequently to avoid performance impact (every 20 ticks = 1 second)
                        if (server.getTicks() % 20 == 0) {
                            checkAndCleanupControlItems(player);
                        }
                        
                        // Only apply control slot rules to players riding happy ghasts
                        if (isPlayerOnHappyGhast(player)) {
                            ensureControlSlotState(player);
                        }
                        
                        updatePlayerItems(player, server);
                    } catch (Exception e) {
                        logger.error("Error updating player items for {}: {}", player.getName().getString(), e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                logger.error("Error in server tick handler: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Check if player is riding a happy ghast
     */
    private static boolean isPlayerOnHappyGhast(ServerPlayerEntity player) {
        if (player.getVehicle() == null) return false;
        String typeId = Registries.ENTITY_TYPE.getId(player.getVehicle().getType()).toString();
        return typeId.equals(HAConstants.HAPPY_GHAST_ENTITY_ID);
    }

    /**
     * Universal cleanup check: scans entire inventory for misplaced control items
     * Called on block updates, player actions, etc. to catch and fix misplaced items
     */
    public static void checkAndCleanupControlItems(ServerPlayerEntity player) {
        try {
            var inventory = player.getInventory();
            boolean isRiding = isPlayerOnHappyGhast(player);
            
            // Scan ALL inventory slots for misplaced control items
            for (int i = 0; i < inventory.size(); i++) {
                var stack = inventory.getStack(i);
                if (stack.isEmpty()) continue;
                
                boolean hasFireTag = CustomDataComponents.hasFireControlTag(stack);
                boolean hasCryTag = CustomDataComponents.hasCryControlTag(stack);
                boolean isTemp = CustomDataComponents.hasTemporaryTag(stack);
                
                // Only slots 4 and 5 should have control tags when riding
                boolean shouldHaveTags = isRiding && (i == 4 || i == 5);
                
                // If item has control tags but shouldn't
                if ((hasFireTag || hasCryTag) && !shouldHaveTags) {
                    // If temporary, delete it
                    if (isTemp) {
                        inventory.setStack(i, ItemStack.EMPTY);
                        logger.info("[HappyArtillery] Cleanup check: Deleted temporary control item from slot {}", i);
                    } else {
                        // Otherwise just remove tags
                        CustomDataComponents.removeFireControlTag(stack);
                        CustomDataComponents.removeCryControlTag(stack);
                        stack.remove(net.minecraft.component.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
                        stack.remove(net.minecraft.component.DataComponentTypes.CUSTOM_NAME);
                        inventory.setStack(i, stack);
                        logger.info("[HappyArtillery] Cleanup check: Removed control tags from slot {}", i);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("[HappyArtillery] Error in universal cleanup check: {}", e.getMessage(), e);
        }
    }

    /**
     * Ensure slots 4 and 5 have the correct tag and enchantment state
     * Slot 4 should have Fire Control tag with enchantment, slot 5 should have Cry Control tag with enchantment
     * Remove all control tags from items NOT in slots 4 or 5
     */
    private static void ensureControlSlotState(ServerPlayerEntity player) {
        var inventory = player.getInventory();
        
        // Check slot 4 (Fire Control)
        var slot4 = inventory.getStack(4);
        if (!slot4.isEmpty()) {
            if (!CustomDataComponents.hasFireControlTag(slot4)) {
                CustomDataComponents.setFireControlTag(slot4);
                CustomDataComponents.setFireControlName(slot4);
                slot4.set(net.minecraft.component.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
            }
        }
        
        // Check slot 5 (Cry Control)
        var slot5 = inventory.getStack(5);
        if (!slot5.isEmpty()) {
            if (!CustomDataComponents.hasCryControlTag(slot5)) {
                CustomDataComponents.setCryControlTag(slot5);
                CustomDataComponents.setCryControlName(slot5);
                slot5.set(net.minecraft.component.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
            }
        }
        
        // Remove all control tags from items NOT in slots 4 or 5
        for (int i = 0; i < inventory.size(); i++) {
            if (i == 4 || i == 5) continue; // Skip control slots
            
            var stack = inventory.getStack(i);
            if (stack.isEmpty()) continue;
            
            // FIRST: Check if it's temporary - if so, delete it immediately
            if (CustomDataComponents.hasTemporaryTag(stack)) {
                inventory.setStack(i, ItemStack.EMPTY);
                continue;
            }
            
            // THEN: Remove any fire or cry control tags
            boolean removedAny = false;
            if (CustomDataComponents.hasFireControlTag(stack)) {
                CustomDataComponents.removeFireControlTag(stack);
                removedAny = true;
            }
            if (CustomDataComponents.hasCryControlTag(stack)) {
                CustomDataComponents.removeCryControlTag(stack);
                removedAny = true;
            }
            if (removedAny) {
                stack.remove(net.minecraft.component.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
                stack.remove(net.minecraft.component.DataComponentTypes.CUSTOM_NAME);
                // IMPORTANT: Call setStack() to persist changes to the inventory
                inventory.setStack(i, stack);
            }
        }
    }

    private static void processAllGhastsInWorld(net.minecraft.world.World world) {
        // Apply cooling and regen to all happy ghasts in this world
        // Iterate through all entities in the world's entity manager
        if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            for (var entity : serverWorld.iterateEntities()) {
                var id = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
                if (id.equals(HAConstants.HAPPY_GHAST_ENTITY_ID)) {
                    var ghastId = entity.getUuid();
                    
                    // Check if currently in firing mode (time since last shot > 0)
                    float timeSinceShot = CooldownTracker.getTimeSinceLastShot(ghastId);
                    boolean isCurrentlyFiring = timeSinceShot > 0;
                    
                    // Only apply cooling when NOT actively firing
                    // Heat decreases by 1 every 3 seconds of non-firing mode
                    if (!isCurrentlyFiring) {
                        double heat = CooldownTracker.getFireballHeat(ghastId);
                        if (heat > 0) {
                            // Apply 1/3 heat reduction per server tick (20 ticks per second)
                            // This results in 1 heat point every 3 seconds (60 ticks)
                            if (serverWorld.getTime() % 60 == 0) {
                                CooldownTracker.setFireballHeat(ghastId, heat - 1);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void updatePlayerItems(ServerPlayerEntity player, net.minecraft.server.MinecraftServer server) {
        boolean isDriverOnHappyGhast = isDriverOnHappyGhast(player);

        try {
            if (isDriverOnHappyGhast) {
                // Setup control items in slots 5 and 6
                logger.info("[HappyArtillery] Player {} is riding happy ghast - setting up control items", player.getName().getString());
                setupControlItems(player);
                // Show status display every tick while riding
                showRidingStatusDisplay(player, player.getVehicle());
            } else {
                // Clean up control items when player dismounts
                logger.debug("[HappyArtillery] Player {} is not riding happy ghast - cleaning up control items", player.getName().getString());
                cleanupControlItems(player);
                // Clean up boss bar when player dismounts
                cleanupBossBarsForPlayer(player.getUuid());
            }
        } catch (Exception e) {
            logger.error("Unexpected error in updatePlayerItems: {}", e.getMessage(), e);
        }
    }

    /**
     * Setup control items in slots 4 and 5 when player enters happy ghast
     */
    private static void setupControlItems(ServerPlayerEntity player) {
        var inventory = player.getInventory();
        
        logger.debug("[HappyArtillery] Setting up control items for {}", player.getName().getString());
        
        // Fire control item in slot 4
        handleControlSlot(inventory, 4, true, false, player);
        
        // Cry control item in slot 5
        handleControlSlot(inventory, 5, false, true, player);
    }

    /**
     * Handle a control item slot - either tag existing item or create temporary control item
     */
    private static void handleControlSlot(net.minecraft.inventory.Inventory inventory, int slotIndex, 
                                         boolean isFire, boolean isCry, ServerPlayerEntity player) {
        ItemStack stack = inventory.getStack(slotIndex);
        String controlType = isFire ? "Fire" : "Cry";
        
        if (stack.isEmpty()) {
            // Create control item with fire charge or ghast tear
            ItemStack controlItem;
            if (isFire) {
                controlItem = new ItemStack(net.minecraft.item.Items.FIRE_CHARGE);
            } else {
                controlItem = new ItemStack(net.minecraft.item.Items.GHAST_TEAR);
            }
            
            // Apply control tags
            if (isFire) {
                CustomDataComponents.setFireControlTag(controlItem);
                CustomDataComponents.setFireControlName(controlItem);
            } else {
                CustomDataComponents.setCryControlTag(controlItem);
                CustomDataComponents.setCryControlName(controlItem);
            }
            
            // Mark as temporary since we created it
            CustomDataComponents.setTemporaryTag(controlItem);
            
            // Add enchantment glow effect
            controlItem.set(net.minecraft.component.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
            
            inventory.setStack(slotIndex, controlItem);
            logger.info("[HappyArtillery] Created {} control item in slot {} (Temporary: {})", 
                controlType, slotIndex, CustomDataComponents.hasTemporaryTag(controlItem));
        } else {
            // Item exists - only tag it if it doesn't already have the control tag
            boolean needsTagging = false;
            if (isFire) {
                needsTagging = !CustomDataComponents.hasFireControlTag(stack);
            } else {
                needsTagging = !CustomDataComponents.hasCryControlTag(stack);
            }
            
            // Only apply tags once - don't re-apply every tick
            if (needsTagging) {
                if (isFire) {
                    CustomDataComponents.setFireControlTag(stack);
                    CustomDataComponents.setFireControlName(stack);
                } else {
                    CustomDataComponents.setCryControlTag(stack);
                    CustomDataComponents.setCryControlName(stack);
                }
                
                // Add enchantment glow effect
                stack.set(net.minecraft.component.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
                
                // IMPORTANT: Call setStack() to persist changes to the inventory
                inventory.setStack(slotIndex, stack);
                
                logger.info("[HappyArtillery] Tagged {} item in slot {} as {} control", 
                    stack.getItem().getName().getString(), slotIndex, controlType);
            }
        }
    }

    /**
     * Clean up control items when player leaves happy ghast
     */
    private static void cleanupControlItems(ServerPlayerEntity player) {
        var inventory = player.getInventory();
        
        // Clean up slot 4 (fire control)
        cleanupControlSlot(inventory, 4, true, false, player);
        
        // Clean up slot 5 (cry control)
        cleanupControlSlot(inventory, 5, false, true, player);
    }

    /**
     * Clean up a control slot - remove tags and delete if temporary
     */
    private static void cleanupControlSlot(net.minecraft.inventory.Inventory inventory, int slotIndex, 
                                          boolean isFire, boolean isCry, ServerPlayerEntity player) {
        ItemStack stack = inventory.getStack(slotIndex);
        
        if (stack.isEmpty()) {
            return;
        }
        
        // FIRST: Check if it's temporary - if so, delete it immediately and inform player
        logger.info("[HappyArtillery] Cleanup slot {}: checking for temporary tag (has tag: {})", slotIndex, CustomDataComponents.hasTemporaryTag(stack));
        if (CustomDataComponents.hasTemporaryTag(stack)) {
            inventory.setStack(slotIndex, ItemStack.EMPTY);
            logger.info("[HappyArtillery] Deleted temporary item from cleanup slot {}", slotIndex);
            // player.sendMessage(Text.literal("[HappyArtillery] Deleted temporary item from slot " + slotIndex), false);
            return;
        }
        
        // THEN: Check for fire or cry control tags and remove them
        boolean removed = false;
        if (CustomDataComponents.hasFireControlTag(stack)) {
            CustomDataComponents.removeFireControlTag(stack);
            removed = true;
        }
        if (CustomDataComponents.hasCryControlTag(stack)) {
            CustomDataComponents.removeCryControlTag(stack);
            removed = true;
        }
        if (removed) {
            stack.remove(net.minecraft.component.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
            stack.remove(net.minecraft.component.DataComponentTypes.CUSTOM_NAME);
            // IMPORTANT: Call setStack() to persist changes to the inventory
            inventory.setStack(slotIndex, stack);
            // player.sendMessage(Text.literal("[HappyArtillery] Removed control tags from slot " + slotIndex), false);
        }
    }

    private static void showRidingStatusDisplay(ServerPlayerEntity player, net.minecraft.entity.Entity vehicle) {
        if (vehicle == null || vehicle.isRemoved()) {
            // Vehicle is null or dead - clean up boss bar
            cleanupBossBarsForPlayer(player.getUuid());
            return;
        }

        try {
            var ghastId = vehicle.getUuid();
            var world = ((EntityWorldAccessor) vehicle).happy$getWorld();
            if (world == null) {
                logger.warn("World is null for riding status display");
                return;
            }

            // Detect biome and update CooldownTracker with the biome type
            var blockPos = net.minecraft.util.math.BlockPos.ofFloored(vehicle.getX(), vehicle.getY(), vehicle.getZ());
            var biome = world.getBiome(blockPos).value();
            float temperature = biome.getTemperature();
            String worldName = world.getRegistryKey().getValue().toString();
            
            // Determine biome/type for heat mechanics.
            // Dimension overrides first: Nether/End get special handling.
            CooldownTracker.BiomeType biomeType;
            if (worldName.contains("nether") || worldName.toLowerCase().contains("the_nether")) {
                biomeType = CooldownTracker.BiomeType.NETHER;
            } else if (worldName.contains("end") || worldName.toLowerCase().contains("the_end")) {
                // End dimension uses low heat per shot and fast cooling
                biomeType = CooldownTracker.BiomeType.END;
            } else {
                // Overworld: use temperature heuristics rather than registry id (more portable).
                // Treat very warm biomes (temp >= 1.0) as HOT (savanna/badlands),
                // and very cold biomes (temp <= 0.0) as COLD (snow/ice).
                float overworldTemp = biome.getTemperature();
                if (overworldTemp >= 1.0f) {
                    biomeType = CooldownTracker.BiomeType.HOT;
                } else if (overworldTemp <= 0.0f) {
                    biomeType = CooldownTracker.BiomeType.COLD;
                } else {
                    biomeType = CooldownTracker.BiomeType.BASE;
                }
            }
            // Always update biome type to ensure it's current
            CooldownTracker.setBiomeType(ghastId, biomeType);

            // Get game values early to determine if currently firing
            int remainingAmmo = CooldownTracker.getAmmo(ghastId);
            int timeUntilNextAmmo = CooldownTracker.getSecondsUntilNextAmmo(ghastId);
            int overheatLimit = biomeType.overheatLimit();
            double heat = CooldownTracker.getFireballHeat(ghastId);
            
            // Check if currently firing (within shooting cooldown)
            float shootCooldownRemaining = CooldownTracker.getRemainingShootCooldown(ghastId);
            boolean isCurrentlyFiring = shootCooldownRemaining > 0;

            // Simple firing state: in firing mode if time-since-last-shot > 0
            long now = System.currentTimeMillis();
            float timeSinceShot = CooldownTracker.getTimeSinceLastShot(ghastId);
            boolean shouldUseFiringMode = timeSinceShot > 0;
            int overheatRemaining = (int) (overheatLimit - heat);

            boolean isHotBiome = biomeType == CooldownTracker.BiomeType.HOT || biomeType == CooldownTracker.BiomeType.NETHER;
            boolean isColdBiome = biomeType == CooldownTracker.BiomeType.COLD;

            // === BOSS BAR FOR HEAT ===
            float heatPercentage = Math.min(1.0f, (float) heat / overheatLimit);
            
            // Determine color based on heat level
            net.minecraft.server.world.ServerWorld serverWorld = (ServerWorld) world;
            net.minecraft.entity.boss.BossBar.Color barColor;
            net.minecraft.entity.boss.BossBar.Style barStyle = net.minecraft.entity.boss.BossBar.Style.PROGRESS;
            
            if (overheatRemaining <= 5) {
                barColor = net.minecraft.entity.boss.BossBar.Color.RED; // Critical
            } else if (overheatRemaining <= 15) {
                barColor = net.minecraft.entity.boss.BossBar.Color.YELLOW; // Warning
            } else if (isHotBiome) {
                barColor = net.minecraft.entity.boss.BossBar.Color.YELLOW; // Hot biome
            } else if (isColdBiome) {
                barColor = net.minecraft.entity.boss.BossBar.Color.BLUE; // Cold biome
            } else {
                barColor = net.minecraft.entity.boss.BossBar.Color.GREEN; // Normal
            }
            
            // Get or create boss bar for this ghast + player combo
            // Each player riding a ghast gets their own boss bar
            String bossBarKey = ghastId.toString() + "_" + player.getUuid().toString();
            if (!activeHeatBars.containsKey(bossBarKey)) {
                var newBar = new net.minecraft.entity.boss.ServerBossBar(
                    Text.literal("§4Heat System"),
                    barColor,
                    barStyle
                );
                activeHeatBars.put(bossBarKey, newBar);
                bossBarOwners.put(bossBarKey, player.getUuid());
            }
            
            var bossBar = activeHeatBars.get(bossBarKey);
            bossBar.setPercent(heatPercentage);
            bossBar.setColor(barColor);
            
            // Update boss bar name with clean heat info (round to nearest 0.5)
            double displayHeat = Math.round(heat * 2.0) / 2.0;
            String barTitle = "§cHeat: " + String.format("%.1f", displayHeat).replaceAll("\\.0$", "") + "/" + overheatLimit;
            bossBar.setName(Text.literal(barTitle));
            
            // Only show boss bar to the specific player riding this ghast
            bossBar.clearPlayers();
            bossBar.addPlayer(player);
            
            // Spawn escalating warning particles when near overheat
            if (overheatRemaining <= 10) {
                int sparks = (11 - overheatRemaining) * 2;
                double baseX = vehicle.getX();
                double baseY = vehicle.getY() + (vehicle instanceof net.minecraft.entity.LivingEntity ? ((net.minecraft.entity.LivingEntity) vehicle).getStandingEyeHeight() : vehicle.getHeight() * 0.6);
                double baseZ = vehicle.getZ();
                serverWorld.spawnParticles(ParticleTypes.FIREWORK, baseX, baseY, baseZ, sparks, 0.3, 0.3, 0.3, 0.01);
            }

            // === ACTION BAR TEXT ===
            List<String> statusParts = new ArrayList<>();
            
            // Simple ammo display only
            String ammoColor = remainingAmmo < 50 ? "§c" : (remainingAmmo < 100 ? "§6" : "§a");
            statusParts.add(ammoColor + "Ammo: " + remainingAmmo + "/" + HAConstants.FIREBALL_AMMO_MAX());
            
            // When stationary (not firing), show cooling mode
            if (!shouldUseFiringMode) {
                // Determine cooling mode based on biome
                String coolingMode;
                if (biomeType == CooldownTracker.BiomeType.NETHER) {
                    coolingMode = "§4No cooling";
                } else if (biomeType == CooldownTracker.BiomeType.COLD) {
                    coolingMode = "§bFast cooling";
                } else if (biomeType == CooldownTracker.BiomeType.HOT) {
                    coolingMode = "§6Slow cooling";
                } else {
                    coolingMode = "§aNormal cooling";
                }
                statusParts.add(coolingMode);
            }
            
            // Critical warnings
            if (overheatRemaining <= 5) {
                statusParts.add("§c§lOVERHEATING!");
            } else if (overheatRemaining <= 15) {
                statusParts.add("§6⚠ Warning");
            }
            
            // Send the live feed as action bar
            if (!statusParts.isEmpty()) {
                String status = String.join(" §f| ", statusParts);
                player.sendMessage(Text.literal(status), true);
            }
        } catch (Exception e) {
            logger.error("Error in showRidingStatusDisplay: {}", e.getMessage(), e);
        }
    }

    // Boss bar tracking - maps ghast UUID to bar, and tracks which player owns which bar
    private static final java.util.concurrent.ConcurrentHashMap<String, net.minecraft.entity.boss.ServerBossBar> activeHeatBars = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.UUID> bossBarOwners = new java.util.concurrent.ConcurrentHashMap<>();

    private static void cleanupBossBarsForPlayer(java.util.UUID playerUuid) {
        // Find and remove all boss bars owned by this player
        var keysToRemove = new java.util.ArrayList<String>();
        for (var entry : bossBarOwners.entrySet()) {
            if (entry.getValue().equals(playerUuid)) {
                keysToRemove.add(entry.getKey());
            }
        }
        
        // Remove the bars and owners
        for (var key : keysToRemove) {
            var bar = activeHeatBars.remove(key);
            if (bar != null) {
                bar.clearPlayers();
            }
            bossBarOwners.remove(key);
        }
    }

    private static void applyBiomeHeatCooling(net.minecraft.world.World world, java.util.UUID ghastId) {
        long now = System.currentTimeMillis();
        if (!lastHeatCoolTime.containsKey(ghastId)) {
            lastHeatCoolTime.put(ghastId, now);
            return;
        }

        long lastCool = lastHeatCoolTime.get(ghastId);
        long timePassedMs = now - lastCool;
        if (timePassedMs < 1000) return; // run roughly once per second

        double currentHeat = CooldownTracker.getFireballHeat(ghastId);
        if (currentHeat <= 0) {
            lastHeatCoolTime.put(ghastId, now);
            return;
        }

        // Determine biome type from tracker (should have been set by showRidingStatusDisplay)
        var biomeType = CooldownTracker.getBiomeType(ghastId);
        // Nether: no cooling
        if (biomeType == CooldownTracker.BiomeType.NETHER) {
            lastHeatCoolTime.put(ghastId, now);
            return;
        }

        double interval = biomeType.coolIntervalSeconds();
        if (Double.isInfinite(interval) || interval <= 0) {
            lastHeatCoolTime.put(ghastId, now);
            return;
        }

        // Calculate how many full intervals have passed and reduce heat accordingly
        long fullIntervals = (long) (timePassedMs / (long) (interval * 1000));
        if (fullIntervals > 0) {
            double newHeat = Math.max(0.0, currentHeat - fullIntervals);
            CooldownTracker.setFireballHeat(ghastId, newHeat);
            lastHeatCoolTime.put(ghastId, now);
        } else {
            // Update timestamp so we won't loop too frequently
            lastHeatCoolTime.put(ghastId, now);
        }
    }

    // Add static map for tracking last cooling time
    private static final java.util.concurrent.ConcurrentHashMap<java.util.UUID, Long> lastHeatCoolTime = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<java.util.UUID, double[]> lastKnownPositions = new java.util.concurrent.ConcurrentHashMap<>();

    private static boolean isDriverOnHappyGhast(ServerPlayerEntity player) {
        var vehicle = player.getVehicle();
        if (vehicle == null) {
            logger.debug("[HappyArtillery] {} has no vehicle", player.getName().getString());
            return false;
        }
        var id = Registries.ENTITY_TYPE.getId(vehicle.getType()).toString();
        logger.debug("[HappyArtillery] {} vehicle type: {}", player.getName().getString(), id);
        if (!id.equals(HAConstants.HAPPY_GHAST_ENTITY_ID)) {
            logger.debug("[HappyArtillery] Vehicle is not happy ghast, got: {}", id);
            return false;
        }
        var firstPassenger = vehicle.getFirstPassenger();
        boolean isDriver = firstPassenger == player;
        logger.debug("[HappyArtillery] {} is driver of ghast: {}", player.getName().getString(), isDriver);
        return isDriver;
    }

    public static boolean isControlItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return CustomDataComponents.hasFireControlTag(stack) || CustomDataComponents.hasCryControlTag(stack);
    }

    public static boolean isFireControlItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return CustomDataComponents.hasFireControlTag(stack);
    }

    public static boolean isCryControlItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return CustomDataComponents.hasCryControlTag(stack);
    }

    private static int getBiomeBasedOverheatLimit(net.minecraft.world.World world, net.minecraft.entity.Entity vehicle) {
        var blockPos = net.minecraft.util.math.BlockPos.ofFloored(vehicle.getX(), vehicle.getY(), vehicle.getZ());
        var biome = world.getBiome(blockPos).value();
        float temperature = biome.getTemperature();

        if (world.getRegistryKey().getValue().toString().contains("nether") || temperature >= 1.5f) {
            return HAConstants.HOT_BIOME_LIMIT();
        } else if (world.getRegistryKey().getValue().toString().contains("end") || temperature <= 0.0f) {
            return HAConstants.COLD_BIOME_LIMIT();
        }
        return HAConstants.BASE_OVERHEAT_LIMIT();
    }

    /**
     * Clean up dropped control items from the ground and from mobs
     */
    public static void cleanupDroppedControlItems(net.minecraft.server.MinecraftServer server) {
        try {
            for (var world : server.getWorlds()) {
                if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                    // Iterate through all item entities in the world
                    for (var entity : serverWorld.iterateEntities()) {
                        if (entity instanceof net.minecraft.entity.ItemEntity itemEntity) {
                            var stack = itemEntity.getStack();
                            // Check if item has fire or cry control tag
                            if (CustomDataComponents.hasFireControlTag(stack) || CustomDataComponents.hasCryControlTag(stack)) {
                                // If temporary, delete it; otherwise remove tags
                                if (CustomDataComponents.hasTemporaryTag(stack)) {
                                    itemEntity.discard();
                                    logger.info("[HappyArtillery] Deleted temporary control item dropped on ground");
                                } else {
                                    CustomDataComponents.removeFireControlTag(stack);
                                    CustomDataComponents.removeCryControlTag(stack);
                                    stack.remove(net.minecraft.component.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
                                    stack.remove(net.minecraft.component.DataComponentTypes.CUSTOM_NAME);
                                }
                            }
                        }
                        // Also check mobs holding control items
                        else if (entity instanceof net.minecraft.entity.LivingEntity living && !(entity instanceof net.minecraft.entity.player.PlayerEntity)) {
                            // Check main hand and off hand
                            var mainHand = living.getMainHandStack();
                            var offHand = living.getOffHandStack();
                            
                            for (var stack : java.util.Arrays.asList(mainHand, offHand)) {
                                if (!stack.isEmpty() && (CustomDataComponents.hasFireControlTag(stack) || CustomDataComponents.hasCryControlTag(stack))) {
                                    // If temporary, delete it; otherwise remove tags
                                    if (CustomDataComponents.hasTemporaryTag(stack)) {
                                        stack.setCount(0);
                                        logger.info("[HappyArtillery] Cleaned up temporary control item from mob hand");
                                    } else {
                                        CustomDataComponents.removeFireControlTag(stack);
                                        CustomDataComponents.removeCryControlTag(stack);
                                        stack.remove(net.minecraft.component.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
                                        stack.remove(net.minecraft.component.DataComponentTypes.CUSTOM_NAME);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("[HappyArtillery] Error cleaning up dropped control items: {}", e.getMessage(), e);
        }
    }

    /**
     * Verify that all players with control item tags are still riding happy ghasts
     */
    public static void verifyPlayersWithControlItems(net.minecraft.server.MinecraftServer server) {
        try {
            for (var player : server.getPlayerManager().getPlayerList()) {
                var inventory = player.getInventory();
                
                // Check slots 4 and 5 for control tags
                var slot4 = inventory.getStack(4);
                var slot5 = inventory.getStack(5);
                
                boolean hasControlTags = (CustomDataComponents.hasFireControlTag(slot4) || CustomDataComponents.hasCryControlTag(slot4)) ||
                                        (CustomDataComponents.hasFireControlTag(slot5) || CustomDataComponents.hasCryControlTag(slot5));
                
                // If player has control tags, verify they're still on happy ghast
                if (hasControlTags) {
                    if (!isDriverOnHappyGhast(player)) {
                        logger.warn("[HappyArtillery] Player {} has control item tags but is not on happy ghast! Cleaning up...", player.getName().getString());
                        cleanupControlItems(player);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("[HappyArtillery] Error verifying players with control items: {}", e.getMessage(), e);
        }
    }

    /**
     * Queue a player for control item cleanup (called when player dies)
     */
    public static void queuePlayerForCleanup(ServerPlayerEntity deadPlayer) {
        recentlyDeadPlayers.put(deadPlayer.getUuid(), System.currentTimeMillis());
    }

    /**
     * Clean up dropped control items from recently dead players (called each tick)
     */
    private static void cleanupDeadPlayerItems(net.minecraft.server.MinecraftServer server) {
        long now = System.currentTimeMillis();
        var toRemove = new java.util.ArrayList<java.util.UUID>();
        
        for (var entry : recentlyDeadPlayers.entrySet()) {
            java.util.UUID playerId = entry.getKey();
            long deathTime = entry.getValue();
            
            // Only process if at least 100ms has passed (allows items to be dropped to ground)
            if (now - deathTime < 100) continue;
            
            // Find the player's last known death location by checking if they're in the player list
            // If not found, still try to clean up items with control tags in all worlds
            var deadPlayer = server.getPlayerManager().getPlayer(playerId);
            boolean found = deadPlayer != null;
            
            // Clean up items in all worlds
            for (var world : server.getWorlds()) {
                if (world instanceof ServerWorld serverWorld) {
                    // Iterate through all item entities to find dropped control items
                    var itemEntities = new java.util.ArrayList<net.minecraft.entity.ItemEntity>();
                    for (var entity : serverWorld.iterateEntities()) {
                        if (entity instanceof net.minecraft.entity.ItemEntity itemEntity) {
                            itemEntities.add(itemEntity);
                        }
                    }
                    
                    for (var itemEntity : itemEntities) {
                        var stack = itemEntity.getStack();
                        
                        // Check if item has fire or cry control tag
                        if (CustomDataComponents.hasFireControlTag(stack) || CustomDataComponents.hasCryControlTag(stack)) {
                            // If temporary, delete it; otherwise remove tags
                            if (CustomDataComponents.hasTemporaryTag(stack)) {
                                itemEntity.discard();
                                logger.info("[HappyArtillery] Cleaned up temporary control item from ground");
                            } else {
                                CustomDataComponents.removeFireControlTag(stack);
                                CustomDataComponents.removeCryControlTag(stack);
                                stack.remove(net.minecraft.component.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
                                stack.remove(net.minecraft.component.DataComponentTypes.CUSTOM_NAME);
                                logger.info("[HappyArtillery] Removed control tags from dropped item");
                            }
                        }
                    }
                }
            }
            
            toRemove.add(playerId);
        }
        
        // Remove processed entries
        for (var playerId : toRemove) {
            recentlyDeadPlayers.remove(playerId);
        }
    }
}
