package org.teacon.powertool.entity;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.teacon.powertool.item.PowerToolItems;

@MethodsReturnNonnullByDefault
public class AutoVanishBoat extends Boat {
    
    protected int idleTickCount = 0;
    
    public AutoVanishBoat(EntityType<? extends Boat> entityType, Level level) {
        super(entityType, level);
    }
    
    public AutoVanishBoat(Level level, double x, double y, double z) {
        super(PowerToolEntities.AUTO_VANISH_BOAT.get(),level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }
    
    public static AutoVanishBoat fromBoat(Boat boat) {
        var result = new AutoVanishBoat(boat.level(), boat.xo, boat.yo, boat.zo);
        result.setVariant(boat.getVariant());
        result.setYRot(boat.getYRot());
        return result;
    }
    
    @Override
    public void tick() {
        if(!this.level().isClientSide()){
            if(this.getPassengers().isEmpty()){
                idleTickCount++;
            }
            else {
                idleTickCount = 0;
            }
            if(idleTickCount > 401){
                this.discard();
            }
        }
        super.tick();
    }
    
    @Override
    public Item getDropItem() {
        return switch (this.getVariant()) {
            case SPRUCE -> PowerToolItems.AV_SPRUCE_BOAT.get();
            case BIRCH -> PowerToolItems.AV_BIRCH_BOAT.get();
            case JUNGLE -> PowerToolItems.AV_JUNGLE_BOAT.get();
            case ACACIA -> PowerToolItems.AV_ACACIA_BOAT.get();
            case CHERRY -> PowerToolItems.AV_CHERRY_BOAT.get();
            case DARK_OAK -> PowerToolItems.AV_DARK_OAK_BOAT.get();
            case MANGROVE -> PowerToolItems.AV_MANGROVE_BOAT.get();
            case BAMBOO -> PowerToolItems.AV_BAMBOO_RAFT.get();
            default -> PowerToolItems.AV_OAK_BOAT.get();
        };
    }
}
