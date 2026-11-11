package com.aacreatecompat.goggles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.ellpeck.actuallyadditions.mod.items.ActuallyItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Renders AA engineer goggles on the Curios head slot using AA's own item model.
 * Does not use Create's goggle assets.
 */
@OnlyIn(Dist.CLIENT)
public class AaGogglesCurioRenderer implements ICurioRenderer {
    public static void register() {
        ICurioRenderer renderer = new AaGogglesCurioRenderer();
        CuriosRendererRegistry.register(ActuallyItems.ENGINEERS_GOGGLES.get(), () -> renderer);
        CuriosRendererRegistry.register(ActuallyItems.ENGINEERS_GOGGLES_ADVANCED.get(), () -> renderer);
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack,
            SlotContext slotContext,
            PoseStack poseStack,
            RenderLayerParent<T, M> renderLayerParent,
            MultiBufferSource buffer,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        poseStack.pushPose();
        if (renderLayerParent.getModel() instanceof HumanoidModel<?> humanoid) {
            humanoid.head.translateAndRotate(poseStack);
        }
        poseStack.translate(0.0, -0.25, 0.0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
        poseStack.scale(0.625f, 0.625f, 0.625f);
        if (!slotContext.entity().getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
            poseStack.translate(0.0, -0.25, 0.0);
        }
        Minecraft mc = Minecraft.getInstance();
        mc.getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.HEAD,
                light,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                mc.level,
                0);
        poseStack.popPose();
    }
}
