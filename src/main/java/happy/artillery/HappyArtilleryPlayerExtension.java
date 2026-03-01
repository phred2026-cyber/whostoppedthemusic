package happy.artillery;

import happy.artillery.data.ExtendedInventory;

/**
 * Interface injected into ServerPlayerEntity via mixin.
 */
public interface HappyArtilleryPlayerExtension {
    ExtendedInventory getExtendedInventory();
}
