package com.aacreatecompat.mixin;

import com.aacreatecompat.CompatLookup;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hook at the belt's own call site (after BlockEntityBehaviour.get returns), so mods that cancel
 * BlockEntityBehaviour.get at HEAD (e.g. Sable's sub-level compat) cannot bypass us.
 */
@Mixin(value = BeltInventory.class, remap = false, priority = 1100)
public class BeltLookupCallSitesMixin {
    @WrapOperation(method = "getBeltProcessingAtSegment", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/blockEntity/behaviour/BlockEntityBehaviour;get(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lcom/simibubi/create/foundation/blockEntity/behaviour/BehaviourType;)Lcom/simibubi/create/foundation/blockEntity/behaviour/BlockEntityBehaviour;"))
    private BlockEntityBehaviour aacc$belt(BlockGetter reader, BlockPos pos, BehaviourType<?> type, Operation<BlockEntityBehaviour> original) {
        BlockEntityBehaviour r = original.call(reader, pos, type);
        if (r == null && type == BeltProcessingBehaviour.TYPE) r = CompatLookup.find(reader, pos);
        return r;
    }
}
