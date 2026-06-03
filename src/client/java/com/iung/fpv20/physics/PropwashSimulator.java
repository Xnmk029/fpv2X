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

        // Propwash occurs when falling downward relative to the prop disc (descentSpeed > 1.5 m/s)
        // and throttle is applied (throttle > 0.1)
        if (descentSpeed > 1.5f && throttle > 0.1f) {
            // Factor is proportional to descent speed (up to a limit) and throttle
            // Propwash peaks when throttle is around 0.3 - 0.7 during a pull-out
            float propwashFactor = Math.min(1.0f, (descentSpeed - 1.5f) / 10.0f) * throttle;
            
            float intensityMultiplier = 0.0f;
            switch (level) {
                case LOW:
                    intensityMultiplier = 15.0f; // Max ~15 deg/s jitter
                    break;
                case MEDIUM:
                    intensityMultiplier = 35.0f; // Max ~35 deg/s jitter
                    break;
                case HIGH:
                    intensityMultiplier = 75.0f; // Max ~75 deg/s jitter (very violent)
                    break;
                case PERFECT:
                default:
                    break;
            }

            float maxJitter = propwashFactor * intensityMultiplier;

            // Generate bandpass/pink-like noise: new_noise = old_noise * (1 - alpha) + random * alpha
            // Typically FPV propwash frequency is around 20Hz - 50Hz.
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
            // Decay noise to zero when not in propwash
            float decay = dt / (0.1f + dt);
            noiseX = noiseX + (0.0f - noiseX) * decay;
            noiseY = noiseY + (0.0f - noiseY) * decay;
        }

        return disturbance;
    }
}
