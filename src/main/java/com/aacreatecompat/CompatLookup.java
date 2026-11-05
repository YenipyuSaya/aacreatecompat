package com.aacreatecompat;

import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import de.ellpeck.actuallyadditions.api.ActuallyAdditionsAPI;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityAtomicReconstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Finds a Create-mode reconstructor acting as a belt processing block at the queried position. */
public final class CompatLookup {
    public static BeltProcessingBehaviour find(BlockGetter reader, BlockPos pos) {
        if (reader == null || pos == null) return null;
        BlockEntity be;
        try { be = reader.getBlockEntity(pos); } catch (Throwable t) { return null; }
        if (!(be instanceof TileEntityAtomicReconstructor tile) || !(tile instanceof CreateModeHolder h)) return null;
        String side = tile.getLevel() != null && tile.getLevel().isClientSide ? "client" : "server";
        if (!h.aacc$isCreateMode()) { Debug.log("rej-mode" + pos, "{} reconstructor at {} queried by belt/depot but NOT in Create mode (pulse={})", side, pos, tile.isPulseMode); return null; }
        if (tile.isRemoved() || tile.getLevel() == null) { Debug.log("rej-lvl" + pos, "{} reconstructor at {} removed/no level", side, pos); return null; }
        if (tile.getLevel() != reader && Debug.on()) Debug.log("lvl" + pos, "{} note: query level {} differs from reconstructor level {}", side, reader, tile.getLevel());
        if (tile.getOrientation() != Direction.DOWN) { Debug.log("rej-face" + pos, "{} reconstructor at {} rejected: facing {} (must be DOWN)", side, pos, tile.getOrientation()); return null; }
        if (tile.getLens() != ActuallyAdditionsAPI.lensDefaultConversion) { Debug.log("rej-lens" + pos, "{} reconstructor at {} rejected: lens installed", side, pos); return null; }
        Debug.log("found" + pos, "{} belt/depot query found Create-mode reconstructor at {} (energy {}, redstone {})", side, pos, tile.getEnergy(), tile.isRedstonePowered);
        return h.aacc$getProcessing();
    }
}
