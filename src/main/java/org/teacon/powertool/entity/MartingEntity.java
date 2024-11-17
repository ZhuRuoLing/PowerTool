package org.teacon.powertool.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.teacon.powertool.PowerTool;
import org.teacon.powertool.item.PowerToolItems;

import java.util.function.Supplier;

/**
 * MalayP asked me to write a Karting Car.
 * @author qyl27
 */
public class MartingEntity extends VehicleEntity {

    // Todo: wheel speed should depends on velocity.
    public static final double WHEEL_ROTATE_DEGREE_PER_TICK = 18;   // 360 / 20

    public static final int MAX_REMAINING_LIFE_TIME_TICKS = 120 * 20;    // 2 minutes

    // <editor-fold desc="Persistent states.">

    private Variant variant = Variant.RED;

    private int remainingLifeTimeTicks = MAX_REMAINING_LIFE_TIME_TICKS;

    // </editor-fold>

    // <editor-fold desc="Temporary states.">

    // Rotate degrees of the steering wheel, negative for left, positive for right.
    // Todo: each wheel speed depends on the steering wheel.
    private static final EntityDataAccessor<Float> DATA_ID_STEERING_WHEEL_ROTATE_DEGREE = SynchedEntityData.defineId(MartingEntity.class, EntityDataSerializers.FLOAT);

    private double wheelRotateDegree = 0;   // Wheels rotate degrees, client only

    // </editor-fold>

    public MartingEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    // <editor-fold desc="Entity staff.">

    public void setVariant(Variant variant) {
        this.variant = variant;
    }

    public Variant getVariant() {
        return variant;
    }

    @Override
    protected @NotNull Item getDropItem() {
        return variant.getItemSupplier().get();
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            if (remainingLifeTimeTicks < 0) {
                discard();
            }

            if (!getPassengers().isEmpty()) {
                remainingLifeTimeTicks = MAX_REMAINING_LIFE_TIME_TICKS;
            } else {
                remainingLifeTimeTicks -= 1;
            }

            applyGravity();
            updateInWaterStateAndDoFluidPushing();

            move(MoverType.SELF, getDeltaMovement());
        } else {
            if (!getPassengers().isEmpty()) {
                wheelRotateDegree += WHEEL_ROTATE_DEGREE_PER_TICK;
                wheelRotateDegree %= 360;
            }
        }

        super.tick();
    }

    // </editor-fold>

    // <editor-fold desc="Physics.">

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canCollideWith(@NotNull Entity entity) {
        return canVehicleCollide(this, entity);
    }

    public static boolean canVehicleCollide(@NotNull Entity vehicle, @NotNull Entity entity) {
        return (entity.canBeCollidedWith() || entity.isPushable()) && !vehicle.isPassengerOfSameVehicle(entity);
    }

    // </editor-fold>

    // <editor-fold desc="Data storage and sync.">

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compound) {
        var variant = compound.getString("variant");
        this.variant = Variant.from(variant);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compound) {
        compound.putString("variant", variant.getName());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ID_STEERING_WHEEL_ROTATE_DEGREE, 0F);
    }

    // </editor-fold>

    public enum Variant {
        RED("marting_red", PowerToolItems.MARTING_RED),
        GREEN("marting_green", PowerToolItems.MARTING_GREEN),
        BLUE("marting_blue", PowerToolItems.MARTING_BLUE),
        ;

        private final String name;
        private final Supplier<Item> itemSupplier;
        private final ResourceLocation id;
        private final ResourceLocation texture;

        Variant(String name, Supplier<Item> itemSupplier) {
            this.name = name;
            this.itemSupplier = itemSupplier;
            this.id = ResourceLocation.fromNamespaceAndPath(PowerTool.MODID, name);
            this.texture = ResourceLocation.fromNamespaceAndPath(PowerTool.MODID, "item/" + name);
        }

        public static Variant from(String name) {
            for (var v : values()) {
                if (v.getName().equals(name)) {
                    return v;
                }
            }

            return RED;
        }

        public String getName() {
            return name;
        }

        public Supplier<Item> getItemSupplier() {
            return itemSupplier;
        }

        public ResourceLocation getId() {
            return id;
        }

        public ResourceLocation getTexture() {
            return texture;
        }
    }
}
