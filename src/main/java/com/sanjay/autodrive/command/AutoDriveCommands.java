package com.sanjay.autodrive.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sanjay.autodrive.AutoDrive;
import com.sanjay.autodrive.entity.VehicleEntity;
import com.sanjay.autodrive.track.TrackBuilder;
import com.sanjay.autodrive.track.TrackLayout;
import com.sanjay.autodrive.track.Tracks;
import com.sanjay.autodrive.vehicle.VehicleControl;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.Comparator;
import java.util.Set;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public final class AutoDriveCommands {
    private AutoDriveCommands() {}
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            literal("autodrive").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> message(c, "AutoDrive: /autodrive track build all | track go <name> | spawn | manual | stop | reset | control <steering> <throttle> <brake> <ticks> | status"))
                .then(literal("spawn").executes(AutoDriveCommands::spawn))
                .then(literal("manual").executes(c -> { var v = vehicle(c); if (v == null) return 0; v.manual(); return message(c,"Manual: movement keys steer/accelerate; back or jump brakes; sneak exits."); }))
                .then(literal("stop").executes(c -> { var v = vehicle(c); if (v == null) return 0; v.stop(); return message(c,"Vehicle stopped. /autodrive manual to resume."); }))
                .then(literal("reset").executes(c -> { var v = vehicle(c); if (v == null) return 0; var t = Tracks.get(v.trackId()); if (t == null) return fail(c,"Use /autodrive track go <name> first."); v.resetToTrack(t); return 1; }))
                .then(literal("status").executes(c -> { var v = vehicle(c); if (v == null) return 0; return message(c, String.format(java.util.Locale.ROOT,"%s | %.1f km/h | %s | lap %d | next checkpoint %d | collisions %d",v.mode(),v.speedMetersPerSecond()*3.6,v.trackId(),v.laps(),v.checkpoint(),v.collisionCount())); }))
                .then(literal("control").then(argument("steering",DoubleArgumentType.doubleArg(-1,1))
                    .then(argument("throttle",DoubleArgumentType.doubleArg(0,1))
                    .then(argument("brake",DoubleArgumentType.doubleArg(0,1))
                    .then(argument("ticks",IntegerArgumentType.integer(1,1200)).executes(c -> {
                        var v = vehicle(c); if (v == null) return 0;
                        v.applyControl(new VehicleControl(DoubleArgumentType.getDouble(c,"steering"),DoubleArgumentType.getDouble(c,"throttle"),DoubleArgumentType.getDouble(c,"brake")),IntegerArgumentType.getInteger(c,"ticks"));
                        return message(c,"Programmatic control applied; it will brake when the duration expires.");
                    }))))))
                .then(literal("track")
                    .then(literal("list").executes(c -> { Tracks.ALL.forEach(t -> message(c,t.id()+": "+t.title()+" — "+t.purpose())); return 1; }))
                    .then(literal("build").then(argument("name",StringArgumentType.word())
                        .suggests((c,b) -> { b.suggest("all"); Tracks.ALL.forEach(t -> b.suggest(t.id())); return b.buildFuture(); })
                        .executes(AutoDriveCommands::build)))
                    .then(literal("go").then(argument("name",StringArgumentType.word())
                        .suggests((c,b) -> { Tracks.ALL.forEach(t -> b.suggest(t.id())); return b.buildFuture(); })
                        .executes(AutoDriveCommands::go)))
                )
        ));
    }
    private static int build(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        var player = c.getSource().getPlayerOrException();
        if (c.getSource().getServer().getLevel(TrackBuilder.DIMENSION) == null) return fail(c,"Test Grounds dimension is unavailable; restart the world with AutoDrive installed.");
        String id = StringArgumentType.getString(c,"name");
        if (id.equals("all")) Tracks.ALL.forEach(t -> TrackBuilder.queue(t,player.getUUID()));
        else { var t = Tracks.get(id); if (t == null) return fail(c,"Unknown circuit. /autodrive track list"); TrackBuilder.queue(t,player.getUUID()); }
        return message(c,"Building in the AutoDrive superflat Test Grounds. You will receive a message when each circuit is ready.");
    }
    private static int go(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        TrackLayout track = Tracks.get(StringArgumentType.getString(c,"name"));
        if (track == null) return fail(c,"Unknown circuit. /autodrive track list");
        if (TrackBuilder.busy()) return fail(c,"Wait for circuit construction to finish before entering the grid.");
        ServerLevel level = c.getSource().getServer().getLevel(TrackBuilder.DIMENSION);
        if (level == null) return fail(c,"Restart the world with AutoDrive installed to load Test Grounds.");
        var start = track.samples().getFirst();
        if (!TrackBuilder.ready(level, track))
            return fail(c,"Build the tracks first: /autodrive track build all");
        ServerPlayer player = c.getSource().getPlayerOrException();
        VehicleEntity old = vehicleOrNull(player);
        if (old != null) old.stop();
        player.stopRiding();
        player.teleportTo(level,start.x()-3,TrackLayout.SURFACE_Y+1,start.z(),Set.of(),(float)Math.toDegrees(start.yaw()),0,true);
        VehicleEntity v = level.getEntitiesOfClass(VehicleEntity.class,new net.minecraft.world.phys.AABB(start.x()-12,TrackLayout.SURFACE_Y-3,start.z()-12,start.x()+12,TrackLayout.SURFACE_Y+6,start.z()+12))
            .stream().filter(car -> car.ownedBy(player) && !car.isVehicle()).findFirst().orElse(null);
        if (v == null) { v = new VehicleEntity(AutoDrive.VEHICLE, level); v.setOwner(player.getUUID()); v.resetToTrack(track); level.addFreshEntity(v); }
        else v.resetToTrack(track);
        player.startRiding(v);
        return message(c,track.title()+": movement keys accelerate/steer, back or jump brakes. /autodrive reset returns to this grid.");
    }
    private static int spawn(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        var player = c.getSource().getPlayerOrException();
        var level = player.level();
        var forward = player.getLookAngle().multiply(3,0,3);
        var v = new VehicleEntity(AutoDrive.VEHICLE,level);
        v.setOwner(player.getUUID()); v.setPos(player.getX()+forward.x,player.getY()+0.1,player.getZ()+forward.z); v.setYRot(player.getYRot());
        if (!level.noCollision(v)) return fail(c,"Make room for the vehicle in front of you.");
        if (!level.addFreshEntity(v)) return fail(c,"Could not spawn vehicle.");
        return message(c,"Vehicle spawned. Right-click to enter; sneak to leave.");
    }
    private static VehicleEntity vehicle(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        var v = vehicleOrNull(c.getSource().getPlayerOrException());
        if (v == null) fail(c,"Enter your vehicle or stand within 12 blocks of it.");
        return v;
    }
    private static VehicleEntity vehicleOrNull(ServerPlayer player) {
        if (player.getVehicle() instanceof VehicleEntity v && v.ownedBy(player)) return v;
        return player.level().getEntitiesOfClass(VehicleEntity.class,player.getBoundingBox().inflate(12)).stream()
                .filter(v -> v.ownedBy(player) && !v.isVehicle()).min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }
    private static int message(CommandContext<CommandSourceStack> c,String text) { c.getSource().sendSuccess(() -> Component.literal(text),false); return 1; }
    private static int fail(CommandContext<CommandSourceStack> c,String text) { c.getSource().sendFailure(Component.literal(text)); return 0; }
}
