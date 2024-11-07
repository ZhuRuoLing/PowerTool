package org.teacon.powertool.client.renders.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;
import org.teacon.powertool.client.renders.entity.model.MartingEntityModel;
import org.teacon.powertool.entity.MartingEntity;

public class MartingEntityRenderer extends EntityRenderer<MartingEntity> {
    private MartingEntityModel<MartingEntity> model;

    public MartingEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new MartingEntityModel<>(context.bakeLayer(MartingEntityModel.LAYER));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull MartingEntity entity) {
        return entity.getVariant().getTexture();
    }

    @Override
    public void render(MartingEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        var buffer = bufferSource.getBuffer(model.renderType(getTextureLocation(entity)));
        model.renderToBuffer(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(255,255,255,255));
    }
}
