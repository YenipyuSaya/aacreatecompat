package com.aacreatecompat.goggles;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Isolated Curios API access. Only class-loaded when Curios is present.
 */
public final class CuriosGoggles {
    private CuriosGoggles() {}

    public static void init(IEventBus modBus) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(CuriosGoggles::onClientSetup);
        }
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(com.aacreatecompat.goggles.client.AaGogglesCurioRenderer::register);
    }

    public static boolean hasAaGoggles(Player player) {
        return anyCurio(player, GoggleCompat::isAaGoggles);
    }

    public static boolean hasInfraredGoggles(Player player) {
        return anyCurio(player, GoggleCompat::isInfraredGoggles);
    }

    public static ItemStack findInfraredGoggles(Player player) {
        return findCurio(player, GoggleCompat::isInfraredGoggles);
    }

    private static boolean anyCurio(LivingEntity entity, Predicate<ItemStack> test) {
        return !findCurio(entity, test).isEmpty();
    }

    private static ItemStack findCurio(LivingEntity entity, Predicate<ItemStack> test) {
        ICuriosItemHandler handler = entity.getCapability(CuriosCapability.INVENTORY);
        if (handler == null) {
            return ItemStack.EMPTY;
        }
        Map<String, ICurioStacksHandler> curios = handler.getCurios();
        if (curios == null || curios.isEmpty()) {
            return ItemStack.EMPTY;
        }
        // Prefer the head identifier, then scan remaining slots (items are tagged for head).
        ICurioStacksHandler head = curios.get("head");
        ItemStack found = findInHandler(head, test);
        if (!found.isEmpty()) {
            return found;
        }
        for (Map.Entry<String, ICurioStacksHandler> entry : curios.entrySet()) {
            if ("head".equals(entry.getKey())) {
                continue;
            }
            found = findInHandler(entry.getValue(), test);
            if (!found.isEmpty()) {
                return found;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack findInHandler(ICurioStacksHandler stacksHandler, Predicate<ItemStack> test) {
        if (stacksHandler == null) {
            return ItemStack.EMPTY;
        }
        int slots = stacksHandler.getSlots();
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = stacksHandler.getStacks().getStackInSlot(slot);
            if (test.test(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
