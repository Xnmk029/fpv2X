package com.iung.fpv20.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.file.Files;
import java.nio.file.Path;

public class Fpv20ConfigClientManual {
    private static String CONFIG_PATH = "./config/fpv20_client.json";

    private float camera_angle = 35;

    public static class Drone {
        public float mass = 0.5f;
        public float max_force = 16f;
        public boolean linearAcceleration = false;
        public int batteryCells = 6;
        public float motorKv = 2900f;
        public float propDiameter = 5.5f;
        public float propPitch = 5.2f;
        
        public int frameIndex = 2; // Default 5" Freestyle
        public int motorIndex = 2; // Default 2207 1950KV
        public int batteryIndex = 3; // Default 6S 1300mAh
        public int cameraIndex = 0; // Default None
        public float selectedPropDia = 5.1f;
        public float selectedPropPitch = 4.3f;
    }

    public Drone drone = new Drone();

    public static enum DroneType {
        DefaultDrone,
        Plane
    }

    public DroneType drone_select = DroneType.DefaultDrone;

    public static class Plane {
        public float c1 = 1.0f;
    }

    public Plane plane = new Plane();

    public static class AngularVelocity_DegSec {
        public float yaw = 300;
        public float pitch = 300;
        public float roll = 300;
    }

    public AngularVelocity_DegSec angular_velocity__deg_sec = new AngularVelocity_DegSec();

    public boolean free_camera_yaw = false;
    public boolean free_camera_pitch = false;

    public float slow_motion_time_rate = 0.2f;
    public String slow_motion_switch_name = "sm";

    // --- Rates & Physics Configurations ---
    public static class RateAxis {
        public float rate = 1.0f;
        public float superRate = 0.6f;
        public float expo = 0.4f;
        // For Actual Rates:
        public float centerSensitivity = 150f;
        public float maxRate = 400f;
    }

    public static class RatesConfig {
        public String type = "betaflight"; // "betaflight", "actual", "kiss"
        public RateAxis roll = new RateAxis();
        public RateAxis pitch = new RateAxis();
        public RateAxis yaw = new RateAxis();
    }

    public static class PhysicsSettings {
        public float bounciness = 0.4f;
        public float groundFriction = 0.3f;
        public float foliageFriction = 0.5f;
        public float waterDrag = 0.8f;
    }

    public RatesConfig rates = new RatesConfig();
    public PhysicsSettings physics = new PhysicsSettings();
    public boolean enableFisheye = true;

    /////////////////////////////////
    public float getCamera_angle() {
        return camera_angle;
    }

    public void setCamera_angle(float camera_angle) {
        this.camera_angle = camera_angle;
    }


    private String to_json() {
        Gson j = new GsonBuilder().setPrettyPrinting().create();

        return j.toJson(this);
    }

    private static Fpv20ConfigClientManual from_json(String json) {
        Gson j = new Gson();

        return j.fromJson(json, Fpv20ConfigClientManual.class);
    }

    public static Fpv20ConfigClientManual createAndLoad() {
        Fpv20ConfigClientManual config;
        try {
            String content = Files.readString(Path.of(CONFIG_PATH));
            config = from_json(content);
        } catch (Exception ignored) {
            config = new Fpv20ConfigClientManual();
        }
        
        // Null safeguards for backwards compatibility
        if (config.rates == null) config.rates = new RatesConfig();
        if (config.rates.roll == null) config.rates.roll = new RateAxis();
        if (config.rates.pitch == null) config.rates.pitch = new RateAxis();
        if (config.rates.yaw == null) config.rates.yaw = new RateAxis();
        if (config.physics == null) config.physics = new PhysicsSettings();
        
        return config;
    }

    public void save() {
        String json = this.to_json();
        try {
            Files.writeString(Path.of(CONFIG_PATH), json);
        } catch (Exception ignored) {

        }
    }
}
