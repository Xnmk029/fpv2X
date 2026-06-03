package com.iung.fpv20.mixin.client;

import com.iung.fpv20.Fpv20;
import com.iung.fpv20.Fpv20Client;
import com.iung.fpv20.flying.GlobalFlying;
import com.iung.fpv20.input.Controller;
import com.iung.fpv20.utils.FastMath;
import com.iung.fpv20.utils.DroneModelRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.render.GameRenderer.class)
public abstract class GameRendererMixin implements com.iung.fpv20.mixin_utils.GameRendererAccessor {
    @Shadow
    @Final
    private Camera camera;

    @Shadow
    public abstract void loadPostProcessor(net.minecraft.util.Identifier id);

    @Shadow
    public abstract void disablePostProcessor();

    @Override
    public void fpv20$loadPostProcessor(net.minecraft.util.Identifier id) {
        this.loadPostProcessor(id);
    }

    @Override
    public void fpv20$disablePostProcessor() {
        this.disablePostProcessor();
    }

    private static long last_render = System.nanoTime();


//    @Inject(
//            method = "tick",
//            at = @At(
//                    value = "HEAD"
//            )
//    )
//    public void tick(CallbackInfo ci) {
//        last_time_tickDelta = 0.0f;
//    }

    @Inject(
            method = "renderWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/RotationAxis;rotationDegrees(F)Lorg/joml/Quaternionf;",
                    ordinal = 2
            )
    )
    public void mixin(float tickDelta, long limitTime, MatrixStack matrix, CallbackInfo ci) {

        if (GlobalFlying.getFlying()) {
            if (Fpv20Client.config1.free_camera_pitch) {
                matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            }
            if (Fpv20Client.config1.free_camera_yaw) {
                matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));

            }
        }

        long time_start = System.nanoTime();


        MinecraftClient client = MinecraftClient.getInstance();

//        float dt = (float) ((tickDelta - Fpv20Client.last_time_tickDelta.get()) * 0.05f);
        float dt = (float) (time_start - last_render) / 1000_000_000f;
        last_render = time_start;
        // if fps is lower than 10, just use another computer
        if (dt < 0) {
            ((String) null).isBlank();
        }
        dt = FastMath.clamp(dt, 0, 0.1f);


//        if (Fpv20Client.last_time_tickDelta>tickDelta){
//
//        }
        Controller controller = Fpv20Client.controller;
        if (controller != null) {
            controller.poll();
        }
        if (client != null) {
            GlobalFlying.G.handle_flying_rotate(client, dt);
            long time_end = System.nanoTime();
            Fpv20.LOGGER.debug("event_time {}", time_end - time_start);

        }

//        this_time_tickDelta = tickDelta;
//        GlobalFlying.G.lastCamRoll += tickDelta / 20f * GlobalFlying.G.angular_speed;
        if (GlobalFlying.getFlying()) {
//            matrix.multiply(GlobalFlying.G.cacl_cam_rotation_last().nlerp(GlobalFlying.G.cacl_cam_rotation(), tickDelta));
            matrix.multiply(GlobalFlying.G.cacl_cam_rotation());

        }

//        Matrix4f mat = new Matrix4f();
//
//        Quaternionf r = RotationAxis.POSITIVE_Z.rotationDegrees(GlobalFlying.G.lastCamRoll);
//        r.getEulerAnglesXYZ()

        //        r.mul()
//        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
//        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));


    }

//    @Inject(
//            method = "renderWorld",
//            at = @At(
//                    value = "INVOKE",
//                    target = "",
//                    ordinal = 2
//            )
//    )
//    public void mixin(float tickDelta, long limitTime, MatrixStack matrix, CallbackInfo ci) {
//
//    }

    @Redirect(
            method = "renderWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionf;)V",
                    ordinal = 2
            )
    )
    public void mixin1(MatrixStack instance, Quaternionf quaternion) {
        if (GlobalFlying.getFlying()) {

        } else {
            instance.multiply(quaternion);
        }
    }

    @Redirect(
            method = "renderWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionf;)V",
                    ordinal = 3
            )
    )
    public void mixin12(MatrixStack instance, Quaternionf quaternion) {
        if (GlobalFlying.getFlying()) {

        } else {
            instance.multiply(quaternion);
        }
    }

    @Inject(
            method = "renderHand",
            at = @At("HEAD"), cancellable = true
    )
    public void fpv20_renderDroneInFirstPerson(MatrixStack matrices, Camera camera, float tickDelta, CallbackInfo ci) {
        if (GlobalFlying.getFlying()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!client.options.getPerspective().isFirstPerson()) {
                ci.cancel();
                return;
            }
            if (client.player != null) {
                matrices.push();

                org.joml.Quaternionf q = new org.joml.Quaternionf(GlobalFlying.G.droneRotation).conjugate();
                matrices.multiply(q);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));
                matrices.translate(0.0f, -0.15f, -0.30f);

                VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
                int light = 0xF000F0;
                DroneModelRenderer.renderDrone(
                        matrices,
                        immediate,
                        light,
                        OverlayTexture.DEFAULT_UV,
                        Fpv20Client.config1.drone.frameIndex,
                        Fpv20Client.config1.drone.motorIndex,
                        Fpv20Client.config1.drone.batteryIndex,
                        Fpv20Client.config1.drone.cameraIndex,
                        Fpv20Client.config1.drone.selectedPropDia,
                        Fpv20Client.config1.drone.selectedPropPitch,
                        GlobalFlying.G.clientPropRotation
                );
                immediate.draw();

                matrices.pop();
            }
            ci.cancel();
        }
    }


}

