package happy.artillery;

import happy.artillery.config.HAConstants;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages tag synchronization for control slots with delayed application
 */
public class ControlSlotTagSyncer {
    private static final Logger logger = LoggerFactory.getLogger("happy-artillery-items");
    
    // Track pending tag applications with delays to allow item moves to complete
    private static final ConcurrentHashMap<String, Long> pendingTagApplications = new ConcurrentHashMap<>();
    private static final long APPLY_DELAY_MS = 10; // 0.01 seconds
    
    /**
     * Schedule a tag sync for a control slot
     */
    public static void scheduleTagSync(ServerPlayerEntity player, int slotIndex) {
        if (player.getVehicle() == null) return;
        
        String typeId = Registries.ENTITY_TYPE.getId(player.getVehicle().getType()).toString();
        if (!typeId.equals(HAConstants.HAPPY_GHAST_ENTITY_ID)) return;
        
        UUID playerId = player.getUuid();
        String delayKey = playerId.toString() + "_" + slotIndex;
        long now = System.currentTimeMillis();
        
        // Schedule the tag sync with a delay
        pendingTagApplications.put(delayKey, now + APPLY_DELAY_MS);
    }
    
    /**
     * Process all pending tag applications for a player
     */
    public static void processPendingTagApplications(ServerPlayerEntity player) {
        if (player.getVehicle() == null) return;
        
        String typeId = Registries.ENTITY_TYPE.getId(player.getVehicle().getType()).toString();
        if (!typeId.equals(HAConstants.HAPPY_GHAST_ENTITY_ID)) return;
        
        long now = System.currentTimeMillis();
        UUID playerId = player.getUuid();
        var inventory = player.getInventory();
        
        // Check slot 4 (fire control)
        String key4 = playerId.toString() + "_4";
        if (pendingTagApplications.containsKey(key4) && now >= pendingTagApplications.get(key4)) {
            syncControlSlotTags(inventory, 4, true);
            pendingTagApplications.remove(key4);
        }
        
        // Check slot 5 (cry control)
        String key5 = playerId.toString() + "_5";
        if (pendingTagApplications.containsKey(key5) && now >= pendingTagApplications.get(key5)) {
            syncControlSlotTags(inventory, 5, false);
            pendingTagApplications.remove(key5);
        }
    }
    
    /**
     * Sync control slot tags - remove from items not in the slot, apply to items in the slot
     */
    private static void syncControlSlotTags(net.minecraft.inventory.Inventory inventory, int slotIndex, boolean isFire) {
        var currentStack = inventory.getStack(slotIndex);
        String controlType = isFire ? "Fire" : "Cry";
        
        // Remove tags from ALL inventory items that have the control tag (not just hotbar)
        // This ensures temporary items are deleted even if moved to main inventory
        for (int i = 0; i < inventory.size(); i++) {
            if (i == slotIndex) continue; // Skip the control slot itself
            
            var stack = inventory.getStack(i);
            if (stack.isEmpty()) continue;
            
            if (isFire) {
                if (CustomDataComponents.hasFireControlTag(stack)) {
                    // Check if it's temporary - if so, delete it; otherwise just remove tags
                    if (CustomDataComponents.hasTemporaryTag(stack)) {
                        inventory.setStack(i, net.minecraft.item.ItemStack.EMPTY);
                        logger.info("[HappyArtillery] Deleted temporary fire control item from slot {}", i);
                    } else {
                        CustomDataComponents.removeFireControlTag(stack);
                        stack.remove(net.minecraft.component.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
                        stack.remove(net.minecraft.component.DataComponentTypes.CUSTOM_NAME);
                    }
                }
            } else {
                if (CustomDataComponents.hasCryControlTag(stack)) {
                    // Check if it's temporary - if so, delete it; otherwise just remove tags
                    if (CustomDataComponents.hasTemporaryTag(stack)) {
                        inventory.setStack(i, net.minecraft.item.ItemStack.EMPTY);
                        logger.info("[HappyArtillery] Deleted temporary cry control item from slot {}", i);
                    } else {
                        CustomDataComponents.removeCryControlTag(stack);
                        stack.remove(net.minecraft.component.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
                        stack.remove(net.minecraft.component.DataComponentTypes.CUSTOM_NAME);
                    }
                }
            }
        }
        
        // Apply tags to the current item in the control slot
        if (!currentStack.isEmpty()) {
            if (isFire) {
                CustomDataComponents.setFireControlTag(currentStack);
                CustomDataComponents.setFireControlName(currentStack);
            } else {
                CustomDataComponents.setCryControlTag(currentStack);
                CustomDataComponents.setCryControlName(currentStack);
            }
            
            // Add enchantment glow effect
            currentStack.set(net.minecraft.component.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
            
            logger.info("[HappyArtillery] Synced {} control tag to {} in slot {}", 
                controlType, currentStack.getItem().getName().getString(), slotIndex);
        }
    }
}
