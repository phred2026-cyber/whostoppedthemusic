package happy.artillery.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

// Obsolete persistence hooks removed; keep empty mixin to avoid compile deletion issues
@Mixin(Entity.class)
public abstract class EntityMixin {
}
