package com.iung.fpv20.physics;

import com.iung.fpv20.config.Fpv20ConfigClientManual.PidPreset;

public class VirtualPIDController {
    private float errorSum = 0.0f;
    private float lastError = 0.0f;
    
    // PT1 Filter state for output smoothing (simulates motor spool-up delay)
    private float filteredGyro = 0.0f;

    public void reset() {
        this.errorSum = 0.0f;
        this.lastError = 0.0f;
        this.filteredGyro = 0.0f;
    }

    public float update(float setpoint, float current, float dt, PidPreset preset) {
        if (preset == PidPreset.PERFECT) {
            return setpoint;
        }

        // Define PID gains and filter time constant (T) based on preset
        float kP, kI, kD;
        float lpfT; // PT1 Lowpass filter time constant (seconds). Smaller = faster motor response.

        switch (preset) {
            case SNAPPY:
                kP = 55.0f;
                kI = 15.0f;
                kD = 1.8f;
                lpfT = 0.003f; // 3ms delay (extremely close to perfect)
                break;
            case NORMAL:
                kP = 40.0f;
                kI = 12.0f;
                kD = 1.2f;
                lpfT = 0.006f; // 6ms delay (very responsive, natural feel)
                break;
            case SOFT:
                kP = 28.0f;
                kI = 10.0f;
                kD = 0.8f;
                lpfT = 0.012f; // 12ms delay (smooth but still highly responsive)
                break;
            case BOUNCEBACK:
                kP = 65.0f; // High P for crisp response and strong overshoot
                kI = 10.0f;
                kD = 0.3f;  // Very low D to allow underdamped oscillations
                lpfT = 0.004f; // 4ms delay
                break;
            case PERFECT:
            default:
                return setpoint;
        }

        float error = setpoint - current;
        errorSum += error * dt;
        
        // Clamp error sum to prevent windup
        errorSum = Math.max(-500.0f, Math.min(500.0f, errorSum));

        float dError = (error - lastError) / Math.max(0.001f, dt);
        lastError = error;

        // Calculate angular acceleration (torque)
        float angularAcceleration = kP * error + kI * errorSum + kD * dError;

        // Update raw gyro rate
        float rawGyro = current + angularAcceleration * dt;

        // Apply PT1 lowpass filter to simulate motor lag (spool-up latency)
        // Formula: y(t) = y(t-dt) + (x(t) - y(t-dt)) * (dt / (T + dt))
        filteredGyro = filteredGyro + (rawGyro - filteredGyro) * (dt / (lpfT + dt));

        return filteredGyro;
    }
}
