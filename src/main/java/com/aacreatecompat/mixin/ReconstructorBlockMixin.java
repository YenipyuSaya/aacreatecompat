package com.aacreatecompat.mixin;

import com.aacreatecompat.CreateModeHolder;
import de.ellpeck.actuallyadditions.api.lens.ILensItem;
import de.ellpeck.actuallyadditions.mod.blocks.BlockAtomicReconstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Refuse lens installation by hand while in Create Process mode. */
@Mixin(value = BlockAtomicReconstructor.class, remap = false)
public class ReconstructorBlockMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true, remap = true)
    private void aacc$noLens(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                             BlockHitResult hit, CallbackInfoReturnable<ItemInteractionResult> cir) {
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof ILensItem)) return;
        if (!(world.getBlockEntity(pos) instanceof CreateModeHolder h) || !h.aacc$isCreateMode()) return;
        if (!world.isClientSide) {
            player.displayClientMessage(Component.translatable("info.aacreatecompat.noLens").withStyle(ChatFormatting.RED), true);
        }
        cir.setReturnValue(ItemInteractionResult.sidedSuccess(world.isClientSide));
    }
}
