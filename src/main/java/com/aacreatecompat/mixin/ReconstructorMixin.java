package com.aacreatecompat.mixin;

import com.aacreatecompat.AACreateCompat;
import com.aacreatecompat.CompatConfig;
import com.aacreatecompat.CreateModeHolder;
import com.aacreatecompat.ReconstructorBeltProcessing;
import de.ellpeck.actuallyadditions.api.ActuallyAdditionsAPI;
import de.ellpeck.actuallyadditions.api.internal.IAtomicReconstructor;
import de.ellpeck.actuallyadditions.api.internal.IMethodHandler;
import de.ellpeck.actuallyadditions.mod.crafting.LaserRecipe;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityAtomicReconstructor;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.ItemStack;
import de.ellpeck.actuallyadditions.mod.util.ItemStackHandlerAA;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntityAtomicReconstructor.class, remap = false)
public abstract class ReconstructorMixin extends BlockEntity implements CreateModeHolder {
    private ReconstructorMixin() { super(null, null, null); }
    @Unique private static final String AACC_KEY = "aacreatecompat:CreateMode";
    @Unique private boolean aacc$createMode;
    @Unique private int aacc$cooldown;
    @Unique private long aacc$lastBeltWork = Long.MIN_VALUE / 2;
    @Unique private ReconstructorBeltProcessing aacc$processing;

    @Override public boolean aacc$isCreateMode() { return aacc$createMode; }
    @Override public void aacc$setCreateMode(boolean v) { aacc$createMode = v; }
    @Override public int aacc$getCooldown() { return aacc$cooldown; }
    @Override public void aacc$setCooldown(int v) { aacc$cooldown = v; }
    @Override public long aacc$getLastBeltWork() { return aacc$lastBeltWork; }
    @Override public void aacc$setLastBeltWork(long v) { aacc$lastBeltWork = v; }
    @Override public ReconstructorBeltProcessing aacc$getProcessing() {
        if (aacc$processing == null) aacc$processing = new ReconstructorBeltProcessing((TileEntityAtomicReconstructor) (Object) this);
        return aacc$processing;
    }

    @Inject(method = "writeSyncableNBT", at = @At("TAIL"))
    private void aacc$write(CompoundTag tag, HolderLookup.Provider p, TileEntityBase.NBTType type, CallbackInfo ci) {
        tag.putBoolean(AACC_KEY, aacc$createMode);
    }

    @Inject(method = "readSyncableNBT", at = @At("TAIL"))
    private void aacc$read(CompoundTag tag, HolderLookup.Provider p, TileEntityBase.NBTType type, CallbackInfo ci) {
        if (tag.contains(AACC_KEY)) aacc$createMode = tag.getBoolean(AACC_KEY);
    }

    @Inject(method = "applyImplicitComponents", at = @At("TAIL"), remap = true)
    private void aacc$applyComponents(BlockEntity.DataComponentInput input, CallbackInfo ci) {
        aacc$createMode = input.getOrDefault(AACreateCompat.CREATE_MODE.get(), false);
        if (aacc$createMode) ((TileEntityBase) (Object) this).isPulseMode = false;
    }

    @Inject(method = "collectImplicitComponents", at = @At("TAIL"), remap = true)
    private void aacc$collectComponents(DataComponentMap.Builder builder, CallbackInfo ci) {
        if (aacc$createMode) builder.set(AACreateCompat.CREATE_MODE.get(), true);
    }

    /** Disable AA's built-in 100-tick auto firing while in Create Process mode. */
    @Redirect(method = "serverTick", at = @At(value = "INVOKE",
            target = "Lde/ellpeck/actuallyadditions/api/internal/IMethodHandler;invokeReconstructor(Lde/ellpeck/actuallyadditions/api/internal/IAtomicReconstructor;)Z"))
    private static boolean aacc$autoFire(IMethodHandler handler, IAtomicReconstructor tile) {
        if (tile instanceof CreateModeHolder h && h.aacc$isCreateMode()) return false;
        return handler.invokeReconstructor(tile);
    }

    /** Cooldown countdown only. */
    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void aacc$tick(Level level, BlockPos pos, BlockState state, BlockEntity be, CallbackInfo ci) {
        if (!(be instanceof TileEntityAtomicReconstructor tile) || !(be instanceof CreateModeHolder h) || !h.aacc$isCreateMode()) return;
        if (h.aacc$getCooldown() > 0) h.aacc$setCooldown(h.aacc$getCooldown() - 1);
        // Create mode never fires on its own; dropped items are only hit as a side effect of belt/depot processing.
    }

    /** No lens may be inserted (hoppers / automation) while in Create Process mode. */
    @Inject(method = "getAcceptor", at = @At("RETURN"), cancellable = true)
    private void aacc$acceptor(CallbackInfoReturnable<ItemStackHandlerAA.IAcceptor> cir) {
        ItemStackHandlerAA.IAcceptor orig = cir.getReturnValue();
        cir.setReturnValue((slot, stack, automation) -> !aacc$createMode && orig.canAccept(slot, stack, automation));
    }
}
