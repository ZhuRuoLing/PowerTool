package org.teacon.powertool.client.renders;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.client.render.sections.BlockEntitySectionGeometryRenderer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import org.teacon.powertool.block.entity.BezierCurveBlockEntity;
import org.teacon.powertool.utils.VanillaUtils;
import org.teacon.powertool.utils.math.BezierCurve3f;
import org.teacon.powertool.utils.math.Line3f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BezierCurveBlockRenderer implements BlockEntityRenderer<BezierCurveBlockEntity>, BlockEntitySectionGeometryRenderer<BezierCurveBlockEntity> {
    
    public BezierCurveBlockRenderer(BlockEntityRendererProvider.Context ignore) {}
    
    @Override
    public void render(BezierCurveBlockEntity te, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
    
    
    }
    
    @Override
    public AABB getRenderBoundingBox(BezierCurveBlockEntity blockEntity) {
        return AABB.INFINITE;
    }
    
    @Override
    public void renderSectionGeometry(BezierCurveBlockEntity te, AddSectionGeometryEvent.SectionRenderingContext context, PoseStack poseStack, BlockPos pos, BlockPos regionOrigin, int packedLight, MultiBufferSource bufferSource) {
        poseStack = context.getPoseStack();
        poseStack.pushPose();
        //poseStack.scale(0.01f,0.1f,0.01f);
        var pose = poseStack.last();
        var buffer = bufferSource.getBuffer(RenderType.SOLID);
        var line = new BezierCurve3f(200,te.getControlPoints());
        var model = new Line3f(10,0.1,line.getPoints());
        for(var i = 0; i < model.vertexAndNormalQuadsList().size(); i++){
            var pair = model.vertexAndNormalQuadsList().get(i);
            var vertex = pair.getFirst();
            var normal = pair.getSecond();
            buffer.addVertex(pose,vertex.x, vertex.y, vertex.z).setUv(0,1).setUv2(1,0).setColor(VanillaUtils.getColor(200,200,i%240,255)).setNormal(pose,normal.x, normal.y, normal.z);
        }
        poseStack.popPose();
    }
}
