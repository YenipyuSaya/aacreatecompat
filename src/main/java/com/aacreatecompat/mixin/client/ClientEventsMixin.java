package com.aacreatecompat.mixin.client;

import com.aacreatecompat.CreateModeHolder;
import com.aacreatecompat.goggles.GoggleCompat;
import de.ellpeck.actuallyadditions.mod.event.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Show "Create Process" in AA's hover overlay instead of "deactivation". */
@Mixin(value = ClientEvents.class, remap = false)
public class ClientEventsMixin {
    @ModifyArg(method = "onGameOverlay", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;", remap = true))
    private String aacc$modeKey(String key) {
        if (!"info.actuallyadditions.redstoneMode.deactivation".equals(key)) return key;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.hitResult instanceof BlockHitResult hit
                && mc.level.getBlockEntity(hit.getBlockPos()) instanceof CreateModeHolder h && h.aacc$isCreateMode()) {
            return "info.aacreatecompat.redstoneMode.create";
        }
        return key;
    }

    /**
     * Infrared entity glow stays exclusive to AA infrared goggles, but also
     * counts a pair worn in the Curios head slot. Create/Aviator goggles
     * are not substituted here.
     */
    @ModifyVariable(method = "renderEngineerEffect", at = @At("STORE"), ordinal = 0)
    private ItemStack aacc$includeCuriosInfrared(ItemStack face, Player player) {
        return GoggleCompat.spectralGogglesStack(player, face);
    }
}
