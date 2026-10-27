package com.sanjay.autodrive.entity;

import com.sanjay.autodrive.track.TrackLayout;
import com.sanjay.autodrive.track.Tracks;
import com.sanjay.autodrive.vehicle.VehicleControl;
import com.sanjay.autodrive.vehicle.VehiclePhysics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

public final class VehicleEntity extends Entity {
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> MODE = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> TRACK = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> LAP = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHECKPOINT = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private UUID owner;
    private double speed;
    private VehicleControl programmatic = VehicleControl.STOP;
    private int controlTicks;
    private int nextCheckpoint = 1;
    private int collisionCount;

    public VehicleEntity(EntityType<? extends VehicleEntity> type, Level level) { super(type, level); blocksBuilding = true; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SPEED, 0f); builder.define(MODE, "MANUAL"); builder.define(TRACK, "");
        builder.define(LAP, 0); builder.define(CHECKPOINT, 1);
    }
    @Override protected InterpolationHandler createInterpolationHandler() { return net.minecraft.world.entity.LinearInterpolationHandler.create(this); }
    public float speedMetersPerSecond() { return entityData.get(SPEED); }
    public String mode() { return entityData.get(MODE); }
    public String trackId() { return entityData.get(TRACK); }
    public int laps() { return entityData.get(LAP); }
    public int checkpoint() { return entityData.get(CHECKPOINT); }
    public int collisionCount() { return collisionCount; }
    public void setOwner(UUID owner) { this.owner = owner; }
    public boolean ownedBy(Player player) { return owner == null || owner.equals(player.getUUID()); }

    /** Server-side interface for commands and a future learned policy. Lease expiry always brakes. */
    public void applyControl(VehicleControl control, int ticks) {
        if (level().isClientSide()) throw new IllegalStateException("Vehicle controls must run on the server");
        programmatic = control;
        controlTicks = Math.clamp(ticks, 1, 1200);
        entityData.set(MODE, "PROGRAMMATIC");
    }
    public void manual() { controlTicks = 0; entityData.set(MODE, "MANUAL"); }
    public void stop() { controlTicks = 0; programmatic = VehicleControl.STOP; speed = 0; setDeltaMovement(Vec3.ZERO); entityData.set(SPEED, 0f); entityData.set(MODE, "STOPPED"); }
    public void resetToTrack(TrackLayout track) {
        stop();
        TrackLayout.Sample start = track.samples().getFirst();
        teleportTo(start.x(), TrackLayout.SURFACE_Y + 0.05, start.z());
        setYRot((float) Math.toDegrees(start.yaw())); setXRot(0);
        entityData.set(TRACK, track.id()); entityData.set(LAP, 0); entityData.set(CHECKPOINT, 1);
        nextCheckpoint = 1; collisionCount = 0;
        manual();
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        VehicleControl action = VehicleControl.STOP;
        if (mode().equals("PROGRAMMATIC")) {
            if (controlTicks-- > 0) action = programmatic;
            else entityData.set(MODE, "STOPPED");
        } else if (mode().equals("MANUAL") && getFirstPassenger() instanceof ServerPlayer driver && ownedBy(driver)) {
            var input = driver.getLastClientInput();
            action = VehicleControl.manual(input.forward(), input.backward(), input.left(), input.right(), input.jump());
        }
        var below = level().getBlockState(blockPosition().below());
        boolean onRoad = below.is(Blocks.CONCRETE.gray()) || below.is(Blocks.CONCRETE.white())
                || below.is(Blocks.CONCRETE.black()) || below.is(Blocks.CONCRETE.red());
        VehiclePhysics.Step step = VehiclePhysics.step(new VehiclePhysics.State(speed, Math.toRadians(getYRot())), action, !onRoad);
        speed = step.state().speed();
        if (onGround()) setYRot((float) Math.toDegrees(step.state().yaw()));
        double gravity = isNoGravity() ? 0 : 0.08;
        double vy = onGround() ? -gravity : Math.max(-3, getDeltaMovement().y - gravity);
        Vec3 movement = new Vec3(step.dx(), vy, step.dz());
        setDeltaMovement(movement);
        move(MoverType.SELF, movement);
        if (horizontalCollision) { if (speed > 0.5) collisionCount++; speed = 0; setDeltaMovement(0, getDeltaMovement().y, 0); }
        if (isInWater() || isInLava()) stop();
        entityData.set(SPEED, (float) speed);
        updateCheckpoints();
    }
    private void updateCheckpoints() {
        TrackLayout track = Tracks.get(trackId());
        if (track == null || level().dimension() != com.sanjay.autodrive.track.TrackBuilder.DIMENSION) return;
        var checkpoints = com.sanjay.autodrive.track.TrackBuilder.checkpoints(track);
        TrackLayout.Sample target = checkpoints.get(nextCheckpoint);
        if (Math.abs(getY() - TrackLayout.SURFACE_Y) > 3 || Math.hypot(getX() - target.x(), getZ() - target.z()) > TrackLayout.ROAD_HALF_WIDTH) return;
        nextCheckpoint = (nextCheckpoint + 1) % checkpoints.size();
        if (nextCheckpoint == 1) {
            entityData.set(LAP, laps() + 1);
            if (getFirstPassenger() instanceof ServerPlayer player) player.sendSystemMessage(Component.literal("AutoDrive: lap " + laps() + " completed."));
        }
        entityData.set(CHECKPOINT, nextCheckpoint);
    }
    @Override public boolean isPickable() { return true; }
    @Override public boolean canBeCollidedWith(Entity other) { return !hasPassenger(other); }
    @Override public boolean canCollideWith(Entity other) { return !hasPassenger(other) && other.canBeCollidedWith(this); }
    @Override protected boolean canAddPassenger(Entity entity) { return getPassengers().isEmpty() && entity instanceof Player; }
    @Override public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!ownedBy(player)) {
            if (!level().isClientSide()) player.sendSystemMessage(Component.literal("This vehicle belongs to another driver."));
            return InteractionResult.FAIL;
        }
        if (!level().isClientSide() && !player.isShiftKeyDown()) { player.startRiding(this); manual(); }
        return InteractionResult.SUCCESS;
    }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && player.isCreative() && ownedBy(player)) { ejectPassengers(); discard(); return true; }
        return false;
    }
    @Override protected void readAdditionalSaveData(ValueInput input) {
        try { owner = UUID.fromString(input.getStringOr("Owner", "")); } catch (IllegalArgumentException ignored) { owner = null; }
        entityData.set(TRACK, input.getStringOr("Track", ""));
        // Reloading never resumes stale throttle commands.
        stop();
    }
    @Override protected void addAdditionalSaveData(ValueOutput output) {
        if (owner != null) output.putString("Owner", owner.toString());
        output.putString("Track", trackId());
    }
}
