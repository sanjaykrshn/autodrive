package com.sanjay.autodrive.vehicle;

/** Fixed-step bicycle model in meters, seconds and radians, isolated for future simulator parity. */
public final class VehiclePhysics {
    public static final double DT = 1.0 / 20.0;
    public static final double MAX_SPEED = 16.0;
    public static final double ACCELERATION = 3.5;
    public static final double BRAKING = 10.0;
    public static final double ROLLING_DRAG = 0.35;
    public static final double LINEAR_DRAG = 0.08;
    public static final double OFF_ROAD_DRAG = 3.0;
    public static final double WHEELBASE = 2.4;
    public static final double MAX_STEER = Math.toRadians(30);

    public record State(double speed, double yaw) {}
    public record Step(State state, double dx, double dz) {}
    private VehiclePhysics() {}

    public static Step step(State previous, VehicleControl action, boolean offRoad) {
        // Brake has priority over throttle; braking never creates reverse motion.
        double acceleration = action.throttle() * (1 - action.brake()) * ACCELERATION
                - action.brake() * BRAKING - ROLLING_DRAG - LINEAR_DRAG * previous.speed()
                - (offRoad ? OFF_ROAD_DRAG : 0);
        double speed = Math.clamp(previous.speed() + acceleration * DT, 0, MAX_SPEED);
        double yaw = previous.yaw() + speed / WHEELBASE * Math.tan(action.steering() * MAX_STEER) * DT;
        yaw = Math.atan2(Math.sin(yaw), Math.cos(yaw));
        return new Step(new State(speed, yaw), -Math.sin(yaw) * speed * DT, Math.cos(yaw) * speed * DT);
    }
}
