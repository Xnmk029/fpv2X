package com.iung.fpv20.gui.screens;

import com.iung.fpv20.Fpv20Client;
import com.iung.fpv20.config.Fpv20ConfigClientManual.PidPreset;
import com.iung.fpv20.config.Fpv20ConfigClientManual.PropwashLevel;
import com.iung.fpv20.consts.TranslateKeys;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class FlightBehaviorScreen extends BackableScreen {

    public FlightBehaviorScreen(Screen parent) {
        super(Text.translatable(TranslateKeys.TITLE_FLIGHT_BEHAVIOR), parent);
    }

    @Override
    protected void init() {
        super.init();
        int i = this.width / 2 - 75;
        int k = this.height / 4;
        int width = 150;
        int height = 20;

        // Button 1: Rates Model
        String ratesTypeText = Fpv20Client.config1.rates.type;
        Text ratesBtnLabel = Text.translatable(TranslateKeys.BTN_RATES_MODEL, ratesTypeText != null ? ratesTypeText.toUpperCase() : "BETAFLIGHT");
        this.addDrawableChild(ButtonWidget.builder(ratesBtnLabel, (btn) -> {
            String currentType = Fpv20Client.config1.rates.type;
            String nextType = "betaflight";
            if ("betaflight".equalsIgnoreCase(currentType)) {
                nextType = "actual";
            } else if ("actual".equalsIgnoreCase(currentType)) {
                nextType = "kiss";
            }
            Fpv20Client.config1.rates.type = nextType;
            Fpv20Client.config1.save();
            if (this.client != null) {
                this.client.setScreen(new FlightBehaviorScreen(this.parent));
            }
        }).dimensions(i, k, width, height).build());

        k += 24;

        // Button 2: PID Preset
        PidPreset currentPid = Fpv20Client.config1.pidPreset;
        String pidKey = switch (currentPid) {
            case PERFECT -> TranslateKeys.PRESET_PID_PERFECT;
            case SNAPPY -> TranslateKeys.PRESET_PID_SNAPPY;
            case NORMAL -> TranslateKeys.PRESET_PID_NORMAL;
            case SOFT -> TranslateKeys.PRESET_PID_SOFT;
            case BOUNCEBACK -> TranslateKeys.PRESET_PID_BOUNCEBACK;
        };
        Text pidBtnLabel = Text.translatable(TranslateKeys.BTN_PID_PRESET_LABEL, Text.translatable(pidKey));
        this.addDrawableChild(ButtonWidget.builder(pidBtnLabel, (btn) -> {
            PidPreset current = Fpv20Client.config1.pidPreset;
            PidPreset next = PidPreset.NORMAL;
            switch (current) {
                case PERFECT -> next = PidPreset.SNAPPY;
                case SNAPPY -> next = PidPreset.NORMAL;
                case NORMAL -> next = PidPreset.SOFT;
                case SOFT -> next = PidPreset.BOUNCEBACK;
                case BOUNCEBACK -> next = PidPreset.PERFECT;
            }
            Fpv20Client.config1.pidPreset = next;
            Fpv20Client.config1.save();
            if (this.client != null) {
                this.client.setScreen(new FlightBehaviorScreen(this.parent));
            }
        }).dimensions(i, k, width, height).build());

        k += 24;

        // Button 3: Propwash Level
        PropwashLevel currentPropwash = Fpv20Client.config1.propwashLevel;
        String propwashKey = switch (currentPropwash) {
            case PERFECT -> TranslateKeys.PRESET_PROPWASH_PERFECT;
            case LOW -> TranslateKeys.PRESET_PROPWASH_LOW;
            case MEDIUM -> TranslateKeys.PRESET_PROPWASH_MEDIUM;
            case HIGH -> TranslateKeys.PRESET_PROPWASH_HIGH;
        };
        Text propwashBtnLabel = Text.translatable(TranslateKeys.BTN_PROPWASH_LEVEL_LABEL, Text.translatable(propwashKey));
        this.addDrawableChild(ButtonWidget.builder(propwashBtnLabel, (btn) -> {
            PropwashLevel current = Fpv20Client.config1.propwashLevel;
            PropwashLevel next = PropwashLevel.MEDIUM;
            switch (current) {
                case PERFECT -> next = PropwashLevel.LOW;
                case LOW -> next = PropwashLevel.MEDIUM;
                case MEDIUM -> next = PropwashLevel.HIGH;
                case HIGH -> next = PropwashLevel.PERFECT;
            }
            Fpv20Client.config1.propwashLevel = next;
            Fpv20Client.config1.save();
            if (this.client != null) {
                this.client.setScreen(new FlightBehaviorScreen(this.parent));
            }
        }).dimensions(i, k, width, height).build());
    }
}
