package org.teacon.powertool.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Special button that only renders tooltip and triggers narration. */
public class InvisibleButton extends Button {
    public InvisibleButton(Builder builder) {
        super(builder);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        // FIXME Proper narration when button toggled
        output.add(NarratedElementType.HINT, Component.literal("Button toggled"));
    }
}
