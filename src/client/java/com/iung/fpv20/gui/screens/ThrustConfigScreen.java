package com.iung.fpv20.gui.screens;

import com.iung.fpv20.Fpv20Client;
import com.iung.fpv20.flying.GlobalFlying;
import com.iung.fpv20.mixin_utils.IsFlying;
import com.iung.fpv20.network.DroneFlyPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.util.math.RotationAxis;

public class ThrustConfigScreen extends BackableScreen {

    public static class FramePreset {
        public String name;
        public float weight; // in grams
        public float maxProp; // in inches
        
        public FramePreset(String name, float weight, float maxProp) {
            this.name = name;
            this.weight = weight;
            this.maxProp = maxProp;
        }
    }

    public static class MotorPreset {
        public String name;
        public float kv;
        public float weight; // in grams
        
        public MotorPreset(String name, float kv, float weight) {
            this.name = name;
            this.kv = kv;
            this.weight = weight;
        }
    }

    public static class BatteryPreset {
        public String name;
        public int cells;
        public float weight; // in grams
        
        public BatteryPreset(String name, int cells, float weight) {
            this.name = name;
            this.cells = cells;
            this.weight = weight;
        }
    }

    public static class CameraPreset {
        public String name;
        public float weight; // in grams
        
        public CameraPreset(String name, float weight) {
            this.name = name;
            this.weight = weight;
        }
    }

    public static final FramePreset[] FRAMES = {
        new FramePreset("2\" Whoop (室内微型)", 12f, 2.0f),
        new FramePreset("3\" Cinewhoop (运镜圈速)", 65f, 3.1f),
        new FramePreset("5\" Freestyle (花飞竞速)", 115f, 5.2f),
        new FramePreset("7\" LongRange (载重远航)", 185f, 7.2f)
    };

    public static final MotorPreset[] MOTORS = {
        new MotorPreset("1204 (5000 KV)", 5000f, 6.5f),
        new MotorPreset("1404 (3800 KV)", 3800f, 9.0f),
        new MotorPreset("2207 (1950 KV - 6S)", 1950f, 32.0f),
        new MotorPreset("2207 (2750 KV - 4S)", 2750f, 32.0f),
        new MotorPreset("2807 (1300 KV - 6S)", 1300f, 52.0f)
    };

    public static final BatteryPreset[] BATTERIES = {
        new BatteryPreset("3S (450 mAh)", 3, 45f),
        new BatteryPreset("4S (850 mAh)", 4, 95f),
        new BatteryPreset("4S (1300 mAh)", 4, 155f),
        new BatteryPreset("6S (1300 mAh)", 6, 225f),
        new BatteryPreset("6S (1800 mAh)", 6, 290f)
    };

    public static final CameraPreset[] CAMERAS = {
        new CameraPreset("无外部挂载 (None)", 0f),
        new CameraPreset("剥离版 GoPro (30g)", 30f),
        new CameraPreset("标准版 GoPro (120g)", 120f)
    };

    private int currentFrameIndex;
    private int currentMotorIndex;
    private int currentBatteryIndex;
    private int currentCameraIndex;
    private float currentPropDia;
    private float currentPropPitch;
    private boolean localLinear;
    private float droneYaw = 0f;
    private float propRotation = 0f;

    private ButtonWidget frameBtn;
    private ButtonWidget motorBtn;
    private ButtonWidget batteryBtn;
    private ButtonWidget cameraBtn;
    private ButtonWidget modelBtn;
    private ButtonWidget propDiaIndicator;
    private ButtonWidget propPitchIndicator;

    public ThrustConfigScreen(Screen parent) {
        super(Text.literal("Thrust Config Screen"), parent);
        this.localLinear = Fpv20Client.config1.drone.linearAcceleration;
        this.currentFrameIndex = Fpv20Client.config1.drone.frameIndex;
        this.currentMotorIndex = Fpv20Client.config1.drone.motorIndex;
        this.currentBatteryIndex = Fpv20Client.config1.drone.batteryIndex;
        this.currentCameraIndex = Fpv20Client.config1.drone.cameraIndex;
        this.currentPropDia = Fpv20Client.config1.drone.selectedPropDia;
        this.currentPropPitch = Fpv20Client.config1.drone.selectedPropPitch;

        clampPropDiameter();
    }

    private void clampPropDiameter() {
        float maxAllowed = FRAMES[currentFrameIndex].maxProp;
        if (currentPropDia > maxAllowed) {
            currentPropDia = maxAllowed;
        }
    }

    @Override
    public boolean hasOkButton() {
        return true;
    }

    @Override
    protected void init() {
        super.init();

        int i = this.width / 2 - 230;
        int j = this.width / 2 - 110;
        int y = 20;
        int width = 140;
        int height = 20;
        int step = 22;

        // Reposition OK & Cancel buttons from BackableScreen to avoid overlapping the 3D preview
        if (this.okButton != null) {
            this.okButton.setX(i);
            this.okButton.setY(this.height - 25);
            this.okButton.setWidth(width);
        }
        if (this.cancelButton != null) {
            this.cancelButton.setX(j);
            this.cancelButton.setY(this.height - 25);
            this.cancelButton.setWidth(width);
        }

        // Title
        this.addDrawableChild(new TextWidget(i, y, width * 2 + 10, height, Text.literal("穿越机配件选型与物理累计调参 (Uncrashed Tuning)"), this.textRenderer));
        y += 24;

        // 1. Model Toggle
        this.addDrawableChild(new TextWidget(i, y, width, height, Text.literal("推力模型选择:"), this.textRenderer));
        modelBtn = this.addDrawableChild(ButtonWidget.builder(
            Text.literal(localLinear ? "线性模型 (Linear)" : "叶片气动 (Propeller)"),
            (btn) -> {
                localLinear = !localLinear;
                updateButtonLabels();
            }
        ).dimensions(j, y, width, height).build());
        y += step;

        // 2. Frame Size
        this.addDrawableChild(new TextWidget(i, y, width, height, Text.literal("机架尺寸 (Frame):"), this.textRenderer));
        frameBtn = this.addDrawableChild(ButtonWidget.builder(
            Text.literal(FRAMES[currentFrameIndex].name),
            (btn) -> {
                currentFrameIndex = (currentFrameIndex + 1) % FRAMES.length;
                clampPropDiameter();
                updateButtonLabels();
            }
        ).dimensions(j, y, width, height).build());
        y += step;

        // 3. Motor Selection
        this.addDrawableChild(new TextWidget(i, y, width, height, Text.literal("电机选型 (Motor):"), this.textRenderer));
        motorBtn = this.addDrawableChild(ButtonWidget.builder(
            Text.literal(MOTORS[currentMotorIndex].name),
            (btn) -> {
                currentMotorIndex = (currentMotorIndex + 1) % MOTORS.length;
                updateButtonLabels();
            }
        ).dimensions(j, y, width, height).build());
        y += step;

        // 4. Battery Selection
        this.addDrawableChild(new TextWidget(i, y, width, height, Text.literal("电池规格 (Battery):"), this.textRenderer));
        batteryBtn = this.addDrawableChild(ButtonWidget.builder(
            Text.literal(BATTERIES[currentBatteryIndex].name),
            (btn) -> {
                currentBatteryIndex = (currentBatteryIndex + 1) % BATTERIES.length;
                updateButtonLabels();
            }
        ).dimensions(j, y, width, height).build());
        y += step;

        // 5. Camera Selection
        this.addDrawableChild(new TextWidget(i, y, width, height, Text.literal("挂载相机 (Payload):"), this.textRenderer));
        cameraBtn = this.addDrawableChild(ButtonWidget.builder(
            Text.literal(CAMERAS[currentCameraIndex].name),
            (btn) -> {
                currentCameraIndex = (currentCameraIndex + 1) % CAMERAS.length;
                updateButtonLabels();
            }
        ).dimensions(j, y, width, height).build());
        y += step;

        // 6. Prop Diameter Row
        this.addDrawableChild(new TextWidget(i, y, width, height, Text.literal("桨叶直径 (propDiameter):"), this.textRenderer));
        this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), (btn) -> {
            currentPropDia = Math.max(1.0f, (float) Math.round((currentPropDia - 0.1f) * 10f) / 10f);
            clampPropDiameter();
            updateButtonLabels();
        }).dimensions(j, y, 25, height).build());
        
        propDiaIndicator = ButtonWidget.builder(Text.literal(currentPropDia + "\""), (btn) -> {})
            .dimensions(j + 30, y, 80, height).build();
        propDiaIndicator.active = false;
        this.addDrawableChild(propDiaIndicator);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), (btn) -> {
            currentPropDia = Math.min(FRAMES[currentFrameIndex].maxProp, (float) Math.round((currentPropDia + 0.1f) * 10f) / 10f);
            clampPropDiameter();
            updateButtonLabels();
        }).dimensions(j + 115, y, 25, height).build());
        y += step;

        // 7. Prop Pitch Row
        this.addDrawableChild(new TextWidget(i, y, width, height, Text.literal("桨叶螺距 (propPitch):"), this.textRenderer));
        this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), (btn) -> {
            currentPropPitch = Math.max(1.0f, (float) Math.round((currentPropPitch - 0.1f) * 10f) / 10f);
            updateButtonLabels();
        }).dimensions(j, y, 25, height).build());
        
        propPitchIndicator = ButtonWidget.builder(Text.literal(currentPropPitch + "\""), (btn) -> {})
            .dimensions(j + 30, y, 80, height).build();
        propPitchIndicator.active = false;
        this.addDrawableChild(propPitchIndicator);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), (btn) -> {
            currentPropPitch = Math.min(12.0f, (float) Math.round((currentPropPitch + 0.1f) * 10f) / 10f);
            updateButtonLabels();
        }).dimensions(j + 115, y, 25, height).build());
    }

    private void updateButtonLabels() {
        if (frameBtn != null) {
            frameBtn.setMessage(Text.literal(FRAMES[currentFrameIndex].name));
        }
        if (motorBtn != null) {
            motorBtn.setMessage(Text.literal(MOTORS[currentMotorIndex].name));
        }
        if (batteryBtn != null) {
            batteryBtn.setMessage(Text.literal(BATTERIES[currentBatteryIndex].name));
        }
        if (cameraBtn != null) {
            cameraBtn.setMessage(Text.literal(CAMERAS[currentCameraIndex].name));
        }
        if (modelBtn != null) {
            modelBtn.setMessage(Text.literal(localLinear ? "线性模型 (Linear)" : "叶片气动 (Propeller)"));
        }
        if (propDiaIndicator != null) {
            propDiaIndicator.setMessage(Text.literal(currentPropDia + "\""));
        }
        if (propPitchIndicator != null) {
            propPitchIndicator.setMessage(Text.literal(currentPropPitch + "\""));
        }
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int i = this.width / 2 - 230;
        int j = this.width / 2 - 110;
        int width = 140;

        // Calculate cumulative physical stats
        float stackWeight = 50f;
        float motorWeight = MOTORS[currentMotorIndex].weight * 4f;
        float propWeight = currentPropDia * currentPropDia * 0.15f * 4f;
        float frameWeight = FRAMES[currentFrameIndex].weight;
        float batteryWeight = BATTERIES[currentBatteryIndex].weight;
        float cameraWeight = CAMERAS[currentCameraIndex].weight;
        float totalWeight = frameWeight + stackWeight + motorWeight + propWeight + batteryWeight + cameraWeight;

        // Estimate static maximum thrust at t = 1.0 (v = 0)
        float dMeter = 0.0254f * currentPropDia;
        float propArea = (float) (Math.PI * (dMeter * dMeter) / 4f);
        float pitchFactor = (float) Math.pow(currentPropDia / (3.29547f * (currentPropPitch + 0.5f)), 1.5f);
        float propFactor = 1.225f * propArea * pitchFactor * 1.5f * 4f;
        float rawMaxRPM = MOTORS[currentMotorIndex].kv * BATTERIES[currentBatteryIndex].cells * 3.7f;
        float loadFactor = 5.4f / (float) Math.pow(currentPropDia, 1.1f);
        float maxRPM = Math.min(rawMaxRPM * loadFactor, rawMaxRPM);
        float Ve = maxRPM * 0.0254f * (currentPropPitch + 0.5f) / 60f;
        float maxThrustN = propFactor * (Ve * Ve);
        
        float massKg = totalWeight / 1000f;
        float twr = maxThrustN / (massKg * 9.8f);

        // Draw dynamic assessment panel at the bottom (aligned left to match columns)
        int panelY = this.height - 78;
        context.fill(i, panelY, j + width, panelY + 48, 0xAA000000); // dark transparent bg

        // White border decoration
        context.fill(i, panelY, j + width, panelY + 1, 0xFFFFFFFF);
        context.fill(i, panelY + 47, j + width, panelY + 48, 0xFFFFFFFF);
        context.fill(i, panelY, i + 1, panelY + 48, 0xFFFFFFFF);
        context.fill(j + width - 1, panelY, j + width, panelY + 48, 0xFFFFFFFF);

        String weightText = String.format("整机预估重量: %.1f g", totalWeight);
        String thrustText = String.format("最大推力估算: %.2f N", maxThrustN);
        context.drawTextWithShadow(this.textRenderer, Text.literal(weightText), i + 10, panelY + 8, 0xFFE0E0E0);
        context.drawTextWithShadow(this.textRenderer, Text.literal(thrustText), j + 10, panelY + 8, 0xFFE0E0E0);

        int twrColor = 0xFF55FF55; // standard green
        String perfLevel = "标准花飞 (Freestyle)";
        if (twr < 3.0f) {
            twrColor = 0xFFFF5555; // red
            perfLevel = "动力极弱 (Underpowered)";
        } else if (twr < 5.0f) {
            twrColor = 0xFFFFFF55; // yellow
            perfLevel = "航拍运镜 (Cinematic)";
        } else if (twr >= 8.0f) {
            twrColor = 0xFF55FFFF; // cyan
            perfLevel = "狂暴拉力 (Racing Beast)";
        }

        String twrText = String.format("推重比 (TWR): %.1f  [%s]", twr, perfLevel);
        context.drawTextWithShadow(this.textRenderer, Text.literal(twrText), i + 10, panelY + 26, twrColor);

        // Update rotation angles for real-time 3D drone animation
        this.droneYaw += delta * 1.5f;
        this.propRotation += delta * 45.0f;

        // Render the 3D drone preview viewport on the right
        int centerX = this.width / 2 + 120;
        int centerY = this.height / 2 - 10;
        int boxWidth = 170;
        int boxHeight = 160;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        // Draw viewport background: tech semi-transparent dark-cyan
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x99081C26);
        // Neon border decoration
        context.fill(boxX, boxY, boxX + boxWidth, boxY + 1, 0xFF00FFFF);
        context.fill(boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, 0xFF00FFFF);
        context.fill(boxX, boxY, boxX + 1, boxY + boxHeight, 0xFF00FFFF);
        context.fill(boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF00FFFF);

        // Tech sub-corners inside viewport
        context.fill(boxX + 4, boxY + 4, boxX + 14, boxY + 5, 0x8800FFFF);
        context.fill(boxX + 4, boxY + 4, boxX + 5, boxY + 14, 0x8800FFFF);
        context.fill(boxX + boxWidth - 14, boxY + 4, boxX + boxWidth - 4, boxY + 5, 0x8800FFFF);
        context.fill(boxX + boxWidth - 5, boxY + 4, boxX + boxWidth - 4, boxY + 14, 0x8800FFFF);
        context.fill(boxX + 4, boxY + boxHeight - 5, boxX + 14, boxY + boxHeight - 4, 0x8800FFFF);
        context.fill(boxX + 4, boxY + boxHeight - 14, boxX + 5, boxY + boxHeight - 4, 0x8800FFFF);
        context.fill(boxX + boxWidth - 14, boxY + boxHeight - 5, boxX + boxWidth - 4, boxY + boxHeight - 4, 0x8800FFFF);
        context.fill(boxX + boxWidth - 5, boxY + boxHeight - 14, boxX + boxWidth - 4, boxY + boxHeight - 4, 0x8800FFFF);

        // Tech indicators/labels
        context.drawTextWithShadow(this.textRenderer, Text.literal("3D REALTIME PREVIEW"), boxX + 8, boxY + 8, 0xFF00FFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("STATUS: SWAPPABLE"), boxX + 8, boxY + boxHeight - 14, 0xAA00FFFF);

        // 3D Rendering Pipeline Setup
        net.minecraft.client.util.math.MatrixStack matrices = context.getMatrices();
        matrices.push();
        // Position the drone model inside the box
        matrices.translate(centerX, centerY + 10, 250.0);
        // Scale appropriately (negating Y to keep standard Up position in Minecraft's Gui space)
        matrices.scale(110.0f, -110.0f, 110.0f);

        // Scientific tilt: 20-deg pitched down, and continuous slow rotation
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(20.0f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(droneYaw));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));

        // Enable diffuse GUI depth lighting for shadows & depth
        DiffuseLighting.enableGuiDepthLighting();

        // Pass matrices and custom component configurations to renderDrone
        com.iung.fpv20.utils.DroneModelRenderer.renderDrone(
            matrices,
            context.getVertexConsumers(),
            0xF000F0, // full light
            OverlayTexture.DEFAULT_UV,
            currentFrameIndex,
            currentMotorIndex,
            currentBatteryIndex,
            currentCameraIndex,
            currentPropDia,
            currentPropPitch,
            propRotation
        );

        // Flush rendering buffer
        context.draw();

        // Reset depth lighting state
        DiffuseLighting.disableGuiDepthLighting();

        matrices.pop();
    }

    @Override
    public void onOk() {
        Fpv20Client.config1.drone.linearAcceleration = localLinear;
        Fpv20Client.config1.drone.frameIndex = currentFrameIndex;
        Fpv20Client.config1.drone.motorIndex = currentMotorIndex;
        Fpv20Client.config1.drone.batteryIndex = currentBatteryIndex;
        Fpv20Client.config1.drone.cameraIndex = currentCameraIndex;
        Fpv20Client.config1.drone.selectedPropDia = currentPropDia;
        Fpv20Client.config1.drone.selectedPropPitch = currentPropPitch;

        // Calculate and derive total mass
        float stackWeight = 50f;
        float motorWeight = MOTORS[currentMotorIndex].weight * 4f;
        float propWeight = currentPropDia * currentPropDia * 0.15f * 4f;
        float frameWeight = FRAMES[currentFrameIndex].weight;
        float batteryWeight = BATTERIES[currentBatteryIndex].weight;
        float cameraWeight = CAMERAS[currentCameraIndex].weight;
        float totalWeightGrams = frameWeight + stackWeight + motorWeight + propWeight + batteryWeight + cameraWeight;

        // Calculate static maximum thrust at t = 1.0 (v = 0)
        float dMeter = 0.0254f * currentPropDia;
        float propArea = (float) (Math.PI * (dMeter * dMeter) / 4f);
        float pitchFactor = (float) Math.pow(currentPropDia / (3.29547f * (currentPropPitch + 0.5f)), 1.5f);
        float propFactor = 1.225f * propArea * pitchFactor * 1.5f * 4f;
        float rawMaxRPM = MOTORS[currentMotorIndex].kv * BATTERIES[currentBatteryIndex].cells * 3.7f;
        float loadFactor = 5.4f / (float) Math.pow(currentPropDia, 1.1f);
        float maxRPM = Math.min(rawMaxRPM * loadFactor, rawMaxRPM);
        float Ve = maxRPM * 0.0254f * (currentPropPitch + 0.5f) / 60f;
        float maxThrustN = propFactor * (Ve * Ve);

        // Convert weight (g) to mass (kg)
        Fpv20Client.config1.drone.mass = totalWeightGrams / 1000f;
        
        // Save the calculated max thrust for the linear model
        Fpv20Client.config1.drone.max_force = maxThrustN;
        
        // Feed direct motor KV, batteryCells, prop Diameter, and Pitch to the physics calculations
        Fpv20Client.config1.drone.motorKv = MOTORS[currentMotorIndex].kv;
        Fpv20Client.config1.drone.batteryCells = BATTERIES[currentBatteryIndex].cells;
        Fpv20Client.config1.drone.propDiameter = currentPropDia;
        Fpv20Client.config1.drone.propPitch = currentPropPitch;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && GlobalFlying.getFlying()) {
            IsFlying p = (IsFlying) client.player;
            p.set_frame_index(currentFrameIndex);
            
            if (ClientPlayNetworking.canSend(DroneFlyPacket.TYPE)) {
                int mode = (Fpv20Client.config1.controlMode == com.iung.fpv20.config.Fpv20ConfigClientManual.ControlMode.SCHEME_A) ? 1 : 0;
                ClientPlayNetworking.send(new DroneFlyPacket(true, currentFrameIndex, mode, Fpv20Client.config1.getCamera_angle()));
            }
        }

        Fpv20Client.config1.save();
    }
}
