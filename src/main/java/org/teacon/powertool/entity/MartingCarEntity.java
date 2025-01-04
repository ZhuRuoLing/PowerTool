package org.teacon.powertool.entity;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teacon.powertool.PowerTool;
import org.teacon.powertool.client.overlay.ClientDebugCharts;
import org.teacon.powertool.item.PowerToolItems;
import org.teacon.powertool.utils.VanillaUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * MalayP asked me to write a Karting Car.
 *
 * @author qyl27
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MartingCarEntity extends LivingEntity {

    // Rotate radians of the steering wheel, negative for left, positive for right.
    public static final EntityDataAccessor<Float> DATA_ID_STEERING_ROTATE_RADIAN = SynchedEntityData.defineId(MartingCarEntity.class, EntityDataSerializers.FLOAT);
    // Rotate radians of the wheels.
    public static final EntityDataAccessor<Float> DATA_ID_WHEEL_ROTATE_RADIAN = SynchedEntityData.defineId(MartingCarEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Float> DATA_ID_DAMAGE = SynchedEntityData.defineId(MartingCarEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(MartingCarEntity.class, EntityDataSerializers.INT);

    // Something definition with radians.
    public static final float WHEEL_ROTATE_RADIAN_BASE = (float) Math.toRadians(90.0);
    public static final float STEERING_ROTATE_RADIAN_LIMIT = (float) Math.toRadians(45);    // Both positive limit and negative limit

    public static final int MAX_REMAINING_LIFE_TIME_TICKS = 120 * 20;    // 2 minutes

    // <editor-fold desc="Persistent states.">
    
    private int remainingLifeTimeTicks = MAX_REMAINING_LIFE_TIME_TICKS;
    private AttributeMap attributeMap;
    private float xxaSum;
    private float zzaSum;
    // </editor-fold>

    public MartingCarEntity(EntityType<MartingCarEntity> entityType, Level level) {
        super(entityType, level);
        this.setDiscardFriction(true);
    }

    public void setVariant(Variant variant) {
        this.entityData.set(VARIANT,variant.ordinal());
    }

    public Variant getVariant() {
        return Variant.from(this.entityData.get(VARIANT));
    }

    public float getSteeringRotateRadian() {
        return this.entityData.get(DATA_ID_STEERING_ROTATE_RADIAN);
    }

    public float getWheelRotateRadian() {
        return this.entityData.get(DATA_ID_WHEEL_ROTATE_RADIAN);
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
        var f2 = this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getFriction(level(), this.getBlockPosBelowThatAffectsMyMovement(), this);
        f2 = this.onGround() ? f2 * 0.1F : 0.05F;
        var facing = getYRot();
        var v = getInputVector(getDeltaMovement(),1,-facing);
        this.setDeltaMovement(getInputVector(new Vec3(v.x*0.9,v.y,v.z),0.9f+f2,facing));
        
        setYHeadRot(facing);
        setYBodyRot(facing);
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
            updateWheelsRotate();
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
                if ((flag || !(this.getDamage() > 10.0F)) && !this.shouldSourceDestroy(source)) {
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

    @SuppressWarnings("unused")
    boolean shouldSourceDestroy(DamageSource source) {
        return false;
    }

    public void destroy(Item dropItem) {
        this.discard();
        if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            ItemStack itemstack = new ItemStack(dropItem);
            itemstack.set(DataComponents.CUSTOM_NAME, this.getCustomName());
            this.spawnAtLocation(itemstack);
        }
    }

    @SuppressWarnings("unused")
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
            player.setDiscardFriction(true);
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        } else {
            return InteractionResult.SUCCESS;
        }
    }
    
    protected @NotNull Item getDropItem() {
        return getVariant().getItemSupplier().get();
    }

    protected void updateWheelsRotate() {
        if (!getPassengers().isEmpty()) {
            var delta = WHEEL_ROTATE_RADIAN_BASE;
            delta *= (float) Mth.clamp(Math.log(getDeltaMovement().length()+0.9),0,4);
            if(!movingForward()) delta = -delta;
            float original = this.entityData.get(DATA_ID_WHEEL_ROTATE_RADIAN);
            original += Mth.PI;
            original += delta;
            original %= Mth.TWO_PI;
            original -= Mth.PI;
            ClientDebugCharts.recordDebugData("Speed", (long) (Mth.sign(delta)*getDeltaMovement().length()*100));
            this.entityData.set(DATA_ID_WHEEL_ROTATE_RADIAN, original);
        } else {
            this.entityData.set(DATA_ID_WHEEL_ROTATE_RADIAN, 0F);
        }
    }
    
    @Override
    protected float getBlockSpeedFactor() {
        return 1;
    }
    
    protected boolean movingForward(){
        var move = getDeltaMovement();
        var yRot = hasControllingPassenger() ? Objects.requireNonNull(getControllingPassenger()).getViewVector(0) : getViewVector(0);
        return move.dot(yRot) > 0;
    }

    protected void updateSteeringRotate(float input) {
        float value = Mth.rotLerp(input, 0, STEERING_ROTATE_RADIAN_LIMIT);
        this.entityData.set(DATA_ID_STEERING_ROTATE_RADIAN, value);
    }
    
    @Override
    public float getFrictionInfluencedSpeed(float friction) {
        return 0;
    }
    
    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        if (getFirstPassenger() instanceof Player player) {
            return player;
        }
        return super.getControllingPassenger();
    }
    
    @Override
    public boolean vibrationAndSoundEffectsFromBlock(BlockPos pos, BlockState state, boolean playStepSound, boolean broadcastGameEvent, Vec3 entityPos) {
        if(PowerTool.GLOBAL_RANDOM.get().nextBoolean()) return false;
        return super.vibrationAndSoundEffectsFromBlock(pos, state, playStepSound, broadcastGameEvent, entityPos);
    }
    
    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        passenger.setYRot(passenger.getYRot() - xxaSum);
        passenger.setYHeadRot(passenger.getYHeadRot() - xxaSum);
    }
    
    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        //setYRot(player.getYHeadRot());
        updateSteeringRotate(player.xxa);
        xxaSum = Mth.clamp((xxaSum + player.xxa*5f)*0.75f,-30f,30f);
        var yRot = (360 + getYRot() - xxaSum) % 360;
        setYRot(yRot);
        zzaSum = Mth.clamp((zzaSum + player.zza*0.04f)*0.75f,-0.25f,0.25f);
        if(getDeltaMovement().lengthSqr() < 2) this.moveRelative(1,new Vec3(0f, 0f, zzaSum));
        return Vec3.ZERO;
    }

    // </editor-fold>

    // <editor-fold desc="Data storage and sync.">

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("variant")) {
            var variant = compound.getString("variant");
            setVariant(Variant.from(variant));
        }

        if (compound.contains("lifetimeRemain")) {
            remainingLifeTimeTicks = compound.getInt("lifetimeRemain");
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        compound.putString("variant", getVariant().getName());
        compound.putInt("lifetimeRemain", remainingLifeTimeTicks);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ID_STEERING_ROTATE_RADIAN, 0F);
        builder.define(DATA_ID_WHEEL_ROTATE_RADIAN, 0F);
        builder.define(DATA_ID_DAMAGE, 0F);
        builder.define(VARIANT,0);
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
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 1)
                .add(Attributes.STEP_HEIGHT, 1.5)
                .add(Attributes.MOVEMENT_SPEED, 0.7)
                .add(Attributes.SCALE)
                .add(Attributes.GRAVITY)
                .add(Attributes.MOVEMENT_EFFICIENCY)
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY)
                .add(Attributes.SAFE_FALL_DISTANCE, 30)
                .add(Attributes.FALL_DAMAGE_MULTIPLIER)
                .add(NeoForgeMod.SWIM_SPEED)
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
    
    @Override
    public void aiStep() {
        if (this.noJumpDelay > 0) {
            this.noJumpDelay--;
        }
        
        if (this.isControlledByLocalInstance()) {
            this.lerpSteps = 0;
            this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
        }
        
        if (this.lerpSteps > 0) {
            this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
            this.lerpSteps--;
        }
        
        if (this.lerpHeadSteps > 0) {
            this.lerpHeadRotationStep(this.lerpHeadSteps, this.lerpYHeadRot);
            this.lerpHeadSteps--;
        }
        
        Vec3 vec3 = this.getDeltaMovement();
        double d0 = vec3.x;
        double d1 = vec3.y;
        double d2 = vec3.z;
        if (Math.abs(vec3.x) < 0.003) {
            d0 = 0.0;
        }
        
        if (Math.abs(vec3.y) < 0.003) {
            d1 = 0.0;
        }
        
        if (Math.abs(vec3.z) < 0.003) {
            d2 = 0.0;
        }
        
        this.setDeltaMovement(d0, d1, d2);
        this.level().getProfiler().push("ai");
        if (this.isImmobile()) {
            this.jumping = false;
            this.xxa = 0.0F;
            this.zza = 0.0F;
        } else if (this.isEffectiveAi()) {
            this.level().getProfiler().push("newAi");
            this.serverAiStep();
            this.level().getProfiler().pop();
        }
        
        this.level().getProfiler().pop();
        this.level().getProfiler().push("jump");
        if (this.jumping && this.isAffectedByFluids()) {
            double d3;
            net.neoforged.neoforge.fluids.FluidType fluidType = this.getMaxHeightFluidType();
            if (!fluidType.isAir()) d3 = this.getFluidTypeHeight(fluidType);
            else
            if (this.isInLava()) {
                //noinspection deprecation
                d3 = this.getFluidHeight(FluidTags.LAVA);
            } else {
                //noinspection deprecation
                d3 = this.getFluidHeight(FluidTags.WATER);
            }
            
            boolean flag = this.isInWater() && d3 > 0.0;
            double d4 = this.getFluidJumpThreshold();
            if (!flag || this.onGround() && !(d3 > d4)) {
                if (!this.isInLava() || this.onGround() && !(d3 > d4)) {
                    if (fluidType.isAir() || this.onGround() && !(d3 > d4)) {
                        if ((this.onGround() || flag && d3 <= d4) && this.noJumpDelay == 0) {
                            this.jumpFromGround();
                            this.noJumpDelay = 10;
                        }
                    } else this.jumpInFluid(fluidType);
                } else {
                    this.jumpInFluid(net.neoforged.neoforge.common.NeoForgeMod.LAVA_TYPE.value());
                }
            } else {
                this.jumpInFluid(net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value());
            }
        } else {
            this.noJumpDelay = 0;
        }
        
        this.level().getProfiler().pop();
        this.level().getProfiler().push("travel");
        this.xxa *= 0.98F;
        this.zza *= 0.98F;
        this.updateFallFlying();
        AABB aabb = this.getBoundingBox();
        Vec3 vec31 = new Vec3(this.xxa, this.yya, this.zza);
        if (this.hasEffect(MobEffects.SLOW_FALLING) || this.hasEffect(MobEffects.LEVITATION)) {
            this.resetFallDistance();
        }
        
        label104: {
            if (this.getControllingPassenger() instanceof Player player && this.isAlive()) {
                this.travelRidden(player, vec31);
                break label104;
            }
            
            this.travel(vec31);
        }
        
        this.level().getProfiler().pop();
        this.level().getProfiler().push("freezing");
        if (!this.level().isClientSide && !this.isDeadOrDying()) {
            int i = this.getTicksFrozen();
            if (this.isInPowderSnow && this.canFreeze()) {
                this.setTicksFrozen(Math.min(this.getTicksRequiredToFreeze(), i + 1));
            } else {
                this.setTicksFrozen(Math.max(0, i - 2));
            }
        }
        
        this.removeFrost();
        this.tryAddFrost();
        if (!this.level().isClientSide && this.tickCount % 40 == 0 && this.isFullyFrozen() && this.canFreeze()) {
            this.hurt(this.damageSources().freeze(), 1.0F);
        }
        
        this.level().getProfiler().pop();
        this.level().getProfiler().push("push");
        if (this.autoSpinAttackTicks > 0) {
            this.autoSpinAttackTicks--;
            this.checkAutoSpinAttack(aabb, this.getBoundingBox());
        }
        
        this.pushEntities();
        this.level().getProfiler().pop();
        if (!this.level().isClientSide && this.isSensitiveToWater() && this.isInWaterRainOrBubble()) {
            this.hurt(this.damageSources().drown(), 1.0F);
        }
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
            this.texture = ResourceLocation.fromNamespaceAndPath(PowerTool.MODID, "textures/entity/" + name + ".png");
        }

        public static Variant from(String name) {
            for (var v : values()) {
                if (v.getName().equals(name)) {
                    return v;
                }
            }
            return RED;
        }
        
        public static Variant from(int ordinal){
            return switch (ordinal){
                case 1 -> GREEN;
                case 2 -> BLUE;
                default -> RED;
            };
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
