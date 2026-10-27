package com.sanjay.autodrive;

import com.sanjay.autodrive.vehicle.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VehiclePhysicsTest {
    @Test void throttleAcceleratesAndCapsSpeed() {
        var state = new VehiclePhysics.State(0,0);
        for(int i=0;i<2000;i++) state=VehiclePhysics.step(state,new VehicleControl(0,1,0),false).state();
        assertEquals(VehiclePhysics.MAX_SPEED,state.speed(),1e-8);
    }
    @Test void brakeOverridesThrottleAndNeverReverses() {
        var state=new VehiclePhysics.State(12,0);
        for(int i=0;i<40;i++) state=VehiclePhysics.step(state,new VehicleControl(0,1,1),false).state();
        assertEquals(0,state.speed());
    }
    @Test void coastingAndOffRoadLoseSpeed() {
        var state=new VehiclePhysics.State(10,0);
        double road=VehiclePhysics.step(state,VehicleControl.COAST,false).state().speed();
        double grass=VehiclePhysics.step(state,VehicleControl.COAST,true).state().speed();
        assertTrue(road<10);assertTrue(grass<road);
    }
    @Test void steeringIsVehicleRelativeAndCannotTurnAtRest() {
        var rest=VehiclePhysics.step(new VehiclePhysics.State(0,0),new VehicleControl(1,0,0),false);
        assertEquals(0,rest.state().yaw());
        var left=VehiclePhysics.step(new VehiclePhysics.State(8,0),new VehicleControl(-1,0,0),false);
        var right=VehiclePhysics.step(new VehiclePhysics.State(8,0),new VehicleControl(1,0,0),false);
        assertTrue(left.dx()>0);assertTrue(right.dx()<0);assertEquals(left.dz(),right.dz(),1e-9);
    }
    @Test void manualInputsCancelOpposingSteeringAndSupportBothBrakes() {
        assertEquals(0,VehicleControl.manual(true,false,true,true,false).steering());
        assertEquals(1,VehicleControl.manual(false,true,false,false,false).brake());
        assertEquals(1,VehicleControl.manual(false,false,false,false,true).brake());
        assertEquals(1,VehicleControl.manual(true,false,false,false,false).throttle());
    }
    @Test void invalidExternalActionsStayFiniteAndBounded() {
        var action=new VehicleControl(Double.NaN,4,-2);
        assertEquals(0,action.steering());assertEquals(1,action.throttle());assertEquals(0,action.brake());
        var state=VehiclePhysics.step(new VehiclePhysics.State(8,0),action,false).state();
        assertTrue(Double.isFinite(state.yaw()));assertTrue(Double.isFinite(state.speed()));
    }
    @Test void headingRotationRotatesMovementWithoutChangingSpeed() {
        var south=VehiclePhysics.step(new VehiclePhysics.State(8,0),VehicleControl.COAST,false);
        var east=VehiclePhysics.step(new VehiclePhysics.State(8,-Math.PI/2),VehicleControl.COAST,false);
        assertEquals(south.dz(),east.dx(),1e-9);assertEquals(0,east.dz(),1e-9);
    }
}
