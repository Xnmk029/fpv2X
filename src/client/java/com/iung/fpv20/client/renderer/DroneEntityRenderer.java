package com.iung.fpv20.client.renderer;

import com.iung.fpv20.entity.DroneEntity;
import com.iung.fpv20.flying.GlobalFlying;
import com.iung.fpv20.utils.DroneModelRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import net.minecraft.util.math.MathHelper;

public class DroneEntityRenderer extends EntityRenderer<DroneEntity> {

    public DroneEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(DroneEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        // 1. Center the model in the entity's hitbox.
        matrices.translate(0.0f, 0.1f, 0.0f);

        // 2. Compute rotation based on the physical state.
        org.joml.Quaternionf q;
        net.minecraft.client.network.ClientPlayerEntity localPlayer = net.minecraft.client.MinecraftClient.getInstance().player;
        if (entity.getPilotUuid() != null && localPlayer != null && entity.getPilotUuid().equals(localPlayer.getUuid())) {
            q = new org.joml.Quaternionf(GlobalFlying.G.droneRotation).conjugate();
        } else {
            q = entity.getQuaternion().conjugate();
        }
        matrices.multiply(q);
        // Align Blockbench model forward (+Z) with Minecraft/OpenGL forward (-Z)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));

        // 3. Render 3D Drone components
        float propAngle = 0f;
        if (entity.getPilotUuid() != null) {
            propAngle = GlobalFlying.G.clientPropRotation;
        } else {
            propAngle = (float) (((entity.getWorld().getTime() + tickDelta) * 80.0) % 360.0);
        }

        DroneModelRenderer.renderDrone(
                matrices,
                vertexConsumers,
                light,
                OverlayTexture.DEFAULT_UV,
                entity.getFrameIndex(),
                entity.getMotorIndex(),
                entity.getBatteryIndex(),
                entity.getCameraIndex(),
                entity.getPropDia(),
                entity.getPropPitch(),
                propAngle
        );

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(DroneEntity entity) {
        return new Identifier("fpv20", "textures/item/drone_carbon.png");
    }
}
