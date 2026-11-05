package com.aacreatecompat.mixin;

import com.aacreatecompat.CreateModeHolder;
import de.ellpeck.actuallyadditions.mod.blocks.base.BlockContainerBase;
import de.ellpeck.actuallyadditions.mod.config.CommonConfig;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityAtomicReconstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Redstone torch cycle for the reconstructor: auto -> pulse -> Create Process -> auto. */
@Mixin(value = BlockContainerBase.class, remap = false)
public class BlockContainerBaseMixin {
    @Inject(method = "tryToggleRedstone", at = @At("HEAD"), cancellable = true)
    private void aacc$cycle(Level world, BlockPos pos, Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player.getMainHandItem().getItem() != CommonConfig.Other.redstoneConfigureItem) return;
        if (!(world.getBlockEntity(pos) instanceof TileEntityAtomicReconstructor tile) || !(tile instanceof CreateModeHolder h)) return;
        if (!world.isClientSide) {
            if (h.aacc$isCreateMode()) { h.aacc$setCreateMode(false); tile.isPulseMode = false; }
            else if (tile.isPulseMode) { tile.isPulseMode = false; h.aacc$setCreateMode(true); h.aacc$setCooldown(0);
                net.minecraft.world.item.ItemStack lens = tile.inv.getStackInSlot(0);
                if (!lens.isEmpty()) {
                    tile.inv.setStackInSlot(0, net.minecraft.world.item.ItemStack.EMPTY);
                    net.minecraft.world.level.block.Block.popResource(world, pos, lens.copy());
                }
            }
            else tile.isPulseMode = true;
            tile.setChanged();
            tile.sendUpdate();
            com.aacreatecompat.Debug.log("mode" + pos + System.nanoTime(), "server: reconstructor at {} mode now pulse={} create={}", pos, tile.isPulseMode, h.aacc$isCreateMode());
        }
        cir.setReturnValue(true);
    }
}
