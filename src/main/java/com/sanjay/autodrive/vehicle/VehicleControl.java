package com.sanjay.autodrive.vehicle;

/** Continuous vehicle-relative actions. Positive steering turns right. No policy is supplied in Phase 1. */
public record VehicleControl(double steering, double throttle, double brake) {
    public static final VehicleControl STOP = new VehicleControl(0, 0, 1);
    public static final VehicleControl COAST = new VehicleControl(0, 0, 0);
    public VehicleControl {
        steering = bounded(steering, -1, 1);
        throttle = bounded(throttle, 0, 1);
        brake = bounded(brake, 0, 1);
    }
    private static double bounded(double value, double min, double max) {
        return Double.isFinite(value) ? Math.clamp(value, min, max) : 0;
    }
    public static VehicleControl manual(boolean forward, boolean backward, boolean left, boolean right, boolean jump) {
        return new VehicleControl((right ? 1 : 0) - (left ? 1 : 0), forward ? 1 : 0, backward || jump ? 1 : 0);
    }
}
