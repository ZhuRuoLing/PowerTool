package org.teacon.powertool.attachment;

import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.teacon.powertool.PowerTool;
import org.teacon.powertool.network.attachment.Permission;

import java.util.ArrayList;
import java.util.List;

public class PowerToolAttachments {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPE = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, PowerTool.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Permission>> PERMISSION = ATTACHMENT_TYPE.register(Permission.KEY.getPath(),
        () -> AttachmentType.builder(Permission::new).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<List<BlockPos>>> DISPLAY_MODE = ATTACHMENT_TYPE.register(
        "display_mode",
        () -> AttachmentType.<List<BlockPos>>builder(() -> new ArrayList<>())
            .serialize(BlockPos.CODEC.listOf())
            .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<List<BlockPos>>> CACHED_MODE = ATTACHMENT_TYPE.register(
        "cached_mode",
        () -> AttachmentType.<List<BlockPos>>builder(() -> new ArrayList<>())
            .serialize(BlockPos.CODEC.listOf())
            .build()
    );
    
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<List<BlockPos>>> STATIC_MODE = ATTACHMENT_TYPE.register(
            "static_mode",
            () -> AttachmentType.<List<BlockPos>>builder(() -> new ArrayList<>())
                    .serialize(BlockPos.CODEC.listOf())
                    .build()
    );

    public static void register(IEventBus bus) {
        ATTACHMENT_TYPE.register(bus);
    }
}
