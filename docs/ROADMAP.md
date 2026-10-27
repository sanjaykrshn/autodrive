# AutoDrive roadmap

## Phase 1 — Minecraft vehicle integration

- Fabric project repurposed as AutoDrive with its own identifier and namespaces.
- Rideable vehicle with manual acceleration, steering, braking and block collisions.
- Continuous server-side action interface and bounded-duration test commands.
- Three asymmetric closed circuits in a dedicated superflat test dimension.
- Basic speed/mode/lap HUD, checkpoints, reset and vehicle ownership.
- Physics/geometry unit tests, server entity GameTests and a client smoke test.

## Phase 2 — Driving simulation

Create the Gymnasium simulator with matching physics, a straight-road environment,
vehicle-relative observations, continuous actions, rewards, checkpoints and episode endings.

## Phase 3 — Reinforcement learning

Train Stable-Baselines3/PyTorch PPO on straight roads, extend to curves and complex
circuits, tune rewards and evaluate stability. Preserve Grand Prix Gardens as a
held-out layout until evaluation.

## Phase 4 — Minecraft deployment

Export the learned policy to ONNX, integrate ONNX Runtime Java, extract live
observations, apply model actions and validate transfer inside Minecraft.

## Phase 5 — Reliability and generalization

Run repeated trials with randomized starts and unseen layouts. Measure completion,
collision rate, lap time, deviation and smoothness. Diagnose simulation differences.

## Phase 6 — Extensions

Intersections, connected roads, traffic, moving obstacles, destinations, multiple
vehicles and experiments with imitation learning or richer perception.

Only completed behavior and experimentally measured results should be described as achieved.
