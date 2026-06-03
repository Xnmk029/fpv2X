package com.iung.fpv20.mixin.client;

import com.iung.fpv20.Fpv20Client;
import com.iung.fpv20.config.Fpv20ConfigClientManual;
import com.iung.fpv20.flying.GlobalFlying;
import com.iung.fpv20.mixin_utils.IsFlying;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    @Inject(
            method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    public void onRender(AbstractClientPlayerEntity player, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (((IsFlying) player).get_is_flying() && Fpv20Client.config1.controlMode == Fpv20ConfigClientManual.ControlMode.SCHEME_B) {
            matrices.push();
            
            // 将无人机中心移到玩家碰撞箱的正中心，略微抬高以对齐
            matrices.translate(0.0f, 0.4f, 0.0f);
            
            // 应用无人机的 3D 旋转姿态
            if (player == MinecraftClient.getInstance().player) {
                // 本地玩家：使用精确物理的全局旋转 Quaternion
                matrices.multiply(new Quaternionf(GlobalFlying.G.droneRotation).conjugate());
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));
            } else {
                // 远端玩家：根据实体的 yaw / pitch 动态估算姿态
                float pYaw = player.getYaw();
                float pPitch = player.getPitch();
                org.joml.Quaternionf q = com.iung.fpv20.physics.PhysicsCore.from_ypr_deg(pYaw, pPitch, 0.0f);
                q.rotateLocalX(25.0f * com.iung.fpv20.utils.LocalMath.DEG_TO_RAD);
                matrices.multiply(q.conjugate());
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));
            }

            // 绘制 3D 无人机实体模型
            com.iung.fpv20.utils.DroneModelRenderer.renderDrone(
                matrices,
                vertexConsumers,
                light,
                net.minecraft.client.render.OverlayTexture.DEFAULT_UV,
                Fpv20Client.config1.drone.frameIndex,
                Fpv20Client.config1.drone.motorIndex,
                Fpv20Client.config1.drone.batteryIndex,
                Fpv20Client.config1.drone.cameraIndex,
                Fpv20Client.config1.drone.selectedPropDia,
                Fpv20Client.config1.drone.selectedPropPitch,
                GlobalFlying.G.clientPropRotation
            );
            
            matrices.pop();
            
            // 拦截原始玩家模型的渲染，实现无缝隐藏与完全 3D 无人机实体替换！
            ci.cancel();
        }
    }
}
