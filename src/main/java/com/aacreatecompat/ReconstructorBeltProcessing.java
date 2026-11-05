package com.aacreatecompat;

import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import de.ellpeck.actuallyadditions.api.ActuallyAdditionsAPI;
import de.ellpeck.actuallyadditions.mod.crafting.LaserRecipe;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityAtomicReconstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A stand-in BeltProcessingBehaviour handed to Create's belts/depots when they look for a processing
 * block 2 above and find an Atomic Reconstructor in "Create Process" mode. Laser is instant.
 */
public class ReconstructorBeltProcessing extends BeltProcessingBehaviour {
    private final TileEntityAtomicReconstructor tile;
    private static boolean loggedHold, loggedFirst;

    public ReconstructorBeltProcessing(TileEntityAtomicReconstructor tile) {
        super(null);
        this.tile = tile;
    }

    /** Create mode, facing down, conversion lens (or none). Redstone is checked separately. */
    public static boolean isConfigured(TileEntityAtomicReconstructor tile) {
        if (!(tile instanceof CreateModeHolder h) || !h.aacc$isCreateMode() || tile.isRemoved() || tile.getLevel() == null) return false;
        return tile.getOrientation() == Direction.DOWN && tile.getLens() == ActuallyAdditionsAPI.lensDefaultConversion;
    }

    @Override
    public ProcessingResult handleReceivedItem(TransportedItemStack stack, TransportedItemStackHandlerBehaviour inventory) {
        return process(stack, inventory);
    }

    @Override
    public ProcessingResult handleHeldItem(TransportedItemStack stack, TransportedItemStackHandlerBehaviour inventory) {
        return process(stack, inventory);
    }

    private ProcessingResult process(TransportedItemStack transported, TransportedItemStackHandlerBehaviour inventory) {
        if (!isConfigured(tile)) return ProcessingResult.PASS;
        if (tile.isRedstonePowered) { Debug.log("rs" + tile.getBlockPos(), "reconstructor at {} releases/ignores {}: redstone signal", tile.getBlockPos(), transported.stack); return ProcessingResult.PASS; }
        Level level = tile.getLevel();
        Optional<RecipeHolder<LaserRecipe>> holder = LaserRecipe.getRecipeForStack(transported.stack);
        if (holder.isEmpty()) { Debug.log("norec" + tile.getBlockPos(), "reconstructor at {} passes {}: no laser recipe", tile.getBlockPos(), transported.stack); return ProcessingResult.PASS; }
        CreateModeHolder h = (CreateModeHolder) tile;
        if (level.isClientSide) return ProcessingResult.HOLD;
        h.aacc$setLastBeltWork(level.getGameTime());
        if (!loggedHold) { loggedHold = true; com.aacreatecompat.AACreateCompat.LOGGER.info("aacreatecompat: first Create Process hold at {} ({})", tile.getBlockPos(), transported.stack.getItem()); }
        Debug.log("hold" + tile.getBlockPos(), "reconstructor at {} holds {} (energy {}, cooldown {})", tile.getBlockPos(), transported.stack, tile.getEnergy(), h.aacc$getCooldown());
        if (h.aacc$getCooldown() > 0) return ProcessingResult.HOLD;

        LaserRecipe recipe = holder.get().value();
        ItemStack result = recipe.getResultItem(level.registryAccess());
        if (result.isEmpty()) return ProcessingResult.PASS;
        int perItem = Math.max(0, recipe.getEnergy());
        int available = tile.getEnergy();
        if (available < perItem) { Debug.log("nrg" + tile.getBlockPos(), "reconstructor at {} waiting for energy: has {}, needs {} per item", tile.getBlockPos(), available, perItem); return ProcessingResult.HOLD; }
        int n = Math.min(transported.stack.getCount(), CompatConfig.BATCH_SIZE.get());
        if (perItem > 0) n = Math.min(n, available / perItem);
        if (n <= 0) return ProcessingResult.HOLD;

        List<TransportedItemStack> outputs = new ArrayList<>();
        int total = result.getCount() * n;
        int max = Math.max(1, result.getMaxStackSize());
        while (total > 0) {
            int c = Math.min(total, max);
            total -= c;
            TransportedItemStack out = transported.copy();
            out.stack = result.copyWithCount(c);
            out.locked = false;
            out.clearFanProcessingData();
            outputs.add(out);
        }
        int left = transported.stack.getCount() - n;
        if (left > 0) {
            TransportedItemStack rest = transported.copy();
            rest.stack = transported.stack.copyWithCount(left);
            rest.locked = true;
            inventory.handleProcessingOnItem(transported, TransportedResult.convertToAndLeaveHeld(outputs, rest));
        } else {
            inventory.handleProcessingOnItem(transported, TransportedResult.convertTo(outputs));
        }

        tile.extractEnergy(perItem * n);
        h.aacc$setCooldown(CompatConfig.COOLDOWN.get());
        tile.setChanged();
        Debug.log("conv" + tile.getBlockPos(), "reconstructor at {} converted {} x{} -> {}, released", tile.getBlockPos(), transported.stack.getItem(), n, result);
        if (!loggedFirst) { loggedFirst = true; com.aacreatecompat.AACreateCompat.LOGGER.info("aacreatecompat: first Create Process conversion at {} ({} x{})", tile.getBlockPos(), transported.stack.getItem(), n); }
        if (level instanceof ServerLevel sl) {
            BlockPos target = tile.getBlockPos().below(2);
            convertDroppedInBeam(tile, sl, target);
            TileEntityAtomicReconstructor.shootLaser(tile, sl, tile.getX(), tile.getY(), tile.getZ(),
                    target.getX(), target.getY(), target.getZ(), tile.getLens());
        }
        return ProcessingResult.HOLD;
    }

    /** Side effect of a shot: convert dropped items inside the beam (column) or the 3x3 at the hit point. */
    static void convertDroppedInBeam(TileEntityAtomicReconstructor tile, ServerLevel level, BlockPos hitPos) {
        Direction dir = tile.getOrientation();
        BlockPos pos = tile.getBlockPos();
        net.minecraft.world.phys.AABB column = new net.minecraft.world.phys.AABB(pos.relative(dir)).minmax(new net.minecraft.world.phys.AABB(hitPos));
        net.minecraft.world.phys.AABB face = new net.minecraft.world.phys.AABB(hitPos).minmax(new net.minecraft.world.phys.AABB(hitPos.relative(dir.getOpposite())))
                .inflate(dir.getAxis() == Direction.Axis.X ? 0 : 1, dir.getAxis() == Direction.Axis.Y ? 0 : 1, dir.getAxis() == Direction.Axis.Z ? 0 : 1);
        for (net.minecraft.world.entity.item.ItemEntity item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, column.minmax(face),
                e -> column.intersects(e.getBoundingBox()) || face.intersects(e.getBoundingBox()))) {
            ItemStack stack = item.getItem();
            if (!item.isAlive() || stack.isEmpty() || item.getPersistentData().getBoolean("aa_cnv")) continue;
            Optional<RecipeHolder<LaserRecipe>> holder = LaserRecipe.getRecipeForStack(stack);
            if (holder.isEmpty()) continue;
            LaserRecipe recipe = holder.get().value();
            ItemStack result = recipe.getResultItem(level.registryAccess());
            if (result.isEmpty()) continue;
            int per = Math.max(0, recipe.getEnergy());
            int n = Math.min(stack.getCount(), CompatConfig.BATCH_SIZE.get());
            if (per > 0) n = Math.min(n, tile.getEnergy() / per);
            if (n <= 0) continue;
            item.discard();
            if (stack.getCount() > n) level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, item.getX(), item.getY(), item.getZ(), stack.copyWithCount(stack.getCount() - n)));
            int total = result.getCount() * n, max = Math.max(1, result.getMaxStackSize());
            while (total > 0) {
                int c = Math.min(total, max);
                total -= c;
                var out = new net.minecraft.world.entity.item.ItemEntity(level, item.getX(), item.getY(), item.getZ(), result.copyWithCount(c));
                out.getPersistentData().putBoolean("aa_cnv", true);
                level.addFreshEntity(out);
            }
            tile.extractEnergy(per * n);
        }
    }
}
