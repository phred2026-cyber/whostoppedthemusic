package happy.artillery.mixin;

import happy.artillery.data.ExtendedInventory;
import happy.artillery.HappyArtilleryPlayerExtension;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements HappyArtilleryPlayerExtension {
    @Unique
    private final ExtendedInventory happy_artillery$extendedInventory = new ExtendedInventory();
    
    @Override
    public ExtendedInventory getExtendedInventory() {
        return happy_artillery$extendedInventory;
    }
}
