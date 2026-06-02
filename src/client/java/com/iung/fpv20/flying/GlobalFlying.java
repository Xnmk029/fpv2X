package com.iung.fpv20.flying;

import com.iung.fpv20.Fpv20;
import com.iung.fpv20.Fpv20Client;
import com.iung.fpv20.input.Controller;
import com.iung.fpv20.mixin_utils.IsFlying;
import com.iung.fpv20.network.DroneFlyPacket;
import com.iung.fpv20.physics.DefaultDrone;
import com.iung.fpv20.physics.Drone;
import com.iung.fpv20.physics.PhysicsCore;
import com.iung.fpv20.physics.Plane;
import com.iung.fpv20.sound.FlyingSound;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static com.iung.fpv20.Fpv20Client.config1;
import static com.iung.fpv20.Fpv20Client.in_slow_motion;
import static com.iung.fpv20.utils.LocalMath.DEG_TO_RAD;

public class GlobalFlying {
    public static GlobalFlying G = new GlobalFlying(0);
//    private float lastCamRoll;
//    private float camRoll;

    private Quaternionf lastDroneRotation;
    private Quaternionf droneRotation;

    private float last_update_time;
    /**
     * deg/s
     */
//    private float angular_speed;

    private boolean last_tick_flying;

    //    private float cam_angel_deg;
    private Drone drone;

    private Vec3d last_pos;

    private long last_tick_nano;
    private long this_tick_nano;
    private Vec3d last_tick_pos;
    private Vec3d this_tick_pos;
    private Vec3d speed;

    private void update_speed_tick(Entity player) {
        last_tick_pos = this_tick_pos;
        this_tick_pos = player.getPos();

        last_tick_nano = this_tick_nano;
        this_tick_nano = System.nanoTime();

        speed = this_tick_pos.subtract(last_tick_pos)
                .multiply(1000_000_000.0 / (this_tick_nano - last_tick_nano));
    }

    private Vec3d get_speed() {
        return this.speed;
    }

    public Drone getDrone() {
        return this.drone;
    }

    private float getMappedThrottle(Controller controller) {
        float input_t = controller.get_value_by_name("t");
        boolean is3D = false;
        try {
            is3D = controller.get_value_by_name("3d") > 0.5f;
        } catch (Exception ignored) {}

        if (is3D) {
            return input_t; // Range [-1, 1] for 3D inverted flight
        } else {
            return (input_t + 1f) / 2f; // Map [-1, 1] to [0, 1] for normal flight
        }
    }


    private static float time_now_float() {
        return (float) ((double) System.nanoTime() / 1000_000_000.d);
    }

    public GlobalFlying(int camRoll) {
//        this.lastCamRoll = camRoll;
//        this.camRoll = camRoll;
//        this.angular_speed = angular_speed;
        this.last_update_time = time_now_float();
        this.last_tick_flying = false;
        switch (Fpv20Client.config1.drone_select) {
            case DefaultDrone -> this.drone = new DefaultDrone();
            case Plane -> this.drone = new Plane();
        }
        this.droneRotation = new Quaternionf();
        this.lastDroneRotation = new Quaternionf();
//        this.cam_angel_deg = 30;
        this.last_pos = new Vec3d(0, 0, 0);
        this.last_tick_pos = new Vec3d(0, 0, 0);
        this.last_tick_nano = System.nanoTime();
        this.this_tick_pos = new Vec3d(0, 0, 0);
        this.this_tick_nano = System.nanoTime() + 1;
    }

    public static void setFlying(boolean if_fly) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        IsFlying p = (IsFlying) player;
        if (p != null) {

            if (ClientPlayNetworking.canSend(DroneFlyPacket.TYPE)) {
                ClientPlayNetworking.send(new DroneFlyPacket(if_fly));
            }
            p.set_is_flying(if_fly);
            client.getSoundManager().play(new FlyingSound(player));

            // GPU FPV 鱼眼畸变后处理着色器触发
            if (client.gameRenderer != null) {
                if (if_fly && Fpv20Client.config1.enableFisheye) {
                    try {
                        ((com.iung.fpv20.mixin_utils.GameRendererAccessor) client.gameRenderer).fpv20$loadPostProcessor(new net.minecraft.util.Identifier("minecraft", "shaders/post/fisheye.json"));
                    } catch (Exception e) {
                        Fpv20.LOGGER.error("Failed to load FPV fisheye shader", e);
                    }
                } else {
                    try {
                        ((com.iung.fpv20.mixin_utils.GameRendererAccessor) client.gameRenderer).fpv20$disablePostProcessor();
                    } catch (Exception e) {
                        Fpv20.LOGGER.error("Failed to clear FPV fisheye shader", e);
                    }
                }
            }
        }
    }

    public static boolean getFlying() {
        IsFlying p = (IsFlying) MinecraftClient.getInstance().player;
        if (p != null) {
            return p.get_is_flying();
        } else {
            return false;
        }
    }

    public void update_tick_start(Entity player) {
        this.update_speed_tick(player);
    }

    public Quaternionf cacl_cam_rotation() {
        Quaternionf new_r = new Quaternionf(this.droneRotation);
        new_r.rotateLocalX(-Fpv20Client.config1.getCamera_angle() * DEG_TO_RAD);
        return new_r;
    }

    @Deprecated
    public Quaternionf cacl_cam_rotation_last() {
        Quaternionf new_r = new Quaternionf(this.lastDroneRotation);
        new_r.rotateLocalX(-Fpv20Client.config1.getCamera_angle() * DEG_TO_RAD);
        return new_r;
    }

    public void _handle_flying(MinecraftClient client) {

        float now_time = time_now_float();

        float dt = now_time - this.last_update_time;

        this._handle_flying_inner(client, dt);

        this.last_tick_flying = getFlying();
        this.last_update_time = now_time;
    }

    private void handle_flying(MinecraftClient client, float tick_delta) {

        float now_time = time_now_float();

        float dt = (float) (tick_delta * 0.05);

        this._handle_flying_inner(client, dt);

        this.last_tick_flying = getFlying();
        this.last_update_time = now_time;
    }

//    private void set_cam_roll(float roll) {
//        this.lastCamRoll = this.camRoll;
//        this.camRoll = roll;
//    }

    private void set_drone_rotation(Quaternionf camPos) {
        this.lastDroneRotation = this.droneRotation;
        this.droneRotation = new Quaternionf(camPos);
    }


    private void _handle_flying_inner(MinecraftClient client, float dt) {

//        float now_time = time_now_float();
//
//        float dt = now_time - this.last_update_time;

        Fpv20.LOGGER.debug("handle_flying:dt {}", dt);

        if (!getFlying()) {
            return;
        }

        ClientPlayerEntity p = client.player;
        if (p == null) {
            return;
        }


//        float roll = this.camRoll;

        // start flying, init the drone
        if (this.last_tick_flying != getFlying()) {
            float yaw = p.getYaw();
            float pitch = p.getPitch();
            drone.re_init();
            drone.update_pose(PhysicsCore.from_ypr_deg(yaw, pitch, 0));
        }

        Quaternionf q = drone.get_pose();

        Controller controller = Fpv20Client.controller;
        if (controller == null) {
            return;
        }

        float input_t = getMappedThrottle(controller);
        apply_rotation_with_rates(q, controller, dt);
        drone.update_pose(q);
        this.set_drone_rotation(q);


        // process hit
        Vec3d v = drone.get_speed();
        Vector3f vd = new Vector3f((float) v.x, (float) v.y, (float) v.z);
        Vec3d pos = p.getPos();
//        Vec3d v0 = pos.subtract(last_pos).multiply(1 / dt);
        Vec3d v0 = this.get_speed();
        Fpv20.LOGGER.debug("process hit:v0 {}", v0);
        last_pos = pos;
        float aaa = 0.5f;
        boolean to_set_x = false;
        boolean to_set_y = false;
        boolean to_set_z = false;


        if (Math.abs(v0.x) < 0.0001) {
            Fpv20.LOGGER.debug("process hit:x");
            vd.x = 0;
            to_set_y = true;
            to_set_z = true;
        }
        if (Math.abs(v0.y) < 0.0001) {
            Fpv20.LOGGER.debug("process hit:y");
            vd.y = 0;
            to_set_x = true;
            to_set_z = true;
        }
        if (Math.abs(v0.z) < 0.0001) {
            Fpv20.LOGGER.debug("process hit:z");
            vd.z = 0;
            to_set_y = true;
            to_set_x = true;
        }

        if (to_set_x) {
            float d = dt * aaa;
            if (vd.x < -d) {
                vd.x += d;
            } else if (vd.x > d) {
                vd.x -= d;
            } else {
                vd.x = 0;
            }
        }
        if (to_set_y) {
            float d = dt * aaa;
            if (vd.y < -d) {
                vd.y += d;
            } else if (vd.y > d) {
                vd.y -= d;
            } else {
                vd.y = 0;
            }
        }
        if (to_set_z) {
            float d = dt * aaa;
            if (vd.z < -d) {
                vd.z += d;
            } else if (vd.z > d) {
                vd.z -= d;
            } else {
                vd.z = 0;
            }
        }

        drone.set_speed(new Vec3d(vd));
        // // process hit


        drone.update_physics(input_t, dt);


//        Fpv20.LOGGER.debug("{}", dt);
        Vec3d v1 = drone.get_speed();
        p.setVelocity(v1);

//        Vector3f vd = new Vector3f((float) v.x, (float) v.y, (float) v.z);
//        Vec3d v0 = p.getVelocity();
//        if (Math.abs(v0.x) < 0.0001) {
//            vd.x = 0;
//        }
//        if (Math.abs(v0.y) < 0.0001) {
//            vd.y = 0;
//        }
//        if (Math.abs(v0.z) < 0.0001) {
//            vd.z = 0;
//        }
//        drone.set_speed(new Vec3d(vd));


//        Fpv20.LOGGER.debug("v0 {}", p.getVelocity());
//        p.setVelocity(drone.get_speed());
        Fpv20.LOGGER.debug("after update phy:v1 {}", p.getVelocity());


        Vector3f new_ypr = PhysicsCore.from_quaternion_to_ypr_deg(this.cacl_cam_rotation());
        if (false) {
            p.setYaw(new_ypr.x);
            p.setPitch(new_ypr.y);
        } else {
            p.setYaw(0);
            p.setPitch(0);
        }
//        this.set_cam_roll(new_ypr.z);


        return;
    }

    public void handle_flying_phy(ClientPlayerEntity player, float dt) {
        if (in_slow_motion) {
            dt *= config1.slow_motion_time_rate;
        }
        if (!getFlying()) {
            return;
        }

        ClientPlayerEntity p = player;
        if (p == null) {
            return;
        }


        Controller controller = Fpv20Client.controller;
        if (controller == null) {
            return;
        }
        float input_t = getMappedThrottle(controller);

        // process hit
        Vec3d v = drone.get_speed();
        Vector3f vd = new Vector3f((float) v.x, (float) v.y, (float) v.z);
        Vec3d pos = p.getPos();

        Vec3d v0 = this.get_speed();
        Fpv20.LOGGER.debug("process hit:v0 {}", v0);
        last_pos = pos;

        // 1. 植被穿梭阻力与坠水阻尼计算
        if (p.getWorld() != null) {
            net.minecraft.util.math.BlockPos blockPos = p.getBlockPos();
            net.minecraft.block.BlockState blockState = p.getWorld().getBlockState(blockPos);
            String blockName = blockState.getBlock().getTranslationKey().toLowerCase();
            boolean isFoliage = blockName.contains("leaves") 
                    || blockName.contains("grass") 
                    || blockName.contains("fern") 
                    || blockName.contains("vine")
                    || blockName.contains("crop")
                    || blockName.contains("flower")
                    || blockName.contains("bush");
            if (isFoliage) {
                float foliageFriction = Fpv20Client.config1.physics.foliageFriction;
                vd.mul(1.0f - foliageFriction * dt);
            }

            if (p.isSubmergedInWater() || p.isTouchingWater()) {
                float waterDrag = Fpv20Client.config1.physics.waterDrag;
                vd.mul(1.0f - waterDrag * 10.0f * dt);
            }
        }

        // 2. 3D 三轴弹性反弹与滑动摩擦力学计算
        float bounciness = Fpv20Client.config1.physics.bounciness;
        float groundFriction = Fpv20Client.config1.physics.groundFriction;

        if (Math.abs(vd.x) > 0.1f && Math.abs(v0.x) < 0.05f) {
            vd.x = -vd.x * bounciness;
            vd.y *= (1.0f - groundFriction);
            vd.z *= (1.0f - groundFriction);
        }
        if (Math.abs(vd.y) > 0.1f && Math.abs(v0.y) < 0.05f) {
            vd.y = -vd.y * bounciness;
            vd.x *= (1.0f - groundFriction);
            vd.z *= (1.0f - groundFriction);
        }
        if (Math.abs(vd.z) > 0.1f && Math.abs(v0.z) < 0.05f) {
            vd.z = -vd.z * bounciness;
            vd.x *= (1.0f - groundFriction);
            vd.y *= (1.0f - groundFriction);
        }

        drone.set_speed(new Vec3d(vd));
        // // process hit


        drone.update_physics(input_t, dt);


        Vec3d v1 = drone.get_speed();


        if (in_slow_motion) {
            p.setVelocity(v1.multiply(Fpv20Client.config1.slow_motion_time_rate * 0.05));
        } else {
            p.setVelocity(v1.multiply(0.05));
        }


        Fpv20.LOGGER.debug("after update phy:v1 {}", p.getVelocity());


    }

    public void handle_flying_rotate(MinecraftClient client, float dt) {
        if (in_slow_motion) {
            dt *= config1.slow_motion_time_rate;
        }


        if (!getFlying()) {
            this.last_tick_flying = false;

            return;
        }

        ClientPlayerEntity p = client.player;
        if (p == null) {
            return;
        }


        // start flying, init the drone
        if (this.last_tick_flying != getFlying()) {
            float yaw = p.getYaw();
            float pitch = p.getPitch();
            drone.re_init();
            if (Fpv20Client.config1.free_camera_yaw) {
                p.setYaw(180);
            }
            if (Fpv20Client.config1.free_camera_pitch) {
                p.setPitch(0);

            }
            Fpv20.LOGGER.info("start flying");

            drone.update_pose(PhysicsCore.from_ypr_deg(yaw, pitch, 0));
        }
        this.last_tick_flying = getFlying();

        Quaternionf q = drone.get_pose();

        Controller controller = Fpv20Client.controller;
        if (controller == null) {
            return;
        }

        float input_t = controller.get_value_by_name("t");
        apply_rotation_with_rates(q, controller, dt);
        drone.update_pose(q);
        this.set_drone_rotation(q);


        Vector3f new_ypr = PhysicsCore.from_quaternion_to_ypr_deg(this.cacl_cam_rotation());


        if (Fpv20.config.in_fabric()) {
            if (!Fpv20Client.config1.free_camera_yaw) {
                p.setYaw(new_ypr.x);
            }
            if (!Fpv20Client.config1.free_camera_pitch) {
                p.setPitch(new_ypr.y);
            }
        } else {
            if (!Fpv20Client.config1.free_camera_yaw) {
                p.setYaw(180);
            }
            if (!Fpv20Client.config1.free_camera_pitch) {
                p.setPitch(0);
            }
//            p.setYaw(180);
//            p.setPitch(0);
        }
    }


    private void apply_rotation_with_rates(Quaternionf q, Controller controller, float dt) {
        float input_y = controller.get_value_by_name_no_rate("y");
        float input_p = controller.get_value_by_name_no_rate("p");
        float input_r = controller.get_value_by_name_no_rate("r");

        com.iung.fpv20.config.Fpv20ConfigClientManual.RatesConfig rates = Fpv20Client.config1.rates;
        
        float yawSpeed = com.iung.fpv20.physics.Rates.calculateRate(
                input_y, rates.type, rates.yaw.rate, rates.yaw.superRate, rates.yaw.expo, rates.yaw.centerSensitivity, rates.yaw.maxRate
        );
        float pitchSpeed = com.iung.fpv20.physics.Rates.calculateRate(
                input_p, rates.type, rates.pitch.rate, rates.pitch.superRate, rates.pitch.expo, rates.pitch.centerSensitivity, rates.pitch.maxRate
        );
        float rollSpeed = com.iung.fpv20.physics.Rates.calculateRate(
                input_r, rates.type, rates.roll.rate, rates.roll.superRate, rates.roll.expo, rates.roll.centerSensitivity, rates.roll.maxRate
        );

        PhysicsCore.rotate_from_local_yaw_pitch_roll(q, yawSpeed, pitchSpeed, rollSpeed, 1.0f, 1.0f, 1.0f, dt);
    }
}
