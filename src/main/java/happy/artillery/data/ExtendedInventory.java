package happy.artillery.data;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages per-player extended inventory state (stored hotbar).
 * Simplified version without NBT persistence for 1.21.11 compatibility.
 */
public class ExtendedInventory {
    private List<ItemStack> storedHotbar = null;

    public boolean hasStoredHotbar() {
        return storedHotbar != null && !storedHotbar.isEmpty();
    }

    public void restoreHotbar(net.minecraft.inventory.Inventory inventory) {
        if (storedHotbar == null) return;
        for (int i = 0; i < 9 && i < storedHotbar.size(); i++) {
            inventory.setStack(i, storedHotbar.get(i).copy());
        }
        storedHotbar = null;
    }

    public NbtCompound toNbt(RegistryWrapper.WrapperLookup lookup) {
        // Simplified: just return empty NBT for now
        // Full ItemStack CODEC serialization requires more complex DynamicOps setup
        return new NbtCompound();
    }

    public void fromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        // Simplified: no-op for now
        // In-memory storage is sufficient; hotbar resets when player quits
    }
}
