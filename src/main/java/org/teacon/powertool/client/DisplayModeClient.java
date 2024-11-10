package org.teacon.powertool.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisplayModeClient {
    public static final DisplayModeClient INSTANCE = new DisplayModeClient();
    private final Map<ChunkPos, List<BlockPos>> data = new HashMap<>();
    private BlockPos interactionSourcePos = null;

    public boolean isDisplayModeEnabledOn(Screen screen) {
        Player player = Minecraft.getInstance().player;
        if (player == null || interactionSourcePos == null || player.getAbilities().instabuild) {
            return false;
        }
        if (screen instanceof AbstractContainerScreen<? extends AbstractContainerMenu> abstractContainerScreen) {
            ChunkPos pos = new ChunkPos(interactionSourcePos);
            if (data.containsKey(pos)) {
                return data.get(pos).contains(interactionSourcePos);
            }
        }
        return false;
    }

    public void screenClosed() {
        interactionSourcePos = null;
    }

    public void clear() {
        data.clear();
    }

    public void updateInteractionSource(BlockPos pos) {
        this.interactionSourcePos = pos;
    }

    public void update(ChunkPos chunkPos, List<BlockPos> blockPosList) {
        data.put(chunkPos, blockPosList);
    }

    public boolean isDisplayModeEnabledAt(BlockPos blockPos) {
        ChunkPos pos = new ChunkPos(blockPos);
        if (data.containsKey(pos)) {
            return data.get(pos).contains(blockPos);
        }
        return false;
    }
}
