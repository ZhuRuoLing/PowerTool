package org.teacon.powertool.client.gui.holo_sign;

import com.mojang.brigadier.StringReader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.commands.ParserUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import org.jetbrains.annotations.NotNull;
import org.teacon.powertool.block.entity.RawJsonHolographicSignBlockEntity;
import org.teacon.powertool.client.gui.widget.JsonComponentList;
import org.teacon.powertool.utils.VanillaUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RawJsonHolographicSignEditingScreen extends BaseHolographicSignEditingScreen<RawJsonHolographicSignBlockEntity> {
    
    public List<String> content;
    protected Button append;
    protected JsonComponentList jsonComponentList;
    
    public RawJsonHolographicSignEditingScreen(RawJsonHolographicSignBlockEntity theSign) {
        super(Component.translatable("sign.edit.raw_json"), theSign);
        content = theSign.content;
    }
    
    @Override
    protected void init() {
        super.init();
        var box_l = (int)Math.max(100,width*0.7);
        var startY = (int)(height*0.05);
        this.append = Button.builder(Component.literal("+"),(b) -> {
            if(this.jsonComponentList != null) jsonComponentList.appendEntry();
        }).size(20,20).pos(width/2+box_l/2-25,startY+52).build();
        this.jsonComponentList = new JsonComponentList(this,box_l,startY+50);
        this.addRenderableWidget(jsonComponentList);
        this.addRenderableWidget(append);
    }
    
    @Override
    public int getDoneButtonY() {
        var startY = (int)(height*0.15);
        return (int) (startY + height*0.7 + 15);
    }
    
    @Override
    protected void writeBackToBE() {
        super.writeBackToBE();
        sign.content = new ArrayList<>(jsonComponentList.entries().stream().map(JsonComponentList.Entry::contentString).toList());
        sign.forFilter = new ArrayList<>(jsonComponentList.entries().stream().map(JsonComponentList.Entry::content).toList());
    }
    
}
