package com.aacreatecompat.test;

import com.aacreatecompat.AACreateCompat;
import com.aacreatecompat.CreateModeHolder;
import com.aacreatecompat.goggles.GoggleCompat;
import de.ellpeck.actuallyadditions.mod.items.ItemEngineerGoggles;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import de.ellpeck.actuallyadditions.mod.blocks.ActuallyBlocks;
import de.ellpeck.actuallyadditions.mod.items.ActuallyItems;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityAtomicReconstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Dev-only GameTests (excluded from the jar). Run with ./gradlew runGameTestServer. */
@GameTestHolder(AACreateCompat.MODID)
@PrefixGameTestTemplate(false)
public class CompatGameTests {

    private static TileEntityAtomicReconstructor reconstructor(GameTestHelper h, BlockPos rel, int energy) {
        h.setBlock(rel, ActuallyBlocks.ATOMIC_RECONSTRUCTOR.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.DOWN));
        TileEntityAtomicReconstructor t = (TileEntityAtomicReconstructor) h.getBlockEntity(rel);
        ((CreateModeHolder) t).aacc$setCreateMode(true);
        t.isPulseMode = false;
        t.storage.setEnergyStored(energy);
        return t;
    }

    private static DepotBehaviour depot(GameTestHelper h, BlockPos rel, ItemStack stack) {
        h.setBlock(rel, AllBlocks.DEPOT.getDefaultState());
        DepotBehaviour d = BlockEntityBehaviour.get(h.getBlockEntity(rel), DepotBehaviour.TYPE);
        d.setHeldItem(new TransportedItemStack(stack));
        return d;
    }

    private static boolean locked(DepotBehaviour d) {
        try {
            var f = DepotBehaviour.class.getDeclaredField("heldItem");
            f.setAccessible(true);
            TransportedItemStack t = (TransportedItemStack) f.get(d);
            return t != null && t.locked;
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }

    private static ItemStack crystal() { return new ItemStack(ActuallyItems.DIAMATINE_CRYSTAL.get()); }

    /** Diamonds on a depot under a powered reconstructor get converted; energy = 60 x 5. */
    @GameTest(template = "empty7")
    public static void depotConverts(GameTestHelper h) {
        DepotBehaviour d = depot(h, new BlockPos(3, 1, 3), new ItemStack(Items.DIAMOND, 5));
        TileEntityAtomicReconstructor t = reconstructor(h, new BlockPos(3, 3, 3), 100000);
        h.succeedWhen(() -> {
            ItemStack held = d.getHeldItemStack();
            h.assertTrue(held.is(crystal().getItem()) && held.getCount() == 5, "depot holds " + held);
            h.assertTrue(t.getEnergy() == 100000 - 300, "energy " + t.getEnergy());
        });
    }

    /** No energy: item is held (locked) and unchanged. */
    @GameTest(template = "empty7")
    public static void depotHoldsWithoutEnergy(GameTestHelper h) {
        DepotBehaviour d = depot(h, new BlockPos(3, 1, 3), new ItemStack(Items.DIAMOND, 3));
        reconstructor(h, new BlockPos(3, 3, 3), 0);
        h.runAfterDelay(30, () -> {
            h.assertTrue(d.getHeldItemStack().is(Items.DIAMOND), "changed: " + d.getHeldItemStack());
            h.assertTrue(locked(d), "item not held/locked");
            h.succeed();
        });
    }

    /** Batch size 64 default + cooldown: 100 diamonds -> 64 now, rest later. Uses non-recipe pass check too. */
    @GameTest(template = "empty7")
    public static void depotPassesNonRecipe(GameTestHelper h) {
        DepotBehaviour d = depot(h, new BlockPos(3, 1, 3), new ItemStack(Items.DIRT, 3));
        TileEntityAtomicReconstructor t = reconstructor(h, new BlockPos(3, 3, 3), 100000);
        h.runAfterDelay(20, () -> {
            h.assertTrue(d.getHeldItemStack().is(Items.DIRT) && !locked(d), "dirt was held");
            h.assertTrue(t.getEnergy() == 100000, "energy used");
            h.succeed();
        });
    }

    /** Redstone signal: held item is released and not processed. */
    @GameTest(template = "empty7")
    public static void depotRedstoneReleases(GameTestHelper h) {
        DepotBehaviour d = depot(h, new BlockPos(3, 1, 3), new ItemStack(Items.DIAMOND, 2));
        TileEntityAtomicReconstructor t = reconstructor(h, new BlockPos(3, 3, 3), 0);
        h.runAfterDelay(10, () -> {
            h.assertTrue(locked(d), "not held before redstone");
            h.setBlock(new BlockPos(4, 3, 3), Blocks.REDSTONE_BLOCK);
            t.storage.setEnergyStored(100000);
        });
        h.runAfterDelay(25, () -> {
            h.assertTrue(d.getHeldItemStack().is(Items.DIAMOND), "processed despite redstone");
            h.assertTrue(!locked(d), "still locked under redstone");
            h.succeed();
        });
    }


    // ---------- belts in every direction, reconstructor over middle or last segment ----------
    private static void beltCase(GameTestHelper h, Direction d, boolean atEnd, int energy, boolean expectConvert) {
        BlockPos c = new BlockPos(3, 1, 3);
        BlockPos start = c.relative(d.getOpposite(), 2), end = c.relative(d, 2);
        Direction.Axis shaftAxis = d.getClockWise().getAxis();
        h.setBlock(start, AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, shaftAxis));
        h.setBlock(end, AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, shaftAxis));
        BeltConnectorItem.createBelts(h.getLevel(), h.absolutePos(start), h.absolutePos(end));
        Direction side = d.getClockWise();
        h.setBlock(start.relative(side), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, side.getOpposite()));
        TileEntityAtomicReconstructor[] t = {null};
        boolean[] sawHeld = {false};
        h.onEachTick(() -> {
            BeltBlockEntity ctrl = BeltHelper.getControllerBE(h.getLevel(), h.absolutePos(start));
            if (ctrl == null) return;
            if (t[0] == null && ctrl.getSpeed() != 0) {
                boolean pos = ctrl.getDirectionAwareBeltMovementSpeed() > 0;
                int idx = atEnd ? (pos ? ctrl.beltLength - 1 : 0) : ctrl.beltLength / 2;
                BlockPos rp = BeltHelper.getPositionForOffset(ctrl, idx).above(2);
                h.getLevel().setBlock(rp, ActuallyBlocks.ATOMIC_RECONSTRUCTOR.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.DOWN), 3);
                t[0] = (TileEntityAtomicReconstructor) h.getLevel().getBlockEntity(rp);
                ((CreateModeHolder) t[0]).aacc$setCreateMode(true);
                t[0].storage.setEnergyStored(energy);
                TransportedItemStack ts = new TransportedItemStack(new ItemStack(Items.DIAMOND, 4));
                ts.beltPosition = pos ? 0.5f : ctrl.beltLength - 0.5f;
                ctrl.getInventory().addItem(ts);
            }
            for (TransportedItemStack ts : ctrl.getInventory().getTransportedItems())
                if (ts.locked && ts.stack.is(Items.DIAMOND)) sawHeld[0] = true;
        });
        if (expectConvert) {
            h.succeedWhen(() -> {
                h.assertTrue(t[0] != null, "belt never moved");
                h.assertTrue(sawHeld[0], "diamond never held");
                h.assertTrue(t[0].getEnergy() == energy - 240, "energy " + t[0].getEnergy());
            });
        } else {
            h.runAfterDelay(150, () -> {
                h.assertTrue(t[0] != null, "belt never moved");
                BeltBlockEntity ctrl = BeltHelper.getControllerBE(h.getLevel(), h.absolutePos(start));
                boolean heldNow = ctrl.getInventory().getTransportedItems().stream().anyMatch(s -> s.locked && s.stack.is(Items.DIAMOND));
                h.assertTrue(heldNow, "diamond not held with 0 FE");
                h.succeed();
            });
        }
    }

    @GameTest(template = "empty7", timeoutTicks = 400) public static void beltNorthMid(GameTestHelper h) { beltCase(h, Direction.NORTH, false, 100000, true); }
    @GameTest(template = "empty7", timeoutTicks = 400) public static void beltSouthMid(GameTestHelper h) { beltCase(h, Direction.SOUTH, false, 100000, true); }
    @GameTest(template = "empty7", timeoutTicks = 400) public static void beltEastMid(GameTestHelper h) { beltCase(h, Direction.EAST, false, 100000, true); }
    @GameTest(template = "empty7", timeoutTicks = 400) public static void beltWestMid(GameTestHelper h) { beltCase(h, Direction.WEST, false, 100000, true); }
    @GameTest(template = "empty7", timeoutTicks = 400) public static void beltNorthEnd(GameTestHelper h) { beltCase(h, Direction.NORTH, true, 100000, true); }
    @GameTest(template = "empty7", timeoutTicks = 400) public static void beltSouthEnd(GameTestHelper h) { beltCase(h, Direction.SOUTH, true, 100000, true); }
    @GameTest(template = "empty7", timeoutTicks = 400) public static void beltEastEnd(GameTestHelper h) { beltCase(h, Direction.EAST, true, 100000, true); }
    @GameTest(template = "empty7", timeoutTicks = 400) public static void beltWestEnd(GameTestHelper h) { beltCase(h, Direction.WEST, true, 100000, true); }
    @GameTest(template = "empty7", timeoutTicks = 400) public static void beltHoldsAtZeroEnergy(GameTestHelper h) { beltCase(h, Direction.EAST, false, 0, false); }

    // ---------- the real player path: torch right-clicks, then sneak placement ----------
    @GameTest(template = "empty7", timeoutTicks = 100)
    public static void playerTogglePath(GameTestHelper h) {
        net.minecraft.world.entity.player.Player p = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos depotRel = new BlockPos(3, 1, 3);
        DepotBehaviour d = depot(h, depotRel, new ItemStack(Items.DIAMOND, 2));
        // sneak-place the reconstructor on the depot top
        p.setShiftKeyDown(true);
        ItemStack recon = new ItemStack(ActuallyBlocks.ATOMIC_RECONSTRUCTOR.getItem());
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, recon);
        BlockPos depotAbs = h.absolutePos(depotRel);
        var ev = net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p, net.minecraft.world.InteractionHand.MAIN_HAND, depotAbs,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(depotAbs).add(0, 0.4, 0), Direction.UP, depotAbs, false)));
        h.assertTrue(ev.isCanceled(), "sneak placement event not handled");
        h.assertBlockPresent(ActuallyBlocks.ATOMIC_RECONSTRUCTOR.get(), new BlockPos(3, 3, 3));
        h.assertTrue(h.getBlockState(new BlockPos(3, 3, 3)).getValue(BlockStateProperties.FACING) == Direction.DOWN, "not facing down");
        TileEntityAtomicReconstructor t = (TileEntityAtomicReconstructor) h.getBlockEntity(new BlockPos(3, 3, 3));
        t.storage.setEnergyStored(100000);
        // two torch clicks: auto -> pulse -> create
        p.setShiftKeyDown(false);
        ItemStack torch = new ItemStack(de.ellpeck.actuallyadditions.mod.config.CommonConfig.Other.redstoneConfigureItem);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, torch);
        BlockPos rAbs = h.absolutePos(new BlockPos(3, 3, 3));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(rAbs).add(0.5, 0, 0), Direction.EAST, rAbs, false);
        h.getBlockState(new BlockPos(3, 3, 3)).useItemOn(torch, h.getLevel(), p, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        h.assertTrue(t.isPulseMode && !((CreateModeHolder) t).aacc$isCreateMode(), "1st click should be pulse");
        h.getBlockState(new BlockPos(3, 3, 3)).useItemOn(torch, h.getLevel(), p, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        h.assertTrue(!t.isPulseMode && ((CreateModeHolder) t).aacc$isCreateMode(), "2nd click should be create");
        h.succeedWhen(() -> h.assertTrue(d.getHeldItemStack().is(crystal().getItem()), "not converted: " + d.getHeldItemStack()));
    }

    /** Redstone torch merely standing next to the machine: AA counts it as powered (vanilla neighbour signal). */
    @GameTest(template = "empty7", timeoutTicks = 100)
    public static void adjacentTorchCountsAsPowered(GameTestHelper h) {
        depot(h, new BlockPos(3, 1, 3), new ItemStack(Items.DIAMOND, 1));
        TileEntityAtomicReconstructor t = reconstructor(h, new BlockPos(3, 3, 3), 100000);
        h.setBlock(new BlockPos(4, 3, 3), Blocks.REDSTONE_TORCH);
        h.runAfterDelay(20, () -> {
            com.aacreatecompat.AACreateCompat.LOGGER.info("[aacc-test] adjacent torch -> isRedstonePowered={}", t.isRedstonePowered);
            h.succeed();
        });
    }

    /** No belt/depot item: a dropped diamond under the beam is NOT processed (machine never fires on its own). */
    @GameTest(template = "empty7", timeoutTicks = 100)
    public static void droppedIgnoredAlone(GameTestHelper h) {
        TileEntityAtomicReconstructor t = reconstructor(h, new BlockPos(3, 4, 3), 100000);
        ItemEntity in = h.spawnItem(Items.DIAMOND, 3.5f, 1.2f, 3.5f);
        in.setPickUpDelay(32767);
        h.runAfterDelay(60, () -> {
            h.assertTrue(in.isAlive() && in.getItem().is(Items.DIAMOND), "dropped item converted on its own");
            h.assertTrue(t.getEnergy() == 100000, "energy used");
            h.succeed();
        });
    }

    /** Depot item triggers the shot; a dropped diamond in the 3x3 at the hit point converts too, one far away does not. */
    @GameTest(template = "empty7", timeoutTicks = 100)
    public static void droppedSideEffect(GameTestHelper h) {
        h.setBlock(new BlockPos(4, 1, 3), Blocks.STONE);
        ItemEntity near = h.spawnItem(Items.DIAMOND, 4.5f, 2.1f, 3.5f);
        ItemEntity far = h.spawnItem(Items.DIAMOND, 0.5f, 1.2f, 0.5f);
        near.setPickUpDelay(32767); far.setPickUpDelay(32767);
        DepotBehaviour d = depot(h, new BlockPos(3, 1, 3), new ItemStack(Items.DIAMOND, 1));
        reconstructor(h, new BlockPos(3, 3, 3), 100000);
        h.succeedWhen(() -> {
            h.assertTrue(d.getHeldItemStack().is(crystal().getItem()), "depot not converted");
            h.assertTrue(!near.isAlive(), "near dropped item not converted");
            h.assertTrue(far.isAlive() && far.getItem().is(Items.DIAMOND), "far item touched");
        });
    }

    // ---------- goggle cross-compat (helmet / Create overlay / AA laser wearing) ----------
    private static net.minecraft.world.entity.player.Player helmeted(GameTestHelper h, ItemStack helmet) {
        net.minecraft.world.entity.player.Player p = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        p.setItemSlot(EquipmentSlot.HEAD, helmet);
        return p;
    }

    @GameTest(template = "empty7")
    public static void gogglesAaHelmetGivesCreateOverlay(GameTestHelper h) {
        var regular = helmeted(h, new ItemStack(ActuallyItems.ENGINEERS_GOGGLES.get()));
        var infrared = helmeted(h, new ItemStack(ActuallyItems.ENGINEERS_GOGGLES_ADVANCED.get()));
        h.assertTrue(GogglesItem.isWearingGoggles(regular), "regular AA goggles should count as Create goggles");
        h.assertTrue(GogglesItem.isWearingGoggles(infrared), "infrared AA goggles should count as Create goggles");
        h.assertTrue(GoggleCompat.isWearingInfraredGoggles(infrared), "infrared helmet should grant spectral vision");
        h.assertTrue(!GoggleCompat.isWearingInfraredGoggles(regular), "regular AA goggles should not grant spectral vision");
        h.succeed();
    }

    @GameTest(template = "empty7")
    public static void gogglesCreateHelmetSeesAaLasers(GameTestHelper h) {
        var create = helmeted(h, new ItemStack(AllItems.GOGGLES.get()));
        var empty = helmeted(h, ItemStack.EMPTY);
        h.assertTrue(ItemEngineerGoggles.isWearing(create), "Create goggles should pass AA isWearing (lasers/HUD)");
        h.assertTrue(GogglesItem.isWearingGoggles(create), "Create helmet should still be wearing Create goggles");
        h.assertTrue(!GoggleCompat.isWearingInfraredGoggles(create), "Create goggles must not grant AA infrared glow");
        h.assertTrue(!ItemEngineerGoggles.isWearing(empty), "empty helmet should not be wearing AA/Create goggles");
        h.assertTrue(!GogglesItem.isWearingGoggles(empty), "empty helmet should not be wearing Create goggles");
        h.succeed();
    }

    @GameTest(template = "empty7")
    public static void gogglesAaTaggedForCuriosHead(GameTestHelper h) {
        var tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", "head"));
        h.assertTrue(new ItemStack(ActuallyItems.ENGINEERS_GOGGLES.get()).is(tag), "regular AA goggles missing from curios:head");
        h.assertTrue(new ItemStack(ActuallyItems.ENGINEERS_GOGGLES_ADVANCED.get()).is(tag), "infrared AA goggles missing from curios:head");
        h.succeed();
    }
}
