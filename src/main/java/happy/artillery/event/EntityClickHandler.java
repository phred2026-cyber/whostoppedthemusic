package happy.artillery.event;

import happy.artillery.config.HAConstants;
import happy.artillery.CustomDataComponents;
import happy.artillery.mixin.accessor.EntityWorldAccessor;
import happy.artillery.util.CooldownTracker;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.chunk.ChunkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles right-click interactions on happy ghasts.
 */
public class EntityClickHandler {
    private static final Logger logger = LoggerFactory.getLogger("happy-artillery");

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            Entity vehicle = player.getVehicle();
            if (vehicle == null || !vehicle.equals(entity)) return ActionResult.PASS;
            if (!isHappyGhast(vehicle)) return ActionResult.PASS;

            return handleStickAction(player, hand, vehicle, (ServerWorld) world);
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient()) return ActionResult.PASS;
            Entity vehicle = player.getVehicle();
            if (vehicle == null) return ActionResult.PASS;
            if (!isHappyGhast(vehicle)) return ActionResult.PASS;

            return handleStickAction(player, hand, vehicle, (ServerWorld) world);
        });
    }

    private static ActionResult handleStickAction(net.minecraft.entity.player.PlayerEntity player, Hand hand, Entity vehicle, ServerWorld world) {
        ItemStack stack = player.getStackInHand(hand);
        if (isFireStick(stack)) {
            return shoot(player, vehicle, world);
        } else if (isCryStick(stack)) {
            return cry(player, vehicle);
        }
        return ActionResult.PASS;
    }

    private static boolean isFireStick(ItemStack stack) {
        // Check for fire control tag (the new way)
        if (CustomDataComponents.hasFireControlTag(stack)) {
            return true;
        }
        // Fallback to original fire charge item check (if not using control system)
        return !stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(HAConstants.FIRE_STICK_ITEM);
    }

    private static boolean isCryStick(ItemStack stack) {
        // Check for cry control tag (the new way)
        if (CustomDataComponents.hasCryControlTag(stack)) {
            return true;
        }
        // Fallback to original ghast tear item check (if not using control system)
        return !stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(HAConstants.CRY_STICK_ITEM);
    }

    private static boolean isHappyGhast(Entity vehicle) {
        String id = Registries.ENTITY_TYPE.getId(vehicle.getType()).toString();
        return id.equals(HAConstants.HAPPY_GHAST_ENTITY_ID);
    }

    private static ActionResult cry(net.minecraft.entity.player.PlayerEntity player, Entity vehicle) {
        if (vehicle.isSubmergedInWater()) {
            return ActionResult.FAIL;
        }
        if (!CooldownTracker.canCry(player.getUuid())) {
            return ActionResult.FAIL;
        }
        CooldownTracker.recordCry(player.getUuid());
        ServerWorld world = (ServerWorld) ((EntityWorldAccessor) vehicle).happy$getWorld();
        world.playSound(null, vehicle.getBlockPos(), SoundEvents.ENTITY_GHAST_SCREAM, SoundCategory.HOSTILE, HAConstants.CRY_VOLUME(), 0.8f);
        return ActionResult.SUCCESS;
    }

    private static ActionResult shoot(net.minecraft.entity.player.PlayerEntity player, Entity vehicle, ServerWorld world) {
        var ghastId = vehicle.getUuid();

        // Water prevents firing but cools
        if (vehicle.isSubmergedInWater()) {
            CooldownTracker.applyWaterCooling(ghastId);
            return ActionResult.FAIL;
        }

        // Heat / Overheat check
        int overheatLimit = biomeOverheatLimit(world, vehicle);
        double currentHeat = CooldownTracker.getFireballHeat(ghastId);
        if (currentHeat >= overheatLimit) {
            return ActionResult.FAIL;
        }

        // Shoot cooldown
        if (!CooldownTracker.canShoot(ghastId)) {
            logger.debug("[HappyArtillery] Shoot blocked by cooldown");
            return ActionResult.FAIL;
        }

        // Ammo check
        int cost = HAConstants.FIREBALL_AMMO_COST();
        int ammo = CooldownTracker.getAmmo(ghastId);
        if (ammo < cost) {
            return ActionResult.FAIL;
        }
        CooldownTracker.useAmmo(ghastId, cost);

        // Apply heat (and detect explosion threshold)
        boolean willExplode = currentHeat + 1 >= overheatLimit;
        CooldownTracker.addFireballHeat(ghastId);
        CooldownTracker.recordShot(ghastId);

        if (willExplode) {
            performOverheatExplosion(world, vehicle, player);
            return ActionResult.SUCCESS;
        }

        spawnFireball(world, vehicle, player);
        return ActionResult.SUCCESS;
    }

    private static void spawnFireball(ServerWorld world, Entity mount, net.minecraft.entity.player.PlayerEntity player) {
        Vec3d dir = player.getRotationVector().normalize();
        float eyeHeight = (mount instanceof LivingEntity) ? ((LivingEntity) mount).getStandingEyeHeight() : (mount.getHeight() * 0.6f);
        double spawnX = mount.getX() + dir.x * 2.0;
        double spawnY = mount.getY() + eyeHeight;
        double spawnZ = mount.getZ() + dir.z * 2.0;
        double speed = 0.5;
        Vec3d velocity = new Vec3d(dir.x * speed, dir.y * speed, dir.z * speed);
        LivingEntity owner = (mount instanceof LivingEntity) ? (LivingEntity) mount : player;
        
        FireballEntity fireball = new FireballEntity(world, owner, velocity, HAConstants.FIREBALL_EXPLOSION_POWER());
        fireball.setPosition(spawnX, spawnY, spawnZ);

        boolean spawned = world.spawnEntity(fireball);
        world.playSound(null, spawnX, spawnY, spawnZ, SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 1.0f, 1.0f);
        logger.debug("[HappyArtillery] Spawned ghast fireball success={}", spawned);
        
        if (spawned) {
            // Force-load chunks along the fireball's path to ensure it can always hit something
            forceLoadFireballPath(world, spawnX, spawnY, spawnZ, velocity);
        } else {
            fallbackInstantRay(world, mount, dir, spawnX, spawnY, spawnZ);
        }
    }

    private static void forceLoadFireballPath(ServerWorld world, double startX, double startY, double startZ, Vec3d velocity) {
        // Pre-load chunks in the direction the fireball is traveling
        // This ensures chunks don't unload as the fireball flies through them
        Vec3d normalizedDir = velocity.normalize();
        double maxDistance = 128.0; // Load chunks up to 128 blocks away
        int steps = 16; // Check every 8 blocks
        
        for (int i = 0; i < steps; i++) {
            double distance = (i / (double) steps) * maxDistance;
            double checkX = startX + normalizedDir.x * distance;
            double checkY = startY + normalizedDir.y * distance;
            double checkZ = startZ + normalizedDir.z * distance;
            
            BlockPos pos = BlockPos.ofFloored(checkX, checkY, checkZ);
            ChunkPos chunkPos = new ChunkPos(pos);
            
            // Force load the chunk - add ticket to keep chunks loaded
            world.getChunkManager().addTicket(
                net.minecraft.server.world.ChunkTicketType.UNKNOWN,
                chunkPos,
                1
            );
        }
    }

    private static void fallbackInstantRay(ServerWorld world, Entity mount, Vec3d dir, double sx, double sy, double sz) {
        Vec3d origin = new Vec3d(sx, sy, sz);
        double maxDistance = 48.0;
        Vec3d target = origin.add(dir.multiply(maxDistance));
        RaycastContext ctx = new RaycastContext(origin, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mount);
        var hit = world.raycast(ctx);
        Vec3d impact = (hit.getType() != net.minecraft.util.hit.HitResult.Type.MISS) ? hit.getPos() : target;
        int steps = (int) (maxDistance / 0.6);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3d point = origin.add(dir.multiply(maxDistance * t));
            if (point.squaredDistanceTo(impact) < 0.4) break;
            world.spawnParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 2, 0.02, 0.02, 0.02, 0.001);
        }
        world.createExplosion(null, impact.x, impact.y, impact.z, HAConstants.FIREBALL_EXPLOSION_POWER(), true, net.minecraft.world.World.ExplosionSourceType.MOB);
        world.playSound(null, origin.x, origin.y, origin.z, SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 1.0f, 1.0f);
        logger.debug("[HappyArtillery] Fallback ray fireball origin={} impact={}", origin, impact);
    }

    private static void performOverheatExplosion(ServerWorld world, Entity mount, net.minecraft.entity.player.PlayerEntity player) {
        Vec3d dir = player.getRotationVector().normalize();
        float eyeHeight = (mount instanceof LivingEntity) ? ((LivingEntity) mount).getStandingEyeHeight() : (mount.getHeight() * 0.6f);
        Vec3d explosionCenter = new Vec3d(mount.getX() + dir.x * 2.0, mount.getY() + eyeHeight, mount.getZ() + dir.z * 2.0);
        
        // Create main explosion
        world.createExplosion(null, explosionCenter.x, explosionCenter.y, explosionCenter.z, HAConstants.OVERHEAT_EXPLOSION_POWER(), HAConstants.OVERHEAT_EXPLOSION_CREATES_FIRE(), net.minecraft.world.World.ExplosionSourceType.TNT);
        
        // Spawn fireballs in all directions (sphere pattern)
        spawnExplosionFireballSphere(world, mount, explosionCenter);
        
        // Spawn fire ring
        spawnFireRing(world, explosionCenter);
        
        logger.info("{}'s ghast overheated and exploded", player.getName().getString());
    }

    private static void spawnExplosionFireballSphere(ServerWorld world, Entity mount, Vec3d center) {
        // Create a dense sphere of fireballs around the explosion center
        LivingEntity owner = (mount instanceof LivingEntity) ? (LivingEntity) mount : null;
        int fireballs = 48; // More fireballs for denser sphere coverage
        
        for (int i = 0; i < fireballs; i++) {
            // Generate uniform sphere distribution using golden spiral
            double phi = Math.acos(2.0 * (i / (double) fireballs) - 1.0);
            double theta = Math.PI * (1.0 + Math.sqrt(5.0)) * i; // Golden angle
            
            double radius = 2.5;
            double vx = radius * Math.sin(phi) * Math.cos(theta);
            double vy = radius * Math.sin(phi) * Math.sin(theta);
            double vz = radius * Math.cos(phi);
            
            Vec3d velocity = new Vec3d(vx, vy, vz);
            FireballEntity fireball = new FireballEntity(world, owner, velocity, HAConstants.FIREBALL_EXPLOSION_POWER());
            fireball.setPosition(center.x, center.y, center.z);
            world.spawnEntity(fireball);
        }
        
        world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 2.0f, 0.8f);
    }

    private static void spawnFireRing(ServerWorld world, Vec3d center) {
        int radius = 5;
        for (int i = 0; i < 15; i++) {
            double angle = world.getRandom().nextDouble() * Math.PI * 2;
            double dist = world.getRandom().nextDouble() * radius;
            double fx = center.x + dist * Math.cos(angle);
            double fz = center.z + dist * Math.sin(angle);
            double fy = center.y + (world.getRandom().nextDouble() - 0.5) * 2;
            BlockPos bp = BlockPos.ofFloored(fx, fy, fz);
            if (world.getBlockState(bp).isAir() && world.getBlockState(bp.down()).isSolid()) {
                world.setBlockState(bp, net.minecraft.block.Blocks.FIRE.getDefaultState());
            }
        }
    }

    private static int biomeOverheatLimit(ServerWorld world, Entity vehicle) {
        BlockPos pos = BlockPos.ofFloored(vehicle.getX(), vehicle.getY(), vehicle.getZ());
        var biome = world.getBiome(pos).value();
        float temp = biome.getTemperature();
        
        if (world.getRegistryKey().getValue().toString().contains("nether") || temp >= 1.5f) {
            return HAConstants.HOT_BIOME_LIMIT();
        } else if (world.getRegistryKey().getValue().toString().contains("end") || temp <= 0.0f) {
            return HAConstants.COLD_BIOME_LIMIT();
        }
        return HAConstants.BASE_OVERHEAT_LIMIT();
    }
}
