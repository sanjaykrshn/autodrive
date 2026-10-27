# AutoDrive

A Minecraft Fabric vehicle platform for a reinforcement learning driving project.
**Phase 1 implements the Minecraft vehicle and test environment.** PPO training,
Python simulation and ONNX inference are planned; this version does not include an autonomous policy.

## Play

Use Minecraft **26.3**, Java **25 or newer**, Fabric Loader **0.19.5 or newer** and
Fabric API **0.161.0+26.3**. Put the AutoDrive JAR and Fabric API in the game's `mods` folder.
AutoDrive must be installed on both client and server. Commands require cheats/operator permission.

Open a world, then run:

```mcfunction
/autodrive track build all
```

Wait for the three completion messages, then enter a circuit:

```mcfunction
/autodrive track go rookie
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
