package happy.artillery.mixin;

import happy.artillery.ControlSlotTagSyncer;
import happy.artillery.config.HAConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {
    
    @Inject(method = "onSlotClick", at = @At("HEAD"))
    private void ha_handleControlSlotChanges(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        
        if (serverPlayer.getVehicle() == null) return;
        
        String typeId = Registries.ENTITY_TYPE.getId(serverPlayer.getVehicle().getType()).toString();
        if (!typeId.equals(HAConstants.HAPPY_GHAST_ENTITY_ID)) return;

        ScreenHandler handler = (ScreenHandler) (Object) this;
        if (slotIndex < 0 || slotIndex >= handler.slots.size()) return;
        
        var slot = handler.getSlot(slotIndex);
        var inv = slot.inventory;
        
        // Only if the slot belongs to the player's own inventory
        if (inv == serverPlayer.getInventory()) {
            int slotIdx = slot.getIndex();
            
            // Handle control slots (4 and 5) - schedule tag sync when items move
            if (slotIdx == 4 || slotIdx == 5) {
                ControlSlotTagSyncer.scheduleTagSync(serverPlayer, slotIdx);
            }
        }
    }
}
