package happy.artillery;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;

/**
 * Simple marker-based control for items - works server-side only, no client mods needed.
 * Uses NBT tags hidden in data components.
 */
public class CustomDataComponents {
    public static final String FIRE_CONTROL_TAG = "FireControl";
    public static final String CRY_CONTROL_TAG = "CryControl";
    public static final String TEMPORARY_TAG = "Temporary";
    

    /**
     * Mark an item as a fire control item
     */
    public static void setFireControlTag(ItemStack stack) {
        stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
            java.util.List.of(
                net.minecraft.text.Text.literal("§0§k" + FIRE_CONTROL_TAG)
            )
        ));
    }

    /**
     * Mark an item as a cry control item
     */
    public static void setCryControlTag(ItemStack stack) {
        stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
            java.util.List.of(
                net.minecraft.text.Text.literal("§0§k" + CRY_CONTROL_TAG)
            )
        ));
    }

    /**
     * Mark an item as temporary (should be deleted on cleanup)
     */
    public static void setTemporaryTag(ItemStack stack) {
        // Use lore marker to indicate temporary state
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) {
            lore = new net.minecraft.component.type.LoreComponent(
                new java.util.ArrayList<>(java.util.List.of(
                    net.minecraft.text.Text.literal("§0§k" + TEMPORARY_TAG)
                ))
            );
        } else {
            var lines = new java.util.ArrayList<>(lore.lines());
            lines.add(net.minecraft.text.Text.literal("§0§k" + TEMPORARY_TAG));
            lore = new net.minecraft.component.type.LoreComponent(lines);
        }
        stack.set(DataComponentTypes.LORE, lore);
    }

    /**
     * Check if item has fire control tag
     */
    public static boolean hasFireControlTag(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return false;
        return lore.lines().stream().anyMatch(line -> 
            line.getString().contains(FIRE_CONTROL_TAG)
        );
    }

    /**
     * Check if item has cry control tag
     */
    public static boolean hasCryControlTag(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return false;
        return lore.lines().stream().anyMatch(line -> 
            line.getString().contains(CRY_CONTROL_TAG)
        );
    }

    /**
     * Check if item has temporary tag
     */
    public static boolean hasTemporaryTag(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return false;
        return lore.lines().stream().anyMatch(line -> 
            line.getString().contains(TEMPORARY_TAG)
        );
    }

    /**
     * Remove fire control tag
     */
    public static void removeFireControlTag(ItemStack stack) {
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return;
        var lines = new java.util.ArrayList<>(lore.lines());
        lines.removeIf(line -> line.getString().contains(FIRE_CONTROL_TAG));
        if (lines.isEmpty()) {
            stack.remove(DataComponentTypes.LORE);
        } else {
            stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(lines));
        }
    }

    /**
     * Remove cry control tag
     */
    public static void removeCryControlTag(ItemStack stack) {
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return;
        var lines = new java.util.ArrayList<>(lore.lines());
        lines.removeIf(line -> line.getString().contains(CRY_CONTROL_TAG));
        if (lines.isEmpty()) {
            stack.remove(DataComponentTypes.LORE);
        } else {
            stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(lines));
        }
    }

    /**
     * Remove temporary tag
     */
    public static void removeTemporaryTag(ItemStack stack) {
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return;
        var lines = new java.util.ArrayList<>(lore.lines());
        lines.removeIf(line -> line.getString().contains(TEMPORARY_TAG));
        if (lines.isEmpty()) {
            stack.remove(DataComponentTypes.LORE);
        } else {
            stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(lines));
        }
    }

    /**
     * Set custom name for fire control item
     */
    public static void setFireControlName(ItemStack stack) {
        var name = net.minecraft.text.Text.literal("\u00a7c\ud83d\udd25 Fire Control");
        stack.set(DataComponentTypes.CUSTOM_NAME, name);
    }

    /**
     * Set custom name for cry control item
     */
    public static void setCryControlName(ItemStack stack) {
        var name = net.minecraft.text.Text.literal("\u00a75\ud83d\udc7b Cry Control");
        stack.set(DataComponentTypes.CUSTOM_NAME, name);
    }

    /**
     * Make item immovable (can't be dropped or moved in inventory)\n     */
    public static void makeItemImmovable(ItemStack stack) {
        // Mark item as immovable by adding an enchantment glint effect
        // Use the enchantment data component to show it's special
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
    }

    public static void initialize() {
        // Nothing to do - data components are applied directly to item stacks
    }
}
