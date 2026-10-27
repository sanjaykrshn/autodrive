# AutoDrive

A Minecraft Fabric vehicle platform for a reinforcement learning driving project.
**Phase 1 implements the Minecraft vehicle and test environment.** PPO training,
Python simulation and ONNX inference are planned; this version does not include an autonomous policy.

## Start the game

### Run from the source code

1. Download this repository using **Code → Download ZIP** and extract it, or clone it.
2. Install Java **25 or newer** and open a terminal in the project folder.
3. Start the development game:

   macOS/Linux:

   ```sh
   ./gradlew runClient
   ```

   Windows PowerShell:

   ```powershell
   .\gradlew.bat runClient
   ```

   If macOS/Linux reports permission denied, run `chmod +x gradlew` first.
   The Gradle wrapper downloads the development dependencies, including Fabric API.
4. Choose **Singleplayer**. Open an existing test world, or create a Creative world
   with commands/cheats enabled. A Superflat world is convenient, but the mod also
   provides its own flat test area inside any world.
5. Press **T** to open chat and follow the first-drive steps below.

### Install in a normal Minecraft instance

Build the mod with `./gradlew build` (Windows: `.\gradlew.bat build`). The installable
file is `build/libs/autodrive-0.1.0.jar`; the `-sources.jar` is for reading source code.

Use Minecraft **26.3**, Java **25 or newer**, Fabric Loader **0.19.5 or newer** and
Fabric API **0.161.0+26.3**. Put the AutoDrive JAR and Fabric API in the game's `mods` folder.
AutoDrive must be installed on both client and server. Commands require cheats/operator permission.

## Your first drive

Enter this command in Minecraft chat:

```mcfunction
/autodrive track build all
```

Wait for all three completion messages, then enter the first circuit:

```mcfunction
/autodrive track go rookie
```

This teleports you to the starting grid and seats you in a car. Hold **W** to
accelerate, use **A/D** to steer, and use **S** or **Space** to brake. Press **F5**
for a third-person view; some keyboards require **Fn+F5**. Slow down before tight turns.

If you are using an already-built AutoDrive Test Grounds save, skip the build command
and go straight to `track go rookie`.

To switch circuits, use either of these:

```mcfunction
/autodrive track go technical
/autodrive track go grand_prix
```

The circuits live in **AutoDrive Test Grounds**, the mod's own superflat dimension
(`autodrive:test_tracks`). Building writes only to this dedicated area. It does not
place roads into your overworld. Construction is incremental and can be repeated.

| Circuit | Approximate length | Character |
| --- | ---: | --- |
| Rookie Circuit (`rookie`) | 780 blocks | Long straight, sweepers, chicane, hairpin and linked corners |
| Switchback Park (`technical`) | 871 blocks | Alternating hairpins, chicanes and a tighter technical section |
| Grand Prix Gardens (`grand_prix`) | 890 blocks | Fast sweepers, a braking hairpin and flowing S bends |

All three are asymmetric closed loops with 12-block-wide roads, white edge lines,
red/white curbs, blue runoff, solid collision barriers, illuminated checkpoint
markers and a checkerboard start/finish gantry. Road surfaces are at Y=-61;
vehicles drive at Y=-60. The third layout is reserved for future evaluation;
no learning or generalization result is claimed.

### Controls

Right-click a car to enter. Use the normal movement bindings (defaults below):

| Input | Action |
| --- | --- |
| W | Throttle |
| A / D | Steer left / right |
| S or Space | Brake |
| Shift | Dismount |
| F5 | Change camera view |

The HUD shows speed in km/h, control mode, completed laps and the next checkpoint.
Pass every checkpoint in order before crossing the finish to count a lap.
This phase supports forward driving; S is a brake, not reverse gear.

```mcfunction
/autodrive spawn
/autodrive manual
/autodrive stop
/autodrive reset
/autodrive status
/autodrive track list
```

`spawn` places a car in front of you; `track go` supplies a car at the selected grid.
`reset` returns your current/nearby car to its grid. `stop` stops immediately;
`manual` resumes keyboard control. Cars are owned by their spawning player.
An owner in Creative mode can remove a car by hitting it.

### Fix a damaged road or a stuck vehicle

If you accidentally break road blocks, restore the original circuit with:

```mcfunction
/autodrive track build rookie
```

Use `technical` or `grand_prix` for the other circuits. To restore all three:

```mcfunction
/autodrive track build all
```

Wait for the completion messages. Rebuilding restores the standard layout,
including the road, curbs and barriers, and replaces edits within the course footprint.
`/autodrive reset` resets your car; rebuilding repairs the road.

If you fall through a hole or get separated from your car, use
`/autodrive track go rookie` (or the relevant circuit) to return to a grid.
If controls do not respond after `/autodrive stop` or a timed command, run
`/autodrive manual`. When standing outside a car, vehicle commands select your nearest
unoccupied car within 12 blocks. You can also right-click your car to enter and resume manual driving.

If `/autodrive` is unknown, check that AutoDrive loaded in the correct Minecraft
instance. If permission is denied, enable commands/cheats or ask the server operator
for permission. If Test Grounds is unavailable, fully close and reopen the world
with the mod installed.

### Programmatic control

```mcfunction
/autodrive control 0 1 0 40
/autodrive control 0.25 0.5 0 20
/autodrive control 0 0 1 20
```

Arguments are **steering [-1,1], throttle [0,1], brake [0,1], duration [1,1200] ticks**.
Positive steering turns right. There are 20 ticks per second. When the duration
expires the car brakes and enters STOPPED mode. `/autodrive manual` returns control
to the driver. These are test commands, not a scripted autonomous driver.

Java integrations on the server can call:

```java
vehicle.applyControl(new VehicleControl(steering, throttle, brake), leaseTicks);
```

The server owns movement and uses Minecraft collision resolution. Manual controls
use vanilla player input packets, so no client movement authority is required.
The bicycle model and constants are documented in [docs/PHYSICS.md](docs/PHYSICS.md).

## Core code to understand before Phase 2

Read these in order. Track construction and rendering can wait.

| File | What to understand |
| --- | --- |
| [VehicleControl.java](src/main/java/com/sanjay/autodrive/vehicle/VehicleControl.java) | The shared action contract: steering [-1,1], throttle [0,1], brake [0,1]. `manual()` converts keyboard input into the same actions a future policy will produce. |
| [VehiclePhysics.java](src/main/java/com/sanjay/autodrive/vehicle/VehiclePhysics.java) | `step()` advances speed and heading by 0.05 seconds and returns horizontal displacement. This is the mathematical model the Python simulator needs to reproduce. |
| [VehicleEntity.java](src/main/java/com/sanjay/autodrive/entity/VehicleEntity.java) | `tick()` selects an action, calls the physics model, applies Minecraft movement/collisions, and synchronizes speed. `applyControl()` accepts timed external actions; `manual()` and `stop()` change modes. |
| [VehiclePhysicsTest.java](src/test/java/com/sanjay/autodrive/VehiclePhysicsTest.java) | Executable examples of acceleration, braking, drag, steering and the coordinate convention. Use these behaviors as simulator parity checks. |

The driving loop is:

```text
keyboard input or programmatic action
    → VehicleControl(steering, throttle, brake)
    → VehiclePhysics.step(previousState, action, offRoad)
    → VehicleEntity moves through Minecraft and resolves collisions
    → updated vehicle state
```

Physics speed is in **meters/second**; returned movement is in **blocks per tick**
(assuming one block equals one meter). Yaw is in **radians** inside the model and
**degrees** on the Minecraft entity. Yaw zero points toward +Z. Keep the 20 Hz step,
action limits, coordinate convention and physics coefficients consistent in Python.
See [docs/PHYSICS.md](docs/PHYSICS.md) for the complete contract.

Phase 2 adds a Gymnasium environment with position/speed/heading state, relative
observations, action stepping, rewards, reset behavior and episode termination.
Observations, rewards, the Python environment, PPO training and ONNX inference
are **not implemented yet**. Start by matching the existing vehicle dynamics in a
straight-road simulator; policy training follows in Phase 3.

## Development and verification

```sh
./gradlew build
./gradlew runClient
./gradlew runClientGameTest
```

`build` compiles both environments, runs 10 JUnit checks and runs the server GameTests
for real entity movement, manual inputs, timed command expiry and wall collisions.
The optional client smoke test creates a playable flat world, builds all three
circuits, exercises keyboard driving through the actual client/server connection
and captures screenshots. It opens a game window and needs a working graphics display.
The test-only mod is excluded from the shipped JAR.

See [docs/TESTING.md](docs/TESTING.md) for test coverage and [docs/ROADMAP.md](docs/ROADMAP.md)
for the remaining phases. The Fabric project remains CC0-1.0 licensed.
