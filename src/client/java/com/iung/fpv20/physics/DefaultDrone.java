package com.iung.fpv20.physics;

import com.iung.fpv20.Fpv20;
import com.iung.fpv20.Fpv20Client;
import com.iung.fpv20.utils.FastMath;
import com.iung.fpv20.utils.Utils;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import oshi.driver.mac.net.NetStat;

public class DefaultDrone implements Drone {

    private static final Vector3f G = new Vector3f(0, -9.8f, 0);
    private static final float AIR_DENSITY = 1.225F;



    private Quaternionf pose;
    private Utils.FloatGetter mass;

    private Utils.FloatGetter max_force;

    private float area;


    private Vector3f a;
    private Vector3f v;

    public DefaultDrone() {
        this.pose = new Quaternionf();
        this.a = new Vector3f();
        this.v = new Vector3f();

        this.mass = () -> Fpv20Client.config1.drone.mass;

        this.area = (float) (FastMath.PI * 0.2 * 0.2);
        this.max_force = () -> Fpv20Client.config1.drone.max_force;
    }

    @Override
    public void update_pose(Quaternionf new_pos) {
//        Fpv20.LOGGER.info("{},{}", mass, max_force);

        this.pose = new Quaternionf(new_pos);
    }

    @Override
    public Quaternionf get_pose() {
        return new Quaternionf(this.pose);
    }

    @Override
    public void update_physics(float throttle, float dt) {
        Quaternionf pose = this.get_pose().conjugate();
        Vector3f drone_up = new Vector3f(0, 1, 0).rotate(pose);

        float speed = this.v.length();
        float cosTheta = 0f;
        if (speed > 0.00001f) {
            float dot = drone_up.dot(new Vector3f(this.v).normalize());
            cosTheta = Math.abs(dot);
            if (Float.isNaN(cosTheta)) {
                cosTheta = 0f;
            }
        }
        float dMeter = 0.0254f * Fpv20Client.config1.drone.propDiameter;
        float propDiscArea = (float) (Math.PI * (dMeter * dMeter) / 4f) * 4f;
        float maxArea = propDiscArea * 0.8f + 0.01f; // ~0.052m^2 for 5" prop
        float minArea = maxArea * 0.2f;              // ~0.010m^2 for 5" prop

        float dynamicArea = minArea + (maxArea - minArea) * cosTheta;

        float dragFactor = (AIR_DENSITY * dynamicArea) / 2F; // kg / m
        Vector3f ambientDragForce = new Vector3f(this.v).normalize().mul(-1f *
                speed *
                speed *
                dragFactor);
        if (this.v.length() < 0.00001) {
            ambientDragForce = new Vector3f();
        }
        Fpv20.LOGGER.debug("#ambientDragForce {}", ambientDragForce);


        float inflowVelocity = 0f;
        float vLen = this.v.length();
        if (vLen > 0.00001f) {
            float dot = drone_up.dot(this.v);
            float cosAngle = dot / vLen;
            cosAngle = Math.max(-1f, Math.min(1f, cosAngle));
            float angle = (float) Math.acos(cosAngle);
            float inflowCoefficient = 1f - (float) Math.sin(angle);
            if (Float.isNaN(inflowCoefficient)) {
                inflowCoefficient = 0f;
            }
            inflowVelocity = vLen * inflowCoefficient;
        }

        float thrustForceScalar = 0f;
        boolean linear = Fpv20Client.config1.drone.linearAcceleration;
        if (linear) {
            float efficiency = MathHelper.lerp(Math.abs(throttle), 0.35f, 1f);
            thrustForceScalar = Fpv20Client.config1.drone.max_force * throttle * efficiency;
        } else {
            int batteryCells = Fpv20Client.config1.drone.batteryCells;
            float motorKv = Fpv20Client.config1.drone.motorKv;
            float propDiameter = Fpv20Client.config1.drone.propDiameter;
            float propPitch = Fpv20Client.config1.drone.propPitch;

            // 1. Calculate prop disc area factor 'a'
            dMeter = 0.0254f * propDiameter;
            float propArea = (float) (Math.PI * (dMeter * dMeter) / 4f);
            float pitchFactor = (float) Math.pow(propDiameter / (3.29547f * (propPitch + 0.5f)), 1.5f);
            // Coefficient matches BeamNG drone.lua: AIR_DENSITY * Area * PitchFactor * 1.5 * 4
            float propFactor = AIR_DENSITY * propArea * pitchFactor * 1.5f * 4f;

            // 2. Calculate RPM
            float rawMaxRPM = motorKv * batteryCells * 3.7f;
            float loadFactor = 5.4f / (float) Math.pow(propDiameter, 1.1f);
            float maxRPM = Math.min(rawMaxRPM * loadFactor, rawMaxRPM);
            
            boolean is3D = false;
            try {
                if (Fpv20Client.controller != null) {
                    is3D = Fpv20Client.controller.get_value_by_name("3d") > 0.5f;
                }
            } catch (Exception ignored) {}

            float throttleValue = is3D ? throttle : Math.max(0f, throttle);
            float rpm = throttleValue * maxRPM;

            // 3. Calculate maxVe and Ve
            float maxVe = maxRPM * 0.0254f * (propPitch + 0.5f) / 60f;
            float Ve = rpm * 0.0254f * (propPitch + 0.5f) / 60f;

            // 4. Calculate inflow velocity correction
            float ic = 1f;
            if (maxVe > 0.0001f) {
                ic = (maxVe - Math.abs(inflowVelocity)) / maxVe;
                // Allow dynamic decay to 0 (full stall/zero efficiency) but avoid reversing here
                ic = Math.max(0.0f, Math.min(1f, ic));
            }

            // 5. Calculate final force
            thrustForceScalar = propFactor * (Math.abs(Ve) * Ve) * ic;
        }

        Vector3f thrust = new Vector3f(drone_up).mul(thrustForceScalar);
        Fpv20.LOGGER.debug("#thrust {}", thrust);

        Vector3f total_force = new Vector3f().add(ambientDragForce).add(thrust);

        this.a = total_force.div(mass.get()).add(G);

        Fpv20.LOGGER.debug("#a {}", this.a);

        Fpv20.LOGGER.debug("#f {}", dt);

        Fpv20.LOGGER.debug("#dv:? {}", new Vector3f(this.a).mul(dt));


        this.v.add(new Vector3f(this.a).mul(dt));
    }

    @Override
    public Vec3d get_acceleration() {
//        Fpv20.LOGGER.debug("acc:{}", this.a);
        return new Vec3d(this.a);
    }

    @Override
    public Vec3d get_speed() {
        return new Vec3d(this.v);
    }

    @Override
    public void set_speed(Vec3d v) {
        this.v = v.toVector3f();
    }

    @Override
    public void re_init() {
        this.pose = new Quaternionf();
        this.v = new Vector3f();

    }
}
