package org.teacon.powertool.block.entity;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.ParserUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.teacon.powertool.block.PowerToolBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
public class RawJsonHolographicSignBlockEntity extends BaseHolographicSignBlockEntity{
    
    public List<String> content = new ArrayList<>();
    
    public List<Component> forFilter = new ArrayList<>();
    public List<Component> forRender = new ArrayList<>();
    
    public RawJsonHolographicSignBlockEntity( BlockPos pPos, BlockState pBlockState) {
        super(PowerToolBlocks.RAW_JSON_HOLOGRAPHIC_SIGN_BLOCK_ENTITY.get(), pPos, pBlockState);
    }
    
    @Override
    public void writeTo(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("contentSize",content.size());
        for(int i = 0; i<content.size(); i++){
            tag.putString("content_"+i,content.get(i));
        }
        tag.putInt("forRenderSize",forRender.size());
        for(int i = 0; i<forRender.size(); i++){
            tag.putString("forRender_"+i,Component.Serializer.toJson(forRender.get(i),registries));
        }
        super.writeTo(tag, registries);
    }
    
    @Override
    public void readFrom(CompoundTag tag, HolderLookup.Provider registries) {
        var contentSize = tag.contains("contentSize") ? tag.getInt("contentSize") : 0;
        content.clear();
        for(int i = 0; i < contentSize; i++){
            content.add(tag.getString("content_"+i));
        }
        
        var forRenderSize = tag.contains("forRenderSize") ? tag.getInt("forRenderSize") : 0;
        forRender.clear();
        for(var i = 0; i<forRenderSize; i++){
            forRender.add(Component.Serializer.fromJson(tag.getString("forRender_"+i),registries));
        }
        
        forFilter.clear();
        try {
            for(var ct : content){
                forFilter.add(ParserUtils.parseJson(registries,new StringReader(ct), ComponentSerialization.CODEC));
            }
        }catch (Exception ignore){
        }
        super.readFrom(tag, registries);
        
        if(tag.contains("content") || tag.contains("forRender")){
            if (tag.contains("content")) content.add(tag.getString("content"));
            if (tag.contains("forRender")) forRender.add(Component.Serializer.fromJson(tag.getString("forRender"),registries));
            
            this.setChanged();
            if (level != null) {
                var state = this.getBlockState();
                level.sendBlockUpdated(this.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
            }
        }
    }
    
    @Override
    public void filterMessage(ServerPlayer player) {
        this.forRender.clear();
        var taskList = new ArrayList<CompletableFuture<?>>();
        //不用processMessageBundle 因为没有处理后list size和顺序不变的保证
        for(var i = 0; i<forFilter.size();i++){
            var task = player.getTextFilter()
                    .processStreamMessage(forFilter.get(i).getString());
            int finalI = i;
            task.thenAccept(filtered -> {
                if (player.isTextFilteringEnabled()) {
                    this.forRender.add(finalI,Component.literal(filtered.filteredOrEmpty()).withStyle(forFilter.get(finalI).getStyle()));
                } else {
                    this.forRender.add(finalI,forFilter.get(finalI));
                }
                try {
                    this.forRender.add(finalI,ComponentUtils.updateForEntity(player.createCommandSourceStack(),forRender.remove(finalI),null,0));
                } catch (CommandSyntaxException ignored) {}
               
            });
            taskList.add(task);
            
        }
        var finalTask = CompletableFuture.allOf(taskList.toArray(new CompletableFuture<?>[0]));
        finalTask.thenAcceptAsync((_void) -> {
            this.setChanged();
            if (level != null) {
                var state = this.getBlockState();
                level.sendBlockUpdated(this.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
            }
        },player.server);
    }
}
