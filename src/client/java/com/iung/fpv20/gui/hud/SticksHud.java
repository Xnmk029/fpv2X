package com.iung.fpv20.gui.hud;

import com.iung.fpv20.Fpv20Client;
import com.iung.fpv20.flying.GlobalFlying;
import com.iung.fpv20.input.Controller;
import com.iung.fpv20.utils.Utils;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public class SticksHud implements HudRenderCallback {
    private ValueProvider t;
    private ValueProvider y;
    private ValueProvider p;
    private ValueProvider r;
    private int size;
    private int padding_between;
    private int padding_down = 10;
    private static final int WHITE = 0Xffffffff;

    public SticksHud() {
        this.size = 40;
        this.padding_between = 10;
        this.padding_down = 20;


        this.t = () -> Utils.requireNonNullOr(Fpv20Client.controller, c -> c.get_calibrated_value_no_rate(c.get_channel_id("t")), 0f);
        this.y = () -> Utils.requireNonNullOr(Fpv20Client.controller, c -> c.get_calibrated_value_no_rate(c.get_channel_id("y")), 0f);
        this.p = () -> Utils.requireNonNullOr(Fpv20Client.controller, c -> c.get_calibrated_value_no_rate(c.get_channel_id("p")), 0f);
        this.r = () -> Utils.requireNonNullOr(Fpv20Client.controller, c -> c.get_calibrated_value_no_rate(c.get_channel_id("r")), 0f);
    }


    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        if (!Fpv20Client.config.show_osd()) {
            return;
        }

        if (!GlobalFlying.getFlying()) {
            return;
        }
//        int size = 100;
//        int padding_between = 10;
//        int padding_down = 10;


        int window_width = drawContext.getScaledWindowWidth();
        int window_height = drawContext.getScaledWindowHeight();

        int start_x_1 = window_width / 2 - padding_between / 2 - size;
        int start_x_2 = window_width / 2 + padding_between / 2;

        int start_y = window_height - padding_down - size;


//        drawContext.fill(start_x_1, start_y, start_x_1 + size, start_y + size, WHITE);
//        drawContext.fill(start_x_2, start_y, start_x_2 + size, start_y + size, WHITE);

        drawContext.drawVerticalLine(start_x_1 + size / 2, start_y, start_y + size, WHITE);
        drawContext.drawVerticalLine(start_x_2 + size / 2, start_y, start_y + size, WHITE);

        drawContext.drawHorizontalLine(start_x_1, start_x_1 + size, start_y + size / 2, WHITE);
        drawContext.drawHorizontalLine(start_x_2, start_x_2 + size, start_y + size / 2, WHITE);

        fill_centered(drawContext, start_x_1 + size / 2 + y(), start_y + size / 2 - t(), 2, WHITE);
        fill_centered(drawContext, start_x_2 + size / 2 + r(), start_y + size / 2 - p(), 2, WHITE);

        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.textRenderer != null) {
            String modeName = "ACRO";
            if (Fpv20Client.controller != null) {
                try {
                    if (Fpv20Client.controller.get_value_by_name("angle") > 0.5f) {
                        modeName = "ANGLE";
                    } else if (Fpv20Client.controller.get_value_by_name("3d") > 0.5f) {
                        modeName = "3D";
                    }
                } catch (Exception ignored) {}
            }
            String modeText = modeName;

            double speedMS = 0;
            double gValue = 1.0;
            com.iung.fpv20.physics.Drone drone = GlobalFlying.G.getDrone();
            if (drone != null) {
                speedMS = drone.get_speed().length();
                Vec3d acc = drone.get_acceleration();
                if (acc != null) {
                    gValue = acc.add(0, 9.8, 0).length() / 9.8;
                }
            }

            int speedMode = Fpv20Client.config.getSpeedDisplayMode();
            String speedText = "";
            if (speedMode == 1) {
                speedText = String.format("%.1f b/s", speedMS);
            } else {
                double speedKBH = speedMS * 3.6;
                speedText = String.format("%.1f kb/h", speedKBH);
            }

            String gForceText = String.format("%.2f G", gValue);
            String cameraAngleText = String.format("%.0f°", Fpv20Client.config1.getCamera_angle());

            int margin_x = 35;
            int text_y = start_y + size / 2 - 4;

            drawContext.drawTextWithShadow(mc.textRenderer, modeText, margin_x, text_y, WHITE);

            int modeWidth = mc.textRenderer.getWidth(modeText);
            int drawX_speed = margin_x + modeWidth + 15;
            drawContext.drawTextWithShadow(mc.textRenderer, speedText, drawX_speed, text_y, WHITE);

            int cameraAngleWidth = mc.textRenderer.getWidth(cameraAngleText);
            int drawX_cameraAngle = window_width - margin_x - cameraAngleWidth;
            drawContext.drawTextWithShadow(mc.textRenderer, cameraAngleText, drawX_cameraAngle, text_y, WHITE);

            int gForceWidth = mc.textRenderer.getWidth(gForceText);
            int drawX_gForce = drawX_cameraAngle - gForceWidth - 15;
            drawContext.drawTextWithShadow(mc.textRenderer, gForceText, drawX_gForce, text_y, WHITE);
        }

        // Live Telemetry Logging for Debugging
        if (Fpv20Client.config1.show_telemetry_debug) {
            if (mc.textRenderer != null) {
                int logY = 10;
                int logX = 10;
                
                // Background panel for telemetry
                drawContext.fill(logX - 4, logY - 4, logX + 270, logY + 96, 0x99000000);
                
                drawContext.drawTextWithShadow(mc.textRenderer, "=== FPV2X TELEMETRY DEBUG LOG ===", logX, logY, 0xFF00FFFF);
                logY += 12;
                
                // 1. Drone Rotation Quaternion (World rotation matrix)
                org.joml.Quaternionf qWorld = GlobalFlying.G.droneRotation;
                if (qWorld != null) {
                    drawContext.drawTextWithShadow(mc.textRenderer, String.format("Q_world: [%.3f, %.3f, %.3f, %.3f]", qWorld.x, qWorld.y, qWorld.z, qWorld.w), logX, logY, 0xFFFFFFFF);
                    logY += 10;
                    // Physical rotation (Conjugate)
                    org.joml.Quaternionf qPhys = new org.joml.Quaternionf(qWorld).conjugate();
                    drawContext.drawTextWithShadow(mc.textRenderer, String.format("Q_phys:  [%.3f, %.3f, %.3f, %.3f]", qPhys.x, qPhys.y, qPhys.z, qPhys.w), logX, logY, 0xFFFFFFFF);
                    logY += 10;
                    
                    // Euler Angles extracted from Q_phys
                    org.joml.Vector3f ypr = com.iung.fpv20.physics.PhysicsCore.from_quaternion_to_ypr_deg(qPhys);
                    drawContext.drawTextWithShadow(mc.textRenderer, String.format("YPR_phys: Yaw=%.1f, Pitch=%.1f, Roll=%.1f", ypr.x, ypr.y, ypr.z), logX, logY, 0xFFFFFF00);
                    logY += 10;
                }
                
                // 2. Active Drone Entity info
                if (GlobalFlying.G.activeClientDroneEntity != null) {
                    com.iung.fpv20.entity.DroneEntity droneEnt = GlobalFlying.G.activeClientDroneEntity;
                    net.minecraft.util.math.Vec3d pos = droneEnt.getPos();
                    org.joml.Quaternionf qEnt = droneEnt.getQuaternion();
                    drawContext.drawTextWithShadow(mc.textRenderer, String.format("Pos: [%.2f, %.2f, %.2f]", pos.x, pos.y, pos.z), logX, logY, 0xFFFFFFFF);
                    logY += 10;
                    drawContext.drawTextWithShadow(mc.textRenderer, String.format("Q_entity: [%.3f, %.3f, %.3f, %.3f]", qEnt.x, qEnt.y, qEnt.z, qEnt.w), logX, logY, 0xFF55FFFF);
                    logY += 10;
                } else {
                    drawContext.drawTextWithShadow(mc.textRenderer, "Active Drone Entity: NULL (Scheme B)", logX, logY, 0xFFFF5555);
                    logY += 10;
                }
                
                // 3. Camera Angle Config
                drawContext.drawTextWithShadow(mc.textRenderer, String.format("Camera Angle: %.1f deg", Fpv20Client.config1.getCamera_angle()), logX, logY, 0xFFAAAAAA);
                logY += 10;
            }
        }
    }


    int t() {
        return Fpv20Client.config1.throttle_display_in_center ?
                Math.round(this.t.get() * size / 2f)
                : Math.round(this.t.get() * size - size / 2f);
    }

    int y() {
        return Math.round(this.y.get() * size / 2);
    }

    int p() {
        return Math.round(this.p.get() * size / 2);
    }

    int r() {
        return Math.round(this.r.get() * size / 2);
    }

    static void fill_centered(DrawContext drawContext, int x, int y, int r, int color) {
        drawContext.fill(x - r, y - r, x + r, y + r, color);
    }


    @FunctionalInterface
    public interface ValueProvider {
        float get();
    }
}
