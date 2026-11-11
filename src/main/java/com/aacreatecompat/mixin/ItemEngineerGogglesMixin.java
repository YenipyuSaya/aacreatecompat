package com.aacreatecompat.mixin;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import de.ellpeck.actuallyadditions.mod.items.ItemEngineerGoggles;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AA laser beams, upgrade models, and LaserRelayHud all go through
 * {@link ItemEngineerGoggles#isWearing}. Keep the original IGoggles helmet
 * check, then accept any Create-recognized goggles (helmet or Curios).
 */
@Mixin(value = ItemEngineerGoggles.class, remap = false)
public class ItemEngineerGogglesMixin {
    @Inject(method = "isWearing", at = @At("RETURN"), cancellable = true)
    private static void aacc$orCreateGoggles(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && player != null && GogglesItem.isWearingGoggles(player)) {
            cir.setReturnValue(true);
        }
    }
}
