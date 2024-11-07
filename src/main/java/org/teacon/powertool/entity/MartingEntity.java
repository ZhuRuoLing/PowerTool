package org.teacon.powertool.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.teacon.powertool.PowerTool;
import org.teacon.powertool.item.PowerToolItems;

import java.util.function.Supplier;

public class MartingEntity extends VehicleEntity {
    private Variant variant = Variant.RED;

    public MartingEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected @NotNull Item getDropItem() {
        return variant.getItemSupplier().get();
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compound) {
        var variant = compound.getString("variant");
        this.variant = Variant.from(variant);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compound) {
        compound.putString("variant", variant.getName());
    }

    public Variant getVariant() {
        return variant;
    }

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
