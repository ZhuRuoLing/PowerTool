package org.teacon.powertool.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teacon.powertool.PowerTool;
import org.teacon.powertool.client.renders.entity.model.MartingEntityModel;
import org.teacon.powertool.item.PowerToolItems;
import org.w3c.dom.Attr;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * MalayP asked me to write a Karting Car.
 *
 * @author qyl27
 */
public class MartingEntity extends LivingEntity {

    // Rotate degrees of the steering wheel, negative for left, positive for right.
    // Todo: each wheel speed depends on the steering wheel.
    public static final EntityDataAccessor<Float> DATA_ID_STEERING_WHEEL_ROTATE_DEGREE = SynchedEntityData.defineId(MartingEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Float> DATA_ID_DAMAGE = SynchedEntityData.defineId(MartingEntity.class, EntityDataSerializers.FLOAT);

    // Todo: wheel speed should depends on velocity.
    public static final double WHEEL_ROTATE_DEGREE_PER_TICK = 18;   // 360 / 20

    public static final int MAX_REMAINING_LIFE_TIME_TICKS = 120 * 20;    // 2 minutes

    // <editor-fold desc="Persistent states.">

    private Variant variant = Variant.RED;
    private int remainingLifeTimeTicks = MAX_REMAINING_LIFE_TIME_TICKS;
    private AttributeMap attributeMap;

    // </editor-fold>

    // <editor-fold desc="Temporary states.">

    private double wheelRotateDegree = 0;   // Wheels rotate degrees, client only

    // </editor-fold>

    public MartingEntity(EntityType<MartingEntity> entityType, Level level) {
        super(entityType, level);
    }

    public void setVariant(Variant variant) {
        this.variant = variant;
    }

    public Variant getVariant() {
        return variant;
    }

    // <editor-fold desc="Living entity staff.">

    @Override
    public @NotNull Iterable<ItemStack> getArmorSlots() {
        return List.of();
    }

    @Override
    public @NotNull ItemStack getItemBySlot(@NotNull EquipmentSlot equipmentSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(@NotNull EquipmentSlot equipmentSlot, @NotNull ItemStack itemStack) {
    }

    @Override
    public @NotNull HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide()) {
            if (remainingLifeTimeTicks < 0) {
                discard();
            }

            if (!getPassengers().isEmpty()) {
                remainingLifeTimeTicks = MAX_REMAINING_LIFE_TIME_TICKS;
            } else {
                remainingLifeTimeTicks -= 1;
            }
        } else {
            updateWheelsAnimation();
        }
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            if (this.isInvulnerableTo(source)) {
                return false;
            } else {
                this.hurtTime = 10;
                this.markHurt();
                this.setDamage(this.getDamage() + amount * 10.0F);
                this.gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());
                boolean flag = source.getEntity() instanceof Player && ((Player) source.getEntity()).getAbilities().instabuild;
                if ((flag || !(this.getDamage() > 40.0F)) && !this.shouldSourceDestroy(source)) {
                    if (flag) {
                        this.discard();
                    }
                } else {
                    this.destroy(source);
                }

                return true;
            }
        } else {
            return true;
        }
    }

    boolean shouldSourceDestroy(DamageSource source) {
        return false;
    }

    public void destroy(Item dropItem) {
        this.kill();
        if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            ItemStack itemstack = new ItemStack(dropItem);
            itemstack.set(DataComponents.CUSTOM_NAME, this.getCustomName());
            this.spawnAtLocation(itemstack);
        }
    }

    protected void destroy(DamageSource source) {
        this.destroy(this.getDropItem());
    }

    // </editor-fold>

    // <editor-fold desc="Vehicle entity staff.">

    @Override
    public boolean showVehicleHealth() {
        return false;
    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        var result = super.interact(player, hand);
        if (result != InteractionResult.PASS) {
            return result;
        }

        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        if (!level().isClientSide()) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        } else {
            return InteractionResult.SUCCESS;
        }
    }

    protected @NotNull Item getDropItem() {
        return variant.getItemSupplier().get();
    }

    @Override
    public void onPassengerTurned(@NotNull Entity passenger) {
        super.onPassengerTurned(passenger);

        setYRot(passenger.getYRot());
    }

    protected void updateWheelsAnimation() {
        // Todo: update wheels speeds.
        if (!getPassengers().isEmpty()) {
            wheelRotateDegree += WHEEL_ROTATE_DEGREE_PER_TICK;
            wheelRotateDegree %= 360;
        }
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        if (getFirstPassenger() instanceof Player player) {
            return player;
        }
        return super.getControllingPassenger();
    }

    @Override
    protected @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 travelVector) {
        float deltaDirection = player.xxa * 0.5F;
        float deltaForward = player.zza * 1.5F;
        return new Vec3(deltaDirection, 0.0F, deltaForward);
    }

    // </editor-fold>

    // <editor-fold desc="Data storage and sync.">

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        if (compound.contains("variant")) {
            var variant = compound.getString("variant");
            setVariant(Variant.from(variant));
        }

        if (compound.contains("lifetimeRemain")) {
            remainingLifeTimeTicks = compound.getInt("lifetimeRemain");
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        compound.putString("variant", variant.getName());
        compound.putInt("lifetimeRemain", remainingLifeTimeTicks);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ID_STEERING_WHEEL_ROTATE_DEGREE, 0F);
        builder.define(DATA_ID_DAMAGE, 0F);
    }

    public float getDamage() {
        return entityData.get(DATA_ID_DAMAGE);
    }

    public void setDamage(float value) {
        entityData.set(DATA_ID_DAMAGE, value);
    }


    @Override
    public @NotNull AttributeMap getAttributes() {
        if (attributeMap == null) {
            attributeMap = new AttributeMap(createAttributes());
        }
        return attributeMap;
    }

    public static AttributeSupplier createAttributes() {
        return AttributeSupplier.builder()
                .add(Attributes.MAX_HEALTH, 1)
                .add(Attributes.STEP_HEIGHT, 1)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.SCALE)
                .add(Attributes.GRAVITY)
                .add(Attributes.MOVEMENT_EFFICIENCY)
                .add(Attributes.SAFE_FALL_DISTANCE, 30)
                .add(Attributes.FALL_DAMAGE_MULTIPLIER)
                .build();
    }

    @Override
    public float getSpeed() {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    @Override
    public void setSpeed(float speed) {
        Objects.requireNonNull(getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(speed);
    }

    @Override
    public float getHealth() {
        return getDamage();
    }

    @Override
    public void setHealth(float health) {
        setDamage(health);
    }

    // </editor-fold>

    public enum Variant {
        RED("marting_red", PowerToolItems.MARTING_RED, MartingEntityModel.LAYER_RED),
        GREEN("marting_green", PowerToolItems.MARTING_GREEN, MartingEntityModel.LAYER_GREEN),
        BLUE("marting_blue", PowerToolItems.MARTING_BLUE, MartingEntityModel.LAYER_BLUE),
        ;

        private final String name;
        private final Supplier<Item> itemSupplier;
        private final ResourceLocation id;
        private final ResourceLocation texture;
        private final ModelLayerLocation layer;

        Variant(String name, Supplier<Item> itemSupplier, ModelLayerLocation layer) {
            this.name = name;
            this.itemSupplier = itemSupplier;
            this.id = ResourceLocation.fromNamespaceAndPath(PowerTool.MODID, name);
            this.texture = ResourceLocation.fromNamespaceAndPath(PowerTool.MODID, "textures/item/" + name + ".png");
            this.layer = layer;
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

        public ModelLayerLocation getModelLayer() {
            return layer;
        }
    }
}
