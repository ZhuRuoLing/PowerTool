package org.teacon.powertool.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;

public class AutoVanishMinecart extends Minecart {
    
    protected int idleTickCount = 0;
    
    public AutoVanishMinecart(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }
    
    public AutoVanishMinecart(Level level, double x, double y, double z) {
        super(PowerToolEntities.AUTO_VANISH_MINECART.get(),level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }
    
    public static AutoVanishMinecart fromMinecart(Minecart minecart) {
        var result = new AutoVanishMinecart(minecart.level(),minecart.xo,minecart.yo,minecart.zo);
        result.setYRot(minecart.getYRot());
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
}
