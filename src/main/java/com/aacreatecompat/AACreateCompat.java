package com.aacreatecompat;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(AACreateCompat.MODID)
public class AACreateCompat {
    public static final String MODID = "aacreatecompat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE, MODID);
    /** Item-form persistence of the "Create Process" mode (copied by our loot table override). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> CREATE_MODE =
            COMPONENTS.registerComponentType("create_mode", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public AACreateCompat(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, CompatConfig.SPEC);
        COMPONENTS.register(modBus);
        NeoForge.EVENT_BUS.register(PlacementHandler.class);
        if (Boolean.getBoolean("aacreatecompat.verifyMixins")) {
            for (String c : new String[]{"de.ellpeck.actuallyadditions.mod.tile.TileEntityAtomicReconstructor",
                    "de.ellpeck.actuallyadditions.mod.blocks.base.BlockContainerBase",
                    "com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour"}) {
                try { Class.forName(c); } catch (ClassNotFoundException e) { throw new RuntimeException(e); }
            }
            LOGGER.info("aacreatecompat: mixin targets loaded OK");
        }
    }
}
