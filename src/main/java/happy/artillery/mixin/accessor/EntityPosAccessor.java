package happy.artillery.mixin.accessor;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityPosAccessor {
    // Yarn field for Entity position
    @Accessor("pos")
    Vec3d happy$getPos();
}
