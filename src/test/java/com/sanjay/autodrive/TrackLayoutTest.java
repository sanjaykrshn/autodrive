package com.sanjay.autodrive;

import com.sanjay.autodrive.track.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrackLayoutTest {
    @Test void threeDistinctSeparatedCircuitsHaveStraightsLeftAndRightTurns() {
        assertEquals(3,Tracks.ALL.size());
        for(var track:Tracks.ALL) {
            var pts=track.samples();
            assertTrue(pts.getLast().distance()>700);
            assertTrue(Math.hypot(pts.getFirst().x()-pts.getLast().x(),pts.getFirst().z()-pts.getLast().z())<1);
            double left=0,right=0,straight=0;
            for(int i=2;i<pts.size();i++) {
                double turn=pts.get(i).yaw()-pts.get(i-1).yaw();turn=Math.atan2(Math.sin(turn),Math.cos(turn));
                if(turn<0) left-=turn; else right+=turn;
                if(Math.abs(turn)<0.001) straight+=0.5;
            }
            assertTrue(left>2,track.id()+" needs left turns");assertTrue(right>2,track.id()+" needs right turns");
            assertTrue(straight>30,track.id()+" needs a long straight");
        }
    }
    @Test void nonAdjacentRoadsAndBarriersNeverMerge() {
        for(var track:Tracks.ALL) {
            var pts=track.samples();double length=pts.getLast().distance();
            for(int i=0;i<pts.size();i+=5) for(int j=i+5;j<pts.size();j+=5) {
                var a=pts.get(i);var b=pts.get(j);
                double arc=Math.min(b.distance()-a.distance(),length-(b.distance()-a.distance()));
                if(arc>40) assertTrue(Math.hypot(a.x()-b.x(),a.z()-b.z())>23,track.id()+" sections touch");
            }
        }
    }
    @Test void orderedCheckpointsCoverTheLoopAndStartAtGrid() {
        for(var track:Tracks.ALL) {
            var checkpoints=track.checkpoints();assertTrue(checkpoints.size()>20);
            assertEquals(track.originX(),checkpoints.getFirst().x());assertEquals(track.originZ(),checkpoints.getFirst().z());
            for(int i=1;i<checkpoints.size();i++) {
                double gap=checkpoints.get(i).distance()-checkpoints.get(i-1).distance();
                assertTrue(gap>33&&gap<37);
            }
        }
    }
}
