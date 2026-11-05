package com.aacreatecompat;

import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.logistics.depot.DepotBlock;
import de.ellpeck.actuallyadditions.mod.blocks.ActuallyBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Sneak-right-click a belt/depot top with a reconstructor: place it 2 above, facing down. */
public class PlacementHandler {
    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!player.isShiftKeyDown() || event.getFace() != Direction.UP) return;
        if (stack.isEmpty() || stack.getItem() != ActuallyBlocks.ATOMIC_RECONSTRUCTOR.getItem()) return;
        Level level = event.getLevel();
        BlockPos base = event.getPos();
        Block baseBlock = level.getBlockState(base).getBlock();
        if (!(baseBlock instanceof BeltBlock) && !(baseBlock instanceof DepotBlock)) return;
        BlockPos gap = base.above(), target = base.above(2);
        BlockState gapState = level.getBlockState(gap), targetState = level.getBlockState(target);
        if (!gapState.canBeReplaced() || !gapState.getCollisionShape(level, gap).isEmpty()) return;
        if (!targetState.canBeReplaced() || !player.mayUseItemAt(target, Direction.UP, stack) || !level.mayInteract(player, target)) return;
        Block block = ActuallyBlocks.ATOMIC_RECONSTRUCTOR.get();
        BlockState state = block.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.DOWN);
        if (!state.canSurvive(level, target) || !level.isUnobstructed(state, target, net.minecraft.world.phys.shapes.CollisionContext.of(player))) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide) return;
        if (!level.setBlock(target, state, Block.UPDATE_ALL_IMMEDIATE)) return;
        BlockEntity be = level.getBlockEntity(target);
        if (be != null) {
            be.applyComponentsFromItemStack(stack);
            be.setChanged();
        }
        block.setPlacedBy(level, target, state, player, stack);
        SoundType sound = state.getSoundType(level, target, player);
        level.playSound(null, target, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, target, GameEvent.Context.of(player, state));
        if (!player.getAbilities().instabuild) stack.shrink(1);
    }
}
