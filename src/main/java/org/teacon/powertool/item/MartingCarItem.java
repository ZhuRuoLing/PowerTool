package org.teacon.powertool.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.NotNull;
import org.teacon.powertool.entity.MartingCarEntity;
import org.teacon.powertool.entity.PowerToolEntities;

import java.util.List;

public class MartingCarItem extends Item {
    public static final String TOOLTIP1 = "tooltip.powertool.marting";
    public static final String TOOLTIP2 = "tooltip.powertool.marting2";

    private final MartingCarEntity.Variant variant;

    public MartingCarItem(Properties properties, MartingCarEntity.Variant variant) {
        super(properties);
        this.variant = variant;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable(TOOLTIP1));
        tooltipComponents.add(Component.translatable(TOOLTIP2));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        var level = context.getLevel();
        if (!level.isClientSide()) {
            var pos = context.getClickedPos().relative(context.getClickedFace());
            var entity = new MartingCarEntity(PowerToolEntities.MARTING.get(), level);
            entity.setVariant(variant);
            entity.setPos(pos.getCenter());
            if(context.getPlayer() != null) entity.setYRot(context.getPlayer().getYRot());
            context.getItemInHand().shrink(1);
            level.addFreshEntity(entity);
        }
        return InteractionResult.SUCCESS;
    }
}
