package happy.artillery.mixin;

import happy.artillery.HappyArtilleryPlayerExtension;
import happy.artillery.data.ExtendedInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixin to inject the HappyArtilleryPlayerExtension interface into ServerPlayerEntity.
 * This allows us to store extended inventory data on players.
 */
@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements HappyArtilleryPlayerExtension {
    private final ExtendedInventory extendedInventory = new ExtendedInventory();

    @Override
    public ExtendedInventory getExtendedInventory() {
        return extendedInventory;
    }
}
