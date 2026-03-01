package happy.artillery;

import happy.artillery.config.ConfigManager;
import happy.artillery.config.HAConstants;
import happy.artillery.event.EntityClickHandler;
import happy.artillery.util.CooldownTracker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Happy Artillery mod initializer.
 */
public class HappyArtillery implements ModInitializer {
    public static final String MOD_ID = "happy-artillery";
    private static final Logger logger = LoggerFactory.getLogger("happy-artillery");

    @Override
    public void onInitialize() {
        logger.info("Initializing Happy Artillery mod...");

        // Load config first — must happen before anything reads HAConstants
        ConfigManager.load();

        // Initialize custom data components first (must be done during mod init)
        CustomDataComponents.initialize();
        logger.info("Custom data components registered");

        // Log constants snapshot
        logger.info("Happy Artillery constants: ammoMax={} overheatBase={} explosion={}",
                HAConstants.FIREBALL_AMMO_MAX(), HAConstants.BASE_OVERHEAT_LIMIT(), HAConstants.FIREBALL_EXPLOSION_POWER());

        // Register event handlers
        EntityClickHandler.register();
        logger.info("Controller stick handler registered");

        // Register server tick event for hotbar management
        ModItems.registerTickHandler();
        logger.info("Event handlers registered");

        // Register player death event to queue cleanup of dropped control items
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // Queue the old player (who just died) for cleanup
            if (oldPlayer instanceof ServerPlayerEntity deadPlayer) {
                ModItems.queuePlayerForCleanup(deadPlayer);
            }
        });
        logger.info("Death event handler registered");

        // Note: Drop prevention is no longer needed as items now have tags synced to slots instead of being immovable

        // Register debug command for testing control items
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                CommandManager.literal("happytest")
                    .executes(context -> {
                        var source = context.getSource();
                        var player = source.getPlayer();
                        if (player == null) {
                            source.sendError(Text.literal("Only players can run this command"));
                            return 0;
                        }
                        
                        try {
                            var inventory = player.getInventory();
                            
                            // Get current items in slots 4 and 5
                            var slot4Item = inventory.getStack(4);
                            var slot5Item = inventory.getStack(5);
                            
                            // Log current state
                            logger.info("[HappyArtillery Test] {} running happytest command", player.getName().getString());
                            logger.info("[HappyArtillery Test] Slot 4: {} (Fire Control: {})", 
                                slot4Item.isEmpty() ? "EMPTY" : slot4Item.getItem().getName().getString(),
                                CustomDataComponents.hasFireControlTag(slot4Item));
                            logger.info("[HappyArtillery Test] Slot 5: {} (Cry Control: {})", 
                                slot5Item.isEmpty() ? "EMPTY" : slot5Item.getItem().getName().getString(),
                                CustomDataComponents.hasCryControlTag(slot5Item));
                            
                            // Apply test tags to slots 4 and 5
                            boolean createdSlot4 = false;
                            if (slot4Item.isEmpty()) {
                                slot4Item = new net.minecraft.item.ItemStack(net.minecraft.item.Items.FIRE_CHARGE);
                                inventory.setStack(4, slot4Item);
                                createdSlot4 = true;
                                logger.info("[HappyArtillery Test] Created temporary fire charge in slot 4");
                            }
                            CustomDataComponents.setFireControlTag(slot4Item);
                            CustomDataComponents.setFireControlName(slot4Item);
                            CustomDataComponents.makeItemImmovable(slot4Item);
                            if (createdSlot4) {
                                CustomDataComponents.setTemporaryTag(slot4Item);
                            }
                            final var slot4Final = slot4Item;
                            logger.info("[HappyArtillery Test] Applied fire control tag to slot 4 (Temporary: " + createdSlot4 + ")");
                            
                            boolean createdSlot5 = false;
                            if (slot5Item.isEmpty()) {
                                slot5Item = new net.minecraft.item.ItemStack(net.minecraft.item.Items.GHAST_TEAR);
                                inventory.setStack(5, slot5Item);
                                createdSlot5 = true;
                                logger.info("[HappyArtillery Test] Created temporary ghast tear in slot 5");
                            }
                            CustomDataComponents.setCryControlTag(slot5Item);
                            CustomDataComponents.setCryControlName(slot5Item);
                            CustomDataComponents.makeItemImmovable(slot5Item);
                            if (createdSlot5) {
                                CustomDataComponents.setTemporaryTag(slot5Item);
                            }
                            final var slot5Final = slot5Item;
                            logger.info("[HappyArtillery Test] Applied cry control tag to slot 5 (Temporary: " + createdSlot5 + ")");
                            
                            // Send feedback to player
                            source.sendFeedback(() -> Text.literal("§a[Test] Applied control item tags to slots 4 & 5"), false);
                            source.sendFeedback(() -> Text.literal("§7Slot 4: Fire Control (Temporary: " + CustomDataComponents.hasTemporaryTag(slot4Final) + ")"), false);
                            source.sendFeedback(() -> Text.literal("§7Slot 5: Cry Control (Temporary: " + CustomDataComponents.hasTemporaryTag(slot5Final) + ")"), false);
                            source.sendFeedback(() -> Text.literal("§7Check server logs for detailed output"), false);
                            
                        } catch (Exception e) {
                            source.sendError(Text.literal("Error: " + e.getMessage()));
                            logger.error("[HappyArtillery Test] Command error: {}", e.getMessage(), e);
                        }
                        return 1;
                    })
            );
        });

        logger.info("Happy Artillery mod initialization complete!");

        // Passive ammo regeneration once per server tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try {
                CooldownTracker.passiveGlobalTick();
                // Verify all players with control items are still on happy ghasts
                ModItems.verifyPlayersWithControlItems(server);
            } catch (Throwable t) {
                if (Math.random() < 0.001) {
                    logger.error("Server tick error (suppressed future occurrences): {}", t.getMessage());
                }
            }
        });
    }
}
