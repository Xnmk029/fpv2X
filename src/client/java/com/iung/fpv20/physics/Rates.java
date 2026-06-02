package com.iung.fpv20.physics;

public class Rates {
    /**
     * 根据不同的 Rate 算法计算真实的物理角速度（度/秒）
     * 
     * @param input             手柄经物理校准后的原始线性输入量，范围 [-1, 1]
     * @param type              Rate 类型："betaflight"、"actual" 或 "kiss"
     * @param rate              常规 Rate 参数（如 KISS 的 rate，Betaflight 的 rcRate）
     * @param superRate         Super Rate 参数（KISS / Betaflight 的 superRate）
     * @param expo              Expo 参数（在 KISS 中映射为 curve）
     * @param centerSensitivity 中心灵敏度（仅用于 Actual 模式）
     * @param maxRate           最大角速度限制（仅用于 Actual 模式）
     * @return 真实的角速度（度/秒）
     */
    public static float calculateRate(
            float input,
            String type,
            float rate,
            float superRate,
            float expo,
            float centerSensitivity,
            float maxRate
    ) {
        float inputAbs = Math.abs(input);
        
        if ("betaflight".equalsIgnoreCase(type)) {
            // Betaflight 3阶 Expo 曲线
            float rcCommandf = input * inputAbs * inputAbs * inputAbs * expo + input * (1.0f - expo);
            float rcRate = rate;
            if (rcRate > 2.0f) {
                // BF 极高 Rate 线性放大修正
                rcRate = rcRate + 14.54f * (rcRate - 2.0f);
            }
            float rcSuperfactor = 1.0f / Math.max(0.01f, 1.0f - inputAbs * superRate);
            return 200.0f * rcRate * rcCommandf * rcSuperfactor;
            
        } else if ("actual".equalsIgnoreCase(type)) {
            // Actual Rates 5阶 Expo 曲线
            float expoFactor = inputAbs * (input * input * input * input * input * expo + input * (1.0f - expo));
            float stickMovement = Math.max(0.0f, maxRate - centerSensitivity);
            return input * centerSensitivity + stickMovement * expoFactor;
            
        } else if ("kiss".equalsIgnoreCase(type)) {
            // KISS Rates 曲线
            float kissRpyUseRates = 1.0f / Math.max(0.01f, 1.0f - inputAbs * superRate);
            // expo 参数在 KISS 里作为 3阶的 curve
            float kissRcCommandf = (input * input * input * expo + input * (1.0f - expo)) * (rate / 10.0f);
            float kissAngle = 2000.0f * kissRpyUseRates * kissRcCommandf;
            return Math.max(-1998.0f, Math.min(1998.0f, kissAngle));
        }
        
        // 默认回退（Fallback）：每秒 300 度线性映射
        return input * 300.0f;
    }
}
