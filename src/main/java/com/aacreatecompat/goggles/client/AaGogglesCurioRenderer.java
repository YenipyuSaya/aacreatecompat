package com.aacreatecompat.goggles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.ellpeck.actuallyadditions.mod.items.ActuallyItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Renders AA engineer goggles worn in the Curios head slot exactly like vanilla's
 * {@code HumanoidArmorLayer} renders them in the helmet slot: AA's own armor material
 * layers/textures (textures/models/armor/goggles_layer_1.png) on the outer-armor
 * {@link HumanoidModel} head, posed from the parent player model (head rotation, sneaking,
 * baby scaling all come from {@link HumanoidModel#copyPropertiesTo}).
 * Uses only AA's assets; no Create assets.
 */
@OnlyIn(Dist.CLIENT)
public class AaGogglesCurioRenderer implements ICurioRenderer {
    /** Slightly inflated head so a real helmet worn at the same time does not z-fight. */
    private static final CubeDeformation OVER_HELMET = new CubeDeformation(1.1f);

    private HumanoidModel<LivingEntity> outerModel;
    private HumanoidModel<LivingEntity> overHelmetModel;

    public static void register() {
        ICurioRenderer renderer = new AaGogglesCurioRenderer();
        CuriosRendererRegistry.register(ActuallyItems.ENGINEERS_GOGGLES.get(), () -> renderer);
        CuriosRendererRegistry.register(ActuallyItems.ENGINEERS_GOGGLES_ADVANCED.get(), () -> renderer);
    }

    private HumanoidModel<LivingEntity> model(boolean overHelmet) {
        if (overHelmet) {
            if (overHelmetModel == null) {
                overHelmetModel = new HumanoidModel<>(
                        LayerDefinition.create(HumanoidModel.createMesh(OVER_HELMET, 0.0f), 64, 32).bakeRoot());
            }
            return overHelmetModel;
        }
        if (outerModel == null) {
            // Same baked layer the player renderer's HumanoidArmorLayer uses for helmets.
            outerModel = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
        }
        return outerModel;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
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
        if (!(stack.getItem() instanceof ArmorItem armorItem)) {
            return;
        }
        LivingEntity entity = slotContext.entity();
        boolean helmetWorn = !entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
        HumanoidModel<LivingEntity> armorModel = model(helmetWorn);

        M parent = renderLayerParent.getModel();
        if (parent instanceof HumanoidModel humanoid) {
            // Mirrors HumanoidArmorLayer#renderArmorPiece: pose + sneaking + young from the player model.
            humanoid.copyPropertiesTo(armorModel);
        } else {
            armorModel.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            ICurioRenderer.followHeadRotations(entity, armorModel.head, armorModel.hat);
        }
        armorModel.setAllVisible(false);
        armorModel.head.visible = true;
        armorModel.hat.visible = true;

        Model model = ClientHooks.getArmorModel(entity, stack, EquipmentSlot.HEAD, armorModel);
        IClientItemExtensions extensions = IClientItemExtensions.of(stack);
        extensions.setupModelAnimations(entity, stack, EquipmentSlot.HEAD, model,
                limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
        int fallbackColor = extensions.getDefaultDyeColor(stack);
        ArmorMaterial material = armorItem.getMaterial().value();
        for (int i = 0; i < material.layers().size(); i++) {
            ArmorMaterial.Layer layer = material.layers().get(i);
            int color = extensions.getArmorLayerTintColor(stack, entity, layer, i, fallbackColor);
            if (color != 0) {
                ResourceLocation texture = ClientHooks.getArmorTexture(entity, stack, layer, false, EquipmentSlot.HEAD);
                VertexConsumer vc = buffer.getBuffer(RenderType.armorCutoutNoCull(texture));
                model.renderToBuffer(poseStack, vc, light, OverlayTexture.NO_OVERLAY, color);
            }
        }
        if (stack.hasFoil()) {
            model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.armorEntityGlint()), light, OverlayTexture.NO_OVERLAY);
        }
    }
}
