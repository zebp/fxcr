package dev.zebulon.fxcr;

import dev.zebulon.fxcr.mixin.MixinExtChunkBuilderChunkData;
import dev.zebulon.fxcr.mixin.MixinExtRenderLayer;
import net.minecraft.block.*;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;

import java.util.Random;
import java.util.Set;

public class RenderSubstitute {
    public static final FxcrBlockView BLOCK_VIEW = new FxcrBlockView();

    public static BlockState[] BLOCK_STATE_CACHE = new BlockState[64];

    public static final RenderLayer FXCR_LAYER = MixinExtRenderLayer.invokeOfFxcr("fxcr",
            VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 0x2000, true, false,
            RenderLayer.MultiPhaseParameters.builder()
                    // .shadeModel(new RenderPhase.ShadeModel(true))
                    .shader(new RenderPhase.Shader(GameRenderer::getRenderTypeSolidShader))
                    .lightmap(new RenderPhase.Lightmap(true)).cull(new RenderPhase.Cull(false))
                    .texture(new RenderPhase.Texture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, false, true)).build(true));

    private static final int TRAPPED_CHEST_FLAG = 1 << 5;

    public static void onRender(final BlockPos blockPos, final BlockState blockState,
            final BlockBufferBuilderStorage chunkRenderTask, final ChunkBuilder.ChunkData chunkData,
            final Random random, final MatrixStack matrixStack) {
        // FIXME: do we need this check? we only call this function on chests and
        // trapped chests
        if (RenderLayers.getBlockLayer(blockState) != RenderLayer.getSolid()) {
            return;
        }

        ((MixinExtChunkBuilderChunkData) chunkData).getNonEmptyLayers().add(FXCR_LAYER);

        final BlockRenderManager blockRendererDispatcher = MinecraftClient.getInstance().getBlockRenderManager();
        final BufferBuilder bufferBuilder = chunkRenderTask.get(FXCR_LAYER);

        final Set<RenderLayer> initializedBuffers = ((MixinExtChunkBuilderChunkData) chunkData).getInitializedLayers();

        if (!initializedBuffers.contains(FXCR_LAYER)) {
            initializedBuffers.add(FXCR_LAYER);
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
        }

        final int x = ChunkSectionPos.getLocalCoord(blockPos.getX());
        final int y = ChunkSectionPos.getLocalCoord(blockPos.getY());
        final int z = ChunkSectionPos.getLocalCoord(blockPos.getZ());

        // FIXME: should we push, translate, render, then pop?
        matrixStack.translate(x, y, z);
        blockRendererDispatcher.renderBlock(transformBlockState(blockState), blockPos, BLOCK_VIEW, matrixStack,
                bufferBuilder, true, random);
        matrixStack.translate(-x, -y, -z);
    }

    private static BlockState transformBlockState(BlockState chestState) {
        Block block = chestState.getBlock();

        Direction direction = chestState.get(Properties.HORIZONTAL_FACING);
        ChestType chestType = chestState.get(ChestBlock.CHEST_TYPE);

        int index = (direction.ordinal() << 2) | chestType.ordinal();

        // Add a trapped flag if it's a trapped chest.
        index |= block == Blocks.TRAPPED_CHEST ? TRAPPED_CHEST_FLAG : 0;

        BlockState cached = BLOCK_STATE_CACHE[index];

        if (cached == null) {
            if ((index & TRAPPED_CHEST_FLAG) == 0) {
                BLOCK_STATE_CACHE[index] = FxcrMod.FAST_CHEST_BLOCK.getDefaultState()
                        .with(HorizontalFacingBlock.FACING, direction).with(ChestBlock.CHEST_TYPE, chestType);
            } else {
                BLOCK_STATE_CACHE[index] = FxcrMod.FAST_TRAPPED_CHEST_BLOCK.getDefaultState()
                        .with(HorizontalFacingBlock.FACING, direction).with(ChestBlock.CHEST_TYPE, chestType);
            }

            cached = BLOCK_STATE_CACHE[index];
        }

        return cached;
    }
}
