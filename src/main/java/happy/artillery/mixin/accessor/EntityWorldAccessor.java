package happy.artillery.mixin.accessor;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityWorldAccessor {
    // Yarn field for Entity's world
    @Accessor("world")
    World happy$getWorld();
}
