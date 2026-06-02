package com.iung.fpv20.utils;

import com.iung.fpv20.Fpv20Client;
import com.iung.fpv20.config.Fpv20ConfigClientManual;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class PresetLoader {
    /**
     * 加载一键配置预设并实时应用与持久化
     * 
     * @param name 预设名称："racing_photography" (竞速跟拍) 或 "freestyle_pro" (专业花飞)
     */
    public static void loadPreset(String name) {
        Fpv20ConfigClientManual config = Fpv20Client.config1;
        if (config == null) return;

        if ("racing_photography".equalsIgnoreCase(name)) {
            // 竞速跟拍：高保真 Actual Rates，关闭撞击弹性（防止拍摄晃动），柔和环境阻尼
            config.rates.type = "actual";
            
            config.rates.roll.centerSensitivity = 200.0f;
            config.rates.roll.maxRate = 500.0f;
            config.rates.roll.expo = 0.45f;
            
            config.rates.pitch.centerSensitivity = 200.0f;
            config.rates.pitch.maxRate = 500.0f;
            config.rates.pitch.expo = 0.45f;
            
            config.rates.yaw.centerSensitivity = 160.0f;
            config.rates.yaw.maxRate = 400.0f;
            config.rates.yaw.expo = 0.5f;
            
            config.physics.bounciness = 0.0f;
            config.physics.groundFriction = 0.1f;
            config.physics.foliageFriction = 0.2f;
            config.physics.waterDrag = 0.3f;
            
            config.enableFisheye = true;
            
            sendMessage("§a[FPV20] 成功加载预设：竞速跟拍 (Actual Rates | 无撞击反弹 | 高保真鱼眼)");
            
        } else if ("freestyle_pro".equalsIgnoreCase(name)) {
            // 专业花飞：高爆发 Betaflight Rates，开启弹性反弹，强环境阻尼以模拟重惯性
            config.rates.type = "betaflight";
            
            config.rates.roll.rate = 1.2f;
            config.rates.roll.superRate = 0.6f;
            config.rates.roll.expo = 0.4f;
            
            config.rates.pitch.rate = 1.2f;
            config.rates.pitch.superRate = 0.6f;
            config.rates.pitch.expo = 0.4f;
            
            config.rates.yaw.rate = 1.0f;
            config.rates.yaw.superRate = 0.6f;
            config.rates.yaw.expo = 0.4f;
            
            config.physics.bounciness = 0.4f;
            config.physics.groundFriction = 0.3f;
            config.physics.foliageFriction = 0.5f;
            config.physics.waterDrag = 0.8f;
            
            config.enableFisheye = true;
            
            sendMessage("§d[FPV20] 成功加载预设：专业花飞 (Betaflight Rates | 弹性反弹 | 高保真鱼眼)");
        }
        
        config.save();
    }

    private static void sendMessage(String text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.inGameHud != null && client.inGameHud.getChatHud() != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal(text));
        }
    }
}
