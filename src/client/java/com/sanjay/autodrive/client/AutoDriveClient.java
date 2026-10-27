package com.sanjay.autodrive.client;

import com.sanjay.autodrive.AutoDrive;
import com.sanjay.autodrive.entity.VehicleEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import java.util.Locale;

public final class AutoDriveClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(AutoDrive.VEHICLE,VehicleRenderer::new);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,AutoDrive.id("driving_hud"),(graphics,delta) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || !(client.player.getVehicle() instanceof VehicleEntity v)) return;
            graphics.fill(8,8,276,57,0xC0101824);
            graphics.text(client.font,"AUTODRIVE  |  "+v.mode(),15,14,0xFF6EDAE6);
            graphics.text(client.font,String.format(Locale.ROOT,"%.1f km/h   Lap %d   Checkpoint %d",v.speedMetersPerSecond()*3.6,v.laps(),v.checkpoint()),15,28,0xFFFFFFFF);
            graphics.text(client.font,"Forward: throttle  |  Back / Jump: brake",15,42,0xFFC3CCD6);
        });
    }
}
