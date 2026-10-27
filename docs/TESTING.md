# Verification

Run `./gradlew build` for the normal checks. Java 25+ is required.

## Unit checks (10)

Seven tests cover acceleration/speed cap, braking priority, drag/off-road behavior,
steering at rest and in motion, manual input mapping, invalid action sanitization,
and rotated coordinate frames. Three geometry tests check distinct mixed-turn
circuits, nonadjacent section clearance and ordered checkpoint coverage.

## Minecraft server GameTests (3 project tests)

`VehicleGameTests` runs real vehicles on concrete pads in the headless game server:

1. Throttle moves and accelerates an entity; command expiry brakes it to rest.
2. A solid wall blocks the entity and records a collision.
3. A mock connected server player accelerates, turns and brakes using manual inputs.

Fabric also includes its own empty structure sanity test in the runner.
The XML report is written to `build/test-results/gametest.xml`.

## Minecraft client smoke test

`./gradlew runClientGameTest` opens the client, creates a new flat world named
AutoDrive Test Grounds, builds the three circuits through the real command dispatcher,
enters the Rookie car, waits for the client terrain load to finish and then exercises
forward/brake keyboard bindings through vanilla input packets. It captures the
vehicle/HUD and circuit overviews, captures overviews of the other circuits, and saves the world.

World: `build/run/clientGameTest/saves/AutoDrive Test Grounds`.
Screenshots: `build/run/clientGameTest/screenshots`.
This test resets its own run directory on each run; copy a save before rerunning if needed.
No trained policy, lap completion performance or generalization has been tested or claimed.
