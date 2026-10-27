package com.sanjay.autodrive.test;

import com.sanjay.autodrive.entity.VehicleEntity;
import com.sanjay.autodrive.track.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import java.nio.file.Path;

/** On-demand end-to-end smoke test, also leaves a playable save and circuit screenshots. */
public class CircuitClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c) {
        try(var world=c.worldBuilder().adjustSettings(s -> s.setName("AutoDrive Test Grounds")).create()) {
            var server=world.getServer();var connection=world.getConnection();
            server.runCommand("execute as @a run autodrive track build all");
            server.waitFor(s -> !TrackBuilder.busy(),6000);
            server.runCommand("execute as @a run autodrive track go rookie");
            connection.waitForClientboundPackets();
            c.waitFor(mc -> mc.player != null && mc.player.getVehicle() instanceof VehicleEntity,600);
            connection.waitForChunksRender();
            c.waitForScreen(null);c.waitTicks(20);
            server.runCommand("time set 6000");server.runCommand("weather clear");server.runCommand("gamemode creative @a");
            // Exercise actual client key mappings and the vanilla input packet path.
            c.getInput().holdKey(o -> o.keyUp);c.waitTicks(40);
            connection.waitForServerboundPackets();
            server.runOnServer(s -> {
                var car=(VehicleEntity)connection.getServerPlayer().getVehicle();
                if(car.speedMetersPerSecond()<3 || car.getX()<3) throw new AssertionError("Client throttle did not move vehicle");
            });
            c.getInput().releaseKey(o -> o.keyUp);
            c.getInput().holdKeyFor(o -> o.keyDown,50);
            server.runOnServer(s -> {
                var car=(VehicleEntity)connection.getServerPlayer().getVehicle();
                if(car.speedMetersPerSecond()>0.01) throw new AssertionError("Client brake failed");
            });
            server.runCommand("execute as @a run autodrive reset");
            c.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            c.getInput().lookAt(-90,18);
            connection.waitForChunksRender();
            c.waitTicks(10);
            c.takeScreenshot(TestScreenshotOptions.of("vehicle-and-hud").withSize(1280,720));
            c.runOnClient(mc -> { mc.options.setCameraType(CameraType.FIRST_PERSON);if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();mc.options.renderDistance().set(20);mc.options.broadcastOptions(); });
            c.waitTicks(60);
            server.runCommand("execute as @a run ride @s dismount");
            server.runCommand("gamerule minecraft:spectators_generate_chunks true");
            server.runCommand("gamemode spectator @a");
            for(var track:Tracks.ALL) {
                server.runCommand("execute in autodrive:test_tracks run tp @a "+(track.originX()+55)+" 160 95 0 90");
                connection.waitForClientboundPackets();c.waitTicks(100);connection.waitForChunksRender(false);c.waitTicks(20);
                c.takeScreenshot(TestScreenshotOptions.of(track.id()+"-circuit").withSize(1280,960));
            }
            server.runCommand("gamemode creative @a");
            server.runCommand("execute as @a run autodrive track go rookie");
            c.runOnClient(mc -> {if(mc.gui.hud.isHidden())mc.gui.hud.toggle();mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);});
            c.waitTicks(20);
            System.out.println("AUTODRIVE_PLAYABLE_WORLD="+world.getWorldSave().getSaveDirectory().toAbsolutePath());
        }
    }
}
