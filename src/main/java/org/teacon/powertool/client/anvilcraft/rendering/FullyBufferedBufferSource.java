package org.teacon.powertool.client.anvilcraft.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.minecraft.client.renderer.RenderStateShard.LIGHTMAP;
import static net.minecraft.client.renderer.RenderStateShard.RENDERTYPE_TRANSLUCENT_SHADER;
import static net.minecraft.client.renderer.RenderStateShard.TRANSLUCENT_TARGET;
import static net.minecraft.client.renderer.RenderStateShard.TRANSLUCENT_TRANSPARENCY;

/**
 * @author ZhuRuoLing
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FullyBufferedBufferSource extends MultiBufferSource.BufferSource implements AutoCloseable {
    public static final BiFunction<RenderStateShard.EmptyTextureStateShard, RenderStateShard.OutputStateShard, RenderType> FORCED_TRANSLUCENT =
        Util.memoize((textureStateShard, outputStateShard) -> RenderType.create(
                "translucent",
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                786432,
                true,
                true,
                RenderType.CompositeState.builder()
                    .setLightmapState(LIGHTMAP)
                    .setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
                    .setTextureState(textureStateShard)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(outputStateShard)
                    .createCompositeState(true)
            )
        );

    private final Map<RenderType, ByteBufferBuilder> byteBuffers = new HashMap<>();
    private final Map<RenderType, BufferBuilder> bufferBuilders = new HashMap<>();
    private final Reference2IntMap<RenderType> indexCountMap = new Reference2IntOpenHashMap<>();
    private final Map<RenderType, MeshData.SortState> meshSorts = new HashMap<>();
    private final Map<RenderType, RenderType> cachedRenderTypeConvertions = new HashMap<>();

    public FullyBufferedBufferSource() {
        super(null, null);
    }

    private ByteBufferBuilder getByteBuffer(RenderType renderType) {
        return byteBuffers.computeIfAbsent(renderType, it -> new ByteBufferBuilder(786432));
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return bufferBuilders.computeIfAbsent(
            forceUseBlockRenderTypes(renderType),
            it -> new BufferBuilder(
                getByteBuffer(forceUseBlockRenderTypes(renderType)),
                it.mode,
                it.format
            )
        );
    }

    private RenderType forceUseBlockRenderTypes(RenderType renderType) {
        if (renderType.format == DefaultVertexFormat.NEW_ENTITY && renderType instanceof RenderType.CompositeRenderType compositeRenderType) {
            return cachedRenderTypeConvertions.computeIfAbsent(
                renderType,
                it -> FORCED_TRANSLUCENT.apply(
                    compositeRenderType.state().textureState,
                    compositeRenderType.state().outputState
                )
            );
        }
        return renderType;
    }

    public boolean isEmpty() {
        return !bufferBuilders.isEmpty() && bufferBuilders.values().stream().noneMatch(it -> it.vertices > 0);
    }

    @Override
    public void endBatch(RenderType renderType) {
    }

    @Override
    public void endLastBatch() {
    }

    @Override
    public void endBatch() {
    }

    public void upload(
        Function<RenderType, VertexBuffer> vertexBufferGetter,
        Function<RenderType, ByteBufferBuilder> byteBufferSupplier,
        Consumer<Runnable> runner) {
        for (RenderType renderType : bufferBuilders.keySet()) {
            runner.accept(() -> {
                BufferBuilder bufferBuilder = bufferBuilders.get(renderType);
                ByteBufferBuilder byteBuffer = byteBuffers.get(renderType);
                int compiledVertices = bufferBuilder.vertices * renderType.format.getVertexSize();
                if (compiledVertices >= 0) {
                    MeshData mesh = bufferBuilder.build();
                    indexCountMap.put(renderType, renderType.mode.indexCount(bufferBuilder.vertices));
                    if (mesh != null) {
                        if (renderType.sortOnUpload) {
                            MeshData.SortState sortState = mesh.sortQuads(
                                byteBufferSupplier.apply(renderType),
                                RenderSystem.getVertexSorting()
                            );
                            meshSorts.put(
                                renderType,
                                sortState
                            );
                        }
                        VertexBuffer vertexBuffer = vertexBufferGetter.apply(renderType);
                        vertexBuffer.bind();
                        vertexBuffer.upload(mesh);
                        VertexBuffer.unbind();
                    }
                }
                byteBuffer.close();
                bufferBuilders.remove(renderType);
                byteBuffers.remove(renderType);
            });
        }
    }

    public void close(RenderType renderType) {
        ByteBufferBuilder builder = byteBuffers.get(renderType);
        builder.close();
    }

    public Reference2IntMap<RenderType> getIndexCountMap() {
        return indexCountMap;
    }

    public Map<RenderType, MeshData.SortState> getMeshSorts() {
        return meshSorts;
    }

    public void close() {
        byteBuffers.keySet().forEach(this::close);
    }
}
