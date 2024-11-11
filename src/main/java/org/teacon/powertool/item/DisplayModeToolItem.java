package org.teacon.powertool.item;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.network.PacketDistributor;
import org.teacon.powertool.attachment.PowerToolAttachments;
import org.teacon.powertool.network.client.UpdateDisplayChunkDataPacket;
import org.teacon.powertool.utils.VanillaUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DisplayModeToolItem extends Item {
    public DisplayModeToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(
            Component.translatable("tooltip.powertool.display_tool")
                .withStyle(ChatFormatting.GRAY)
        );
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null || !player.getAbilities().instabuild) {
            return InteractionResult.FAIL;
        }
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockState blockState = level.getBlockState(pos);
        MenuProvider menuProvider = blockState.getMenuProvider(level, pos);
        Component blockName = VanillaUtils.getName(blockState.getBlock());
        if (menuProvider == null && !player.isShiftKeyDown()) {
            player.displayClientMessage(
                Component.translatable("powertool.gui.display_mode_error", blockName)
                    .withStyle(ChatFormatting.RED),
                true
            );
            return InteractionResult.SUCCESS;
        }
        ChunkPos chunkPos = new ChunkPos(pos);
        ChunkAccess chunk = level.getChunkAt(pos);
        List<BlockPos> displayEnabledPosList = new ArrayList<>(chunk.getData(PowerToolAttachments.DISPLAY_MODE));
        if (displayEnabledPosList.contains(pos)) {
            displayEnabledPosList.remove(pos);
            player.displayClientMessage(
                Component.translatable("powertool.gui.display_mode_disabled", blockName),
                true
            );
        } else {
            displayEnabledPosList.add(pos);
            player.displayClientMessage(
                Component.translatable("powertool.gui.display_mode_enabled", blockName),
                true
            );
        }
        chunk.setUnsaved(true);
        chunk.setData(PowerToolAttachments.DISPLAY_MODE, displayEnabledPosList);
        PacketDistributor.sendToPlayer(
            (ServerPlayer) player,
            new UpdateDisplayChunkDataPacket(
                chunkPos.x,
                chunkPos.z,
                displayEnabledPosList
            )
        );
        return InteractionResult.SUCCESS;
    }
}
