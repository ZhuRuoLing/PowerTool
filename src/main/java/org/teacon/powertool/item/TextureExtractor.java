package org.teacon.powertool.item;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.teacon.powertool.menu.TextureExtractorMenu;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TextureExtractor extends Item {
    
    public TextureExtractor(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        var itemstack = player.getItemInHand(usedHand);
        if(level.isClientSide) return InteractionResultHolder.success(itemstack);
        player.openMenu(new TextureExtractorMenu.Provider());
        return InteractionResultHolder.pass(itemstack);
    }
}
