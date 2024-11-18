package org.teacon.powertool.client.renders.entity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;
import org.teacon.powertool.client.renders.entity.model.MartingEntityModel;
import org.teacon.powertool.entity.MartingEntity;

import java.util.Arrays;
import java.util.Map;

public class MartingEntityRenderer extends EntityRenderer<MartingEntity> {

    private final Map<MartingEntity.Variant, MartingEntityModel<MartingEntity>> variantToModel;

    public MartingEntityRenderer(EntityRendererProvider.Context context) {
        super(context);

        this.variantToModel = Arrays.stream(MartingEntity.Variant.values())
                .collect(
                        ImmutableMap.toImmutableMap(
                                v -> v,
                                v -> createModel(context, v))
                );
    }

    private MartingEntityModel<MartingEntity> createModel(EntityRendererProvider.Context context, MartingEntity.Variant variant) {
        return new MartingEntityModel<>(context.bakeLayer(variant.getModelLayer()));
    }

    private MartingEntityModel<MartingEntity> getBuffer(MartingEntity entity) {
        var v = entity.getVariant();
        return variantToModel.get(v);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull MartingEntity entity) {
        return entity.getVariant().getTexture();
    }

    @Override
    public void render(@NotNull MartingEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        var model = getBuffer(entity);
        var buffer = bufferSource.getBuffer(model.renderType(getTextureLocation(entity)));

        poseStack.pushPose();
        poseStack.translate(0, 1.5, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.scale(1, -1, 1);

        model.renderToBuffer(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(255,255,255,255));

        poseStack.popPose();
    }
}
