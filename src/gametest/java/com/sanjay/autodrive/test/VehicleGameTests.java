package com.sanjay.autodrive.test;

import com.sanjay.autodrive.AutoDrive;
import com.sanjay.autodrive.entity.VehicleEntity;
import com.sanjay.autodrive.vehicle.VehicleControl;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class VehicleGameTests {
    private VehicleEntity car(GameTestHelper h) {
        for(int x=0;x<40;x++)for(int z=0;z<24;z++)h.setBlock(x,1,z,Blocks.CONCRETE.gray());
        var car=new VehicleEntity(AutoDrive.VEHICLE,h.getLevel());
        car.setPos(h.absoluteVec(new Vec3(4,2.05,12)));car.setYRot(-90);
        h.getLevel().addFreshEntity(car);
        return car;
    }
    @GameTest(structure="autodrive-test:driving_pad",maxTicks=110)
    public void programmaticThrottleBrakesAndExpires(GameTestHelper h) {
        var car=car(h);double start=car.getX();
        car.applyControl(new VehicleControl(0,1,0),40);
        h.runAtTickTime(35,()->{
            h.assertTrue(car.getX()>start+3,"Throttle must move the real entity");
            h.assertTrue(car.speedMetersPerSecond()>4,"Throttle must accelerate");
        });
        h.runAtTickTime(80,()->{
            h.assertTrue(car.mode().equals("STOPPED"),"Expired commands must leave PROGRAMMATIC mode");
            h.assertTrue(car.speedMetersPerSecond()==0,"Expired control must brake to a stop");h.succeed();
        });
    }
    @GameTest(structure="autodrive-test:driving_pad",maxTicks=110)
    public void wallCollisionStopsVehicle(GameTestHelper h) {
        var car=car(h);double wallX=h.absoluteVec(new Vec3(14,2,12)).x;
        for(int z=1;z<23;z++)for(int y=2;y<5;y++)h.setBlock(14,y,z,Blocks.STONE);
        car.applyControl(new VehicleControl(0,1,0),100);
        h.runAtTickTime(95,()->{
            h.assertTrue(car.getX()+car.getBbWidth()/2<=wallX+0.01,"Vehicle must not pass through walls");
            h.assertTrue(car.collisionCount()>0,"Collision must be observed");
            h.assertTrue(car.speedMetersPerSecond()<0.3,"Wall collision must stop forward speed");h.succeed();
        });
    }
    @GameTest(structure="autodrive-test:driving_pad",maxTicks=100)
    public void manualInputAcceleratesSteersAndBrakes(GameTestHelper h) {
        var car=car(h);double start=car.getX();
        ServerPlayer driver=h.makeMockServerPlayerInLevel();
        car.setOwner(driver.getUUID());driver.setPos(car.position());driver.startRiding(car,true,true);
        driver.setLastClientInput(new Input(true,false,false,false,false,false,false));
        h.runAtTickTime(30,()->{
            h.assertTrue(car.getX()>start+2,"Manual forward input must drive the car");
            driver.setLastClientInput(new Input(true,false,false,true,false,false,false));
        });
        h.runAtTickTime(40,()->{
            h.assertTrue(car.getYRot()>-85,"Manual right input must steer");
            driver.setLastClientInput(new Input(false,true,false,false,false,false,false));
        });
        h.runAtTickTime(70,()->{
            h.assertTrue(car.speedMetersPerSecond()==0,"Manual back input must brake");
            driver.stopRiding();h.succeed();
        });
    }
}
