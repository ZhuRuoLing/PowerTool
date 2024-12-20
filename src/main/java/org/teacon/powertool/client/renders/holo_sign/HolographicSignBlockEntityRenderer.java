/*
 * Parts of this Java source file are from GlowCase project, maintained by ModFest team,
 * licensed under CC0-1.0 per its repository.
 * You may find the original code at https://github.com/ModFest/glowcase
 */
package org.teacon.powertool.client.renders.holo_sign;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.teacon.powertool.block.entity.BaseHolographicSignBlockEntity;
import org.teacon.powertool.block.entity.CommonHolographicSignBlockEntity;
import org.teacon.powertool.utils.VanillaUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class HolographicSignBlockEntityRenderer implements BlockEntityRenderer<CommonHolographicSignBlockEntity> {
    private static final Vector3f SHADOW_OFFSET = new Vector3f(0.0F, 0.0F, -0.2F);
    private final BlockEntityRenderDispatcher dispatcher;
    private final Font font;

    public HolographicSignBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
        this.dispatcher = context.getBlockEntityRenderDispatcher();
    }

    @Override
    public void render(CommonHolographicSignBlockEntity theSign, float partialTick, PoseStack transform, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderInternal(theSign,transform,bufferSource,packedLight,theSign.yRotate,theSign.xRotate);
        if(theSign.bidirectional){
            renderInternal(theSign,transform,bufferSource,packedLight,(theSign.yRotate +180)%360,(360 - theSign.xRotate) % 360);
        }
    }
    
    public void renderInternal(CommonHolographicSignBlockEntity theSign, PoseStack transform, MultiBufferSource bufferSource, int packedLight, int yRotation,int xRotation){
        transform.pushPose();
        beforeRender(theSign,transform,dispatcher,yRotation,xRotation);
        //VanillaUtils.ClientHandler.renderAxis(bufferSource,transform);
        Matrix4f matrix4f = transform.last().pose();
        int bgColor = getBackgroundColor(theSign);
        var dropShadow = theSign.dropShadow;
        int yOffset = -theSign.renderedContents.size() / 2 * this.font.lineHeight;
        int fontColor = theSign.colorInARGB;
        int maxWidth = 0;
        for (var text : theSign.contents) {
            int w = this.font.width(text);
            if (w > maxWidth) {
                maxWidth = w;
            }
        }
        var align = theSign.align;
        for (var text : theSign.renderedContents) {
            if (text != null && !text.isEmpty()) {
                int xOffset = switch (align) {
                    case LEFT -> -maxWidth / 2;
                    case CENTER -> -this.font.width(text) / 2;
                    case RIGHT -> maxWidth / 2 - this.font.width(text);
                };
                renderText(font,text, xOffset, yOffset, fontColor, dropShadow, matrix4f, bufferSource, bgColor, packedLight);
            }
            yOffset += this.font.lineHeight + 2;
        }
        transform.popPose();
    }
    
    public static void beforeRender(BaseHolographicSignBlockEntity theSign, PoseStack transform, BlockEntityRenderDispatcher dispatcher,int yRotation,int xRotation){
        transform.translate(0.5, 0.5, 0.5);
        if(theSign.lock){
            transform.mulPose(Axis.YP.rotationDegrees(yRotation));
            transform.mulPose(Axis.XP.rotationDegrees(xRotation));
        }
        else {
            transform.mulPose(dispatcher.camera.rotation());
            transform.mulPose(Axis.YP.rotationDegrees(180));
        }
        transform.scale(-0.025F, -0.025F, 0.025F);
        transform.scale(theSign.scale, theSign.scale, 1);
        transform.translate(0.0, 0.0, theSign.zOffset*40);
    }
    
    public static int getBackgroundColor(BaseHolographicSignBlockEntity theSign) {
        int bgColor = VanillaUtils.TRANSPARENT;
        if(theSign.renderBackground) bgColor = 0x40000000;
        return bgColor;
    }
    
    @SuppressWarnings("DuplicatedCode")
    public static void renderText(Font font, Component component, float x, float y, int color, boolean dropShadow,
                                  Matrix4f matrix, MultiBufferSource buffer,
                                  int backgroundColor, int packedLightCoords){
        var text = component.getVisualOrderText();
        Matrix4f matrix4f = new Matrix4f(matrix);
        renderBackground(font,backgroundColor,packedLightCoords,x,y,font.width(component),dropShadow,matrix4f,buffer);
        if (dropShadow) {
            font.drawInBatch(text, x, y, color, true, matrix, buffer, Font.DisplayMode.NORMAL,VanillaUtils.TRANSPARENT , packedLightCoords);
            matrix4f.translate(SHADOW_OFFSET);
        }
        font.drawInBatch(text, x, y, color, false, matrix4f, buffer, Font.DisplayMode.NORMAL, VanillaUtils.TRANSPARENT, packedLightCoords);
    }
    
    @SuppressWarnings("DuplicatedCode")
    public static void renderText(Font font,String text,float x, float y, int color, boolean dropShadow,
                                  Matrix4f matrix, MultiBufferSource buffer,
                                  int backgroundColor, int packedLightCoords){
        Matrix4f matrix4f = new Matrix4f(matrix);
        renderBackground(font,backgroundColor,packedLightCoords,x,y,font.width(text),dropShadow,matrix4f,buffer);
        if (dropShadow) {
            font.drawInBatch(text, x, y, color, true, matrix, buffer, Font.DisplayMode.NORMAL, VanillaUtils.TRANSPARENT, packedLightCoords);
            matrix4f.translate(SHADOW_OFFSET);
        }
        font.drawInBatch(text, x, y, color, false, matrix4f, buffer, Font.DisplayMode.NORMAL, VanillaUtils.TRANSPARENT, packedLightCoords);
    }
    
    public static void renderBackground(Font font,int backgroundColor, int packedLightCoords,float x,float y,int length,boolean append,Matrix4f matrix, MultiBufferSource buffer){
        if (backgroundColor != 0) {
            float f = (float)(backgroundColor >> 24 & 0xFF) / 255.0F;
            float f1 = (float)(backgroundColor >> 16 & 0xFF) / 255.0F;
            float f2 = (float)(backgroundColor >> 8 & 0xFF) / 255.0F;
            float f3 = (float)(backgroundColor & 0xFF) / 255.0F;
            var bgEffect = new BakedGlyph.Effect(x - 1.0F, y + 9.0F, x + length + (append ? 1 : 0), y - 1.0F, 0.02F, f1, f2, f3, f);
            var bakedGlyph = font.getFontSet(Style.DEFAULT_FONT).whiteGlyph();
            var vertexConsumer = buffer.getBuffer(bakedGlyph.renderType(Font.DisplayMode.NORMAL));
            bakedGlyph.renderEffect(bgEffect,matrix,vertexConsumer,packedLightCoords);
        }
    }
    
}
