package com.iung.fpv20.gui.screens;

import com.iung.fpv20.Fpv20;
import com.iung.fpv20.Fpv20Client;
import com.iung.fpv20.config.Fpv20ConfigClientManual;
import com.iung.fpv20.config.Fpv20ConfigCommon;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class PresetMenuScreen extends BackableScreen {

    public PresetMenuScreen(Screen parent) {
        super(com.iung.fpv20.consts.Texts.BTN_LOAD_PRESET, parent);
    }

    @Override
    protected void init() {
        super.init();
        int i = this.width / 2 - 75;
        int k = this.height / 4;
        int width = 150;
        int height = 20;

        this.addDrawableChild(ButtonWidget.builder(com.iung.fpv20.consts.Texts.BTN_RELOAD_CONFIG_MENU, (btn) -> {
                    Fpv20Client.config1 = Fpv20ConfigClientManual.createAndLoad();
                    Fpv20.config = Fpv20ConfigCommon.createAndLoad();
                    if (this.client != null) {
                        this.client.setScreen(this.parent);
                    }
                })
                .dimensions(i, k, width, height).build());
        k += 24;

        this.addDrawableChild(ButtonWidget.builder(com.iung.fpv20.consts.Texts.BTN_LOAD_RACING_PRESET, (btn) -> {
                    com.iung.fpv20.utils.PresetLoader.loadPreset("racing_photography");
                    if (this.client != null) {
                        this.client.setScreen(this.parent);
                    }
                })
                .dimensions(i, k, width, height).build());
        k += 24;

        this.addDrawableChild(ButtonWidget.builder(com.iung.fpv20.consts.Texts.BTN_LOAD_FREESTYLE_PRESET, (btn) -> {
                    com.iung.fpv20.utils.PresetLoader.loadPreset("freestyle_pro");
                    if (this.client != null) {
                        this.client.setScreen(this.parent);
                    }
                })
                .dimensions(i, k, width, height).build());
        k += 24;

        Text debugTextLabel = Text.translatable(com.iung.fpv20.consts.TranslateKeys.BTN_TELEMETRY_DEBUG, Fpv20Client.config1.show_telemetry_debug ? Text.translatable("gui.yes") : Text.translatable("gui.no"));
        this.addDrawableChild(ButtonWidget.builder(debugTextLabel, (btn) -> {
                    Fpv20Client.config1.show_telemetry_debug = !Fpv20Client.config1.show_telemetry_debug;
                    Fpv20Client.config1.save();
                    if (this.client != null) {
                        this.client.setScreen(new PresetMenuScreen(this.parent));
                    }
                })
                .dimensions(i, k, width, height).build());
    }
}
