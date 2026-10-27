# Vehicle contract for later simulator integration

The authoritative implementation is `VehiclePhysics.java`. It has no Minecraft
or Fabric dependencies and is tested independently.

- Tick duration: 0.05 seconds (20 Hz).
- Speed: meters/second, assuming one Minecraft block is one meter.
- Yaw: radians internally; zero points toward +Z, positive yaw turns toward -X.
- Actions: steering [-1,1], throttle [0,1], brake [0,1]. Positive steering is right.
- Maximum forward speed: 16 m/s (57.6 km/h).
- Throttle acceleration: 3.5 m/s².
- Brake deceleration: 10 m/s², multiplied by brake input.
- Rolling resistance: 0.35 m/s²; speed-dependent drag: 0.08 × speed.
- Additional off-road drag: 3 m/s².
- Wheelbase: 2.4 m; maximum steering angle: 30 degrees.

Each step computes:

```
a = throttle * (1 - brake) * 3.5 - brake * 10 - 0.35 - 0.08 * speed
# subtract another 3 off-road
speed = clamp(speed + a * dt, 0, 16)
yaw += speed / 2.4 * tan(steering * radians(30)) * dt
dx = -sin(yaw) * speed * dt
dz =  cos(yaw) * speed * dt
```

Braking wins over simultaneous throttle, cannot create reverse motion, and steering
cannot rotate a stationary vehicle. Non-finite inputs become zero and actions are clamped.
The Minecraft entity applies gravity and delegates movement to `Entity.move`, then
zeros forward speed on horizontal collision. Water/lava stops the vehicle.
Grass, runoff and other surfaces apply the off-road drag; gray/white/black/red
concrete use the road model. Collision geometry is a conservative axis-aligned
2.4 × 2.4 × 1.2 block box; it does not rotate with the visual body.

An unoccupied manual vehicle brakes. Programmatic commands have an explicit time
lease, and expiry brakes the vehicle. Reloading an entity always discards previous
control and speed. The vehicle's owner and circuit assignment persist; lap and
collision counters are per-session. Ordered checkpoint proximity provides basic lap
progress, not anti-cheat race timing or validated RL evaluation.

The future Python simulator should reproduce the same tick duration, action ranges,
coordinate convention and coefficients. Minecraft collision, gravity and surface
interactions still require explicit simulator-to-game validation.
