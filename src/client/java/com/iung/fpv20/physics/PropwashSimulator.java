package com.iung.fpv20.physics;

import com.iung.fpv20.config.Fpv20ConfigClientManual.PropwashLevel;
import org.joml.Vector3f;
import java.util.Random;

public class PropwashSimulator {
    private final Random random = new Random();
    
    // Accumulators to create smooth, correlated noise (like pink noise / bandpass filtered noise)
    // rather than purely white noise, making it feel like aerodynamic buffet.
    private float noiseX = 0.0f;
    private float noiseY = 0.0f;

    public void reset() {
        this.noiseX = 0.0f;
        this.noiseY = 0.0f;
    }

    /**
     * Calculates the propwash disturbance torque to be added to the gyro rates (deg/s).
     * 
     * @param velocity World velocity of the drone (m/s)
     * @param pose Quaternion (representing drone's rotation in world space)
     * @param throttle Current throttle value [0, 1]
     * @param level Propwash level configuration
     * @param dt Time delta (s)
     * @return A Vector3f containing the disturbance for [roll, pitch, yaw] in deg/s.
     */
    public Vector3f calculateDisturbance(Vector3f velocity, org.joml.Quaternionf pose, float throttle, PropwashLevel level, float dt) {
        Vector3f disturbance = new Vector3f(0, 0, 0);
        if (level == PropwashLevel.PERFECT || velocity == null || pose == null) {
            return disturbance;
        }

        // 1. Calculate drone's UP direction in world coordinates
        Vector3f droneUp = new Vector3f(0, 1, 0).rotate(pose);

        // 2. Calculate descent speed along the propeller axis (positive when falling in propwash direction)
        float descentSpeed = -droneUp.dot(velocity);

        // 3. Calculate propeller RPM and slipstream wash velocity Ve
        int batteryCells = com.iung.fpv20.Fpv20Client.config1.drone.batteryCells;
        float motorKv = com.iung.fpv20.Fpv20Client.config1.drone.motorKv;
        float propDiameter = com.iung.fpv20.Fpv20Client.config1.drone.propDiameter;
        float propPitch = com.iung.fpv20.Fpv20Client.config1.drone.propPitch;

        float rawMaxRPM = motorKv * batteryCells * 3.7f;
        float loadFactor = 5.4f / (float) Math.pow(propDiameter, 1.1f);
        float maxRPM = Math.min(rawMaxRPM * loadFactor, rawMaxRPM);
        float rpm = Math.max(0.0f, throttle) * maxRPM;

        // Propeller wash (slipstream) velocity Ve in m/s
        float Ve = rpm * 0.0254f * (propPitch + 0.5f) / 60f;

        // Propwash occurs when:
        // - Drone is falling along the thrust axis (descentSpeed > 0.5 m/s)
        // - Propellers are spinning and generating wash (Ve > 1.0 m/s)
        if (descentSpeed > 0.5f && Ve > 1.0f) {
            // Propwash is strongest when the descent speed matches the propeller wash velocity (ratio ≈ 1.0)
            float ratio = descentSpeed / Ve;
            // Gaussian envelope centered at 1.0
            float envelope = (float) Math.exp(-Math.pow(ratio - 1.0f, 2) / 0.25f);
            
            // Wash strength scales with RPM (Ve) up to a saturation limit
            float washStrength = Math.min(1.0f, Ve / 12.0f);
            
            float propwashFactor = washStrength * envelope;
            
            float intensityMultiplier = 0.0f;
            switch (level) {
                case LOW:
                    intensityMultiplier = 20.0f; // Max ~20 deg/s jitter
                    break;
                case MEDIUM:
                    intensityMultiplier = 45.0f; // Max ~45 deg/s jitter
                    break;
                case HIGH:
                    intensityMultiplier = 95.0f; // Max ~95 deg/s jitter (very violent)
                    break;
                case PERFECT:
                default:
                    break;
            }

            float maxJitter = propwashFactor * intensityMultiplier;

            // Generate bandpass/pink-like noise: new_noise = old_noise * (1 - alpha) + random * alpha
            float alpha = dt / (0.02f + dt); // 20ms correlation time
            float targetNoiseX = (random.nextFloat() * 2.0f - 1.0f) * maxJitter;
            float targetNoiseY = (random.nextFloat() * 2.0f - 1.0f) * maxJitter;

            noiseX = noiseX + (targetNoiseX - noiseX) * alpha;
            noiseY = noiseY + (targetNoiseY - noiseY) * alpha;

            // Propwash mainly affects pitch (Y) and roll (X), and very little yaw (Z)
            disturbance.x = noiseX; // Roll jitter
            disturbance.y = noiseY; // Pitch jitter
            disturbance.z = noiseX * 0.1f; // Minor yaw jitter
        } else {
            // Decay noise to zero when not in propwash conditions
            float decay = dt / (0.1f + dt);
            noiseX = noiseX + (0.0f - noiseX) * decay;
            noiseY = noiseY + (0.0f - noiseY) * decay;
        }

        return disturbance;
    }
}
