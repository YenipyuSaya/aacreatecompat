package com.aacreatecompat.mixin;

import com.aacreatecompat.CreateModeHolder;
import com.aacreatecompat.ReconstructorBeltProcessing;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityAtomicReconstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Belts and depots look up the processing block 2 above via this method; answer for reconstructors. */
@Mixin(value = BlockEntityBehaviour.class, remap = false)
public class BlockEntityBehaviourMixin {
    @Inject(method = "get(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lcom/simibubi/create/foundation/blockEntity/behaviour/BehaviourType;)Lcom/simibubi/create/foundation/blockEntity/behaviour/BlockEntityBehaviour;",
            at = @At("RETURN"), cancellable = true)
    private static <T extends BlockEntityBehaviour> void aacc$reconstructor(BlockGetter reader, BlockPos pos, BehaviourType<T> type, CallbackInfoReturnable<T> cir) {
        if (cir.getReturnValue() != null || type != BeltProcessingBehaviour.TYPE) return;
        BeltProcessingBehaviour b = com.aacreatecompat.CompatLookup.find(reader, pos);
        if (b != null) { @SuppressWarnings("unchecked") T t = (T) b; cir.setReturnValue(t); }
    }
}
