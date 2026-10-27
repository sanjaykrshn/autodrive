package com.sanjay.autodrive;

import com.sanjay.autodrive.command.AutoDriveCommands;
import com.sanjay.autodrive.entity.VehicleEntity;
import com.sanjay.autodrive.track.TrackBuilder;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AutoDrive implements ModInitializer {
    public static final String MOD_ID = "autodrive";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final EntityType<VehicleEntity> VEHICLE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, id("vehicle"),
            EntityType.Builder.<VehicleEntity>of(VehicleEntity::new, MobCategory.MISC)
                    .sized(2.4f, 1.2f).passengerAttachments(0.65f)
                    .clientTrackingRange(12).updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id("vehicle"))));

    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }

    @Override public void onInitialize() {
        TrackBuilder.register();
        AutoDriveCommands.register();
        LOGGER.info("AutoDrive Phase 1 loaded: manual vehicles, continuous control API and three test circuits.");
    }
}
