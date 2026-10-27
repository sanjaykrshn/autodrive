package com.sanjay.autodrive.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sanjay.autodrive.entity.VehicleEntity;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Compact open cockpit car made from native model geometry and vanilla materials. */
public final class VehicleRenderer extends EntityRenderer<VehicleEntity,VehicleRenderer.State> {
    public static final class State extends EntityRenderState { public float yaw; }
    private record Part(Model<State> model, Identifier texture) {}
    private final java.util.List<Part> parts;
    public VehicleRenderer(EntityRendererProvider.Context context) {
        super(context); shadowRadius = 1.1f;
        parts = java.util.List.of(
            part("blue_concrete", new float[][]{{-11,-10,-19,22,6,38},{-9,-14,6,18,4,10},{-10,-12,-18,20,2,10}}),
            part("black_concrete",new float[][]{{-14,-7,-13,5,7,8},{9,-7,-13,5,7,8},{-14,-7,9,5,7,8},{9,-7,9,5,7,8},{-6,-14,-1,12,4,8}}),
            part("light_blue_concrete",new float[][]{{-9,-19,-6,18,7,2}}),
            part("yellow_concrete",new float[][]{{-9,-9,-19.2f,5,3,1},{4,-9,-19.2f,5,3,1}}),
            part("red_concrete",new float[][]{{-9,-9,18.2f,5,3,1},{4,-9,18.2f,5,3,1}})
        );
    }
    private static Part part(String material,float[][] cubes) {
        MeshDefinition mesh = new MeshDefinition();
        CubeListBuilder builder = CubeListBuilder.create();
        for (float[] c : cubes) builder.texOffs(0,0).addBox(c[0],c[1],c[2],c[3],c[4],c[5]);
        mesh.getRoot().addOrReplaceChild("body",builder,PartPose.ZERO);
        var root = LayerDefinition.create(mesh,16,16).bakeRoot();
        Model<State> model = new Model<State>(root,RenderTypes::entitySolid) {};
        return new Part(model,Identifier.withDefaultNamespace("textures/block/"+material+".png"));
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(VehicleEntity v,State state,float partialTick) {
        super.extractRenderState(v,state,partialTick); state.yaw = v.getYRot(partialTick);
    }
    @Override public void submit(State state,PoseStack poses,SubmitNodeCollector collector,CameraRenderState camera) {
        poses.pushPose();
        // Model forward is -Z; Minecraft yaw zero points +Z.
        poses.rotateDegrees(Axis.YP,180-state.yaw);
        poses.scale(-1,-1,1);
        for (Part part : parts) collector.submitModel(part.model,state,poses,part.texture,state.lightCoords,OverlayTexture.NO_OVERLAY,state.outlineColor);
        poses.popPose();
        super.submit(state,poses,collector,camera);
    }
}
