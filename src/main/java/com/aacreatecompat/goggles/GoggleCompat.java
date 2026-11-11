package com.aacreatecompat.goggles;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import de.ellpeck.actuallyadditions.api.misc.IGoggles;
import de.ellpeck.actuallyadditions.mod.items.ActuallyItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Cross-mod goggle wearing checks. Curios is optional and only touched
 * when the mod is present, so this class stays loadable without it.
 */
public final class GoggleCompat {
    private GoggleCompat() {}

    public static void register() {
        GogglesItem.addIsWearingPredicate(GoggleCompat::isWearingAaGoggles);
    }

    public static boolean isAaGoggles(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ActuallyItems.ENGINEERS_GOGGLES) || stack.is(ActuallyItems.ENGINEERS_GOGGLES_ADVANCED));
    }

    public static boolean isInfraredGoggles(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IGoggles goggles && goggles.displaySpectralMobs();
    }

    /** Helmet slot or Curios head (when Curios is loaded). */
    public static boolean isWearingAaGoggles(Player player) {
        if (player == null) {
            return false;
        }
        if (isAaGoggles(player.getItemBySlot(EquipmentSlot.HEAD))) {
            return true;
        }
        return curiosLoaded() && CuriosGoggles.hasAaGoggles(player);
    }

    /**
     * Infrared / spectral-mob vision: only AA infrared goggles, including
     * a Curios head slot. Create and Aviator goggles never qualify.
     */
    public static boolean isWearingInfraredGoggles(Player player) {
        if (player == null) {
            return false;
        }
        if (isInfraredGoggles(player.getItemBySlot(EquipmentSlot.HEAD))) {
            return true;
        }
        return curiosLoaded() && CuriosGoggles.hasInfraredGoggles(player);
    }

    /**
     * For AA's {@code renderEngineerEffect}: if the helmet is not infrared,
     * substitute a Curios infrared stack so the existing glow logic can run.
     */
    public static ItemStack spectralGogglesStack(Player player, ItemStack face) {
        if (isInfraredGoggles(face)) {
            return face;
        }
        if (player != null && curiosLoaded()) {
            ItemStack curios = CuriosGoggles.findInfraredGoggles(player);
            if (!curios.isEmpty()) {
                return curios;
            }
        }
        return face;
    }

    public static boolean curiosLoaded() {
        return ModList.get().isLoaded("curios");
    }
}
