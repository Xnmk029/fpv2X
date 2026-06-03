package com.iung.fpv20.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.Optional;
import java.util.UUID;

public class DroneEntity extends Entity {

    // DataTrackers for automatic server-client state synchronization
    private static final TrackedData<Optional<UUID>> PILOT_UUID = DataTracker.registerData(DroneEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Integer> FRAME_INDEX = DataTracker.registerData(DroneEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> MOTOR_INDEX = DataTracker.registerData(DroneEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> BATTERY_INDEX = DataTracker.registerData(DroneEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CAMERA_INDEX = DataTracker.registerData(DroneEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> PROP_DIA = DataTracker.registerData(DroneEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> PROP_PITCH = DataTracker.registerData(DroneEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> DRONE_ROLL = DataTracker.registerData(DroneEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> CAMERA_ANGLE = DataTracker.registerData(DroneEntity.class, TrackedDataHandlerRegistry.FLOAT);
    
    // Quaternion components for lock-free 3-axis tracking
    private static final TrackedData<Float> DRONE_QX = DataTracker.registerData(DroneEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> DRONE_QY = DataTracker.registerData(DroneEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> DRONE_QZ = DataTracker.registerData(DroneEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> DRONE_QW = DataTracker.registerData(DroneEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public DroneEntity(EntityType<?> type, World world) {
        super(type, world);
        this.intersectionChecked = true;
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(PILOT_UUID, Optional.empty());
        this.dataTracker.startTracking(FRAME_INDEX, 2); // default 5"
        this.dataTracker.startTracking(MOTOR_INDEX, 2);
        this.dataTracker.startTracking(BATTERY_INDEX, 3);
        this.dataTracker.startTracking(CAMERA_INDEX, 0);
        this.dataTracker.startTracking(PROP_DIA, 5.0f);
        this.dataTracker.startTracking(PROP_PITCH, 4.0f);
        this.dataTracker.startTracking(DRONE_ROLL, 0.0f);
        this.dataTracker.startTracking(CAMERA_ANGLE, 25.0f);
        
        // Default orientation is identity quaternion
        this.dataTracker.startTracking(DRONE_QX, 0.0f);
        this.dataTracker.startTracking(DRONE_QY, 0.0f);
        this.dataTracker.startTracking(DRONE_QZ, 0.0f);
        this.dataTracker.startTracking(DRONE_QW, 1.0f);
    }

    @Override
    public void tick() {
        super.tick();

        // Standard movement logic: integrate velocity into position
        if (!this.hasNoGravity()) {
            // Under remote flight mode, gravity is handled inside the physical simulator
        }
        
        this.move(net.minecraft.entity.MovementType.SELF, this.getVelocity());
    }

    // Getters and Setters with automatic packet synchronization via DataTracker
    @Nullable
    public UUID getPilotUuid() {
        return this.dataTracker.get(PILOT_UUID).orElse(null);
    }

    public void setPilotUuid(@Nullable UUID uuid) {
        this.dataTracker.set(PILOT_UUID, Optional.ofNullable(uuid));
    }

    public int getFrameIndex() {
        return this.dataTracker.get(FRAME_INDEX);
    }

    public void setFrameIndex(int val) {
        this.dataTracker.set(FRAME_INDEX, val);
        this.calculateDimensions();
    }

    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        int frame = this.getFrameIndex();
        float width = 0.5f;
        float height = 0.15f;
        switch (frame) {
            case 0: // 2" Whoop
                width = 0.25f;
                height = 0.08f;
                break;
            case 1: // 3" Cinewhoop
                width = 0.35f;
                height = 0.12f;
                break;
            case 2: // 5" Freestyle
                width = 0.50f;
                height = 0.15f;
                break;
            case 3: // 7" LongRange
                width = 0.68f;
                height = 0.20f;
                break;
        }
        return new EntityDimensions(width, height, false);
    }

    public int getMotorIndex() {
        return this.dataTracker.get(MOTOR_INDEX);
    }

    public void setMotorIndex(int val) {
        this.dataTracker.set(MOTOR_INDEX, val);
    }

    public int getBatteryIndex() {
        return this.dataTracker.get(BATTERY_INDEX);
    }

    public void setBatteryIndex(int val) {
        this.dataTracker.set(BATTERY_INDEX, val);
    }

    public int getCameraIndex() {
        return this.dataTracker.get(CAMERA_INDEX);
    }

    public void setCameraIndex(int val) {
        this.dataTracker.set(CAMERA_INDEX, val);
    }

    public float getPropDia() {
        return this.dataTracker.get(PROP_DIA);
    }

    public void setPropDia(float val) {
        this.dataTracker.set(PROP_DIA, val);
    }

    public float getPropPitch() {
        return this.dataTracker.get(PROP_PITCH);
    }

    public void setPropPitch(float val) {
        this.dataTracker.set(PROP_PITCH, val);
    }

    public float getDroneRoll() {
        return this.dataTracker.get(DRONE_ROLL);
    }

    public void setDroneRoll(float val) {
        this.dataTracker.set(DRONE_ROLL, val);
    }

    public float getCameraAngle() {
        return this.dataTracker.get(CAMERA_ANGLE);
    }

    public void setCameraAngle(float val) {
        this.dataTracker.set(CAMERA_ANGLE, val);
    }

    public Quaternionf getQuaternion() {
        return new Quaternionf(
            this.dataTracker.get(DRONE_QX),
            this.dataTracker.get(DRONE_QY),
            this.dataTracker.get(DRONE_QZ),
            this.dataTracker.get(DRONE_QW)
        );
    }

    public void setQuaternion(float x, float y, float z, float w) {
        this.dataTracker.set(DRONE_QX, x);
        this.dataTracker.set(DRONE_QY, y);
        this.dataTracker.set(DRONE_QZ, z);
        this.dataTracker.set(DRONE_QW, w);
    }

    public void setQuaternion(Quaternionf q) {
        this.setQuaternion(q.x(), q.y(), q.z(), q.w());
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("PilotUuid")) {
            this.setPilotUuid(nbt.getUuid("PilotUuid"));
        }
        this.setFrameIndex(nbt.getInt("FrameIndex"));
        this.setMotorIndex(nbt.getInt("MotorIndex"));
        this.setBatteryIndex(nbt.getInt("BatteryIndex"));
        this.setCameraIndex(nbt.getInt("CameraIndex"));
        this.setPropDia(nbt.getFloat("PropDia"));
        this.setPropPitch(nbt.getFloat("PropPitch"));
        this.setDroneRoll(nbt.getFloat("DroneRoll"));
        if (nbt.contains("CameraAngle")) {
            this.setCameraAngle(nbt.getFloat("CameraAngle"));
        }
        
        float qx = nbt.contains("DroneQx") ? nbt.getFloat("DroneQx") : 0.0f;
        float qy = nbt.contains("DroneQy") ? nbt.getFloat("DroneQy") : 0.0f;
        float qz = nbt.contains("DroneQz") ? nbt.getFloat("DroneQz") : 0.0f;
        float qw = nbt.contains("DroneQw") ? nbt.getFloat("DroneQw") : 1.0f;
        this.setQuaternion(qx, qy, qz, qw);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (this.getPilotUuid() != null) {
            nbt.putUuid("PilotUuid", this.getPilotUuid());
        }
        nbt.putInt("FrameIndex", this.getFrameIndex());
        nbt.putInt("MotorIndex", this.getMotorIndex());
        nbt.putInt("BatteryIndex", this.getBatteryIndex());
        nbt.putInt("CameraIndex", this.getCameraIndex());
        nbt.putFloat("PropDia", this.getPropDia());
        nbt.putFloat("PropPitch", this.getPropPitch());
        nbt.putFloat("DroneRoll", this.getDroneRoll());
        nbt.putFloat("CameraAngle", this.getCameraAngle());
        
        Quaternionf q = this.getQuaternion();
        nbt.putFloat("DroneQx", q.x());
        nbt.putFloat("DroneQy", q.y());
        nbt.putFloat("DroneQz", q.z());
        nbt.putFloat("DroneQw", q.w());
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }
}
