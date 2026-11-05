package com.aacreatecompat.mixin;

import com.aacreatecompat.CompatLookup;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Same as the belt hook, for depots / weighted ejectors. */
@Mixin(value = DepotBehaviour.class, remap = false, priority = 1100)
public class DepotLookupCallSiteMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/blockEntity/behaviour/BlockEntityBehaviour;get(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lcom/simibubi/create/foundation/blockEntity/behaviour/BehaviourType;)Lcom/simibubi/create/foundation/blockEntity/behaviour/BlockEntityBehaviour;"))
    private BlockEntityBehaviour aacc$depot(BlockGetter reader, BlockPos pos, BehaviourType<?> type, Operation<BlockEntityBehaviour> original) {
        BlockEntityBehaviour r = original.call(reader, pos, type);
        if (r == null && type == BeltProcessingBehaviour.TYPE) r = CompatLookup.find(reader, pos);
        return r;
    }
}
