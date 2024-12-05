package io.github.tt432.eyelib.client.render.sections;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import org.teacon.powertool.PowerTool;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME, modid = PowerTool.MODID, value = Dist.CLIENT)
public class SectionGeometryGameEvents {
    @SubscribeEvent
    public static void addSectionGeometry(AddSectionGeometryEvent event) {
        event.addRenderer(new SectionGeometryBlockEntityRenderDispatcher(event.getSectionOrigin().immutable()));
    }
}
