package com.iung.fpv20.gui.screens;

import com.iung.fpv20.Fpv20;
import com.iung.fpv20.Fpv20Client;
import com.iung.fpv20.config.Fpv20ConfigClientManual;
import com.iung.fpv20.config.Fpv20ConfigCommon;
import com.iung.fpv20.consts.Texts;
import com.iung.fpv20.consts.TranslateKeys;
import com.iung.fpv20.flying.GlobalFlying;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class OptionsMainScreen extends BackableScreen {

    public OptionsMainScreen(Screen parent) {
        super(Texts.TITLE_MAIN_OPTION_SCREEN, parent);
    }

    @Override
    protected void init() {
        super.init();
        int i = this.width / 2 - 155;
        int j = i + 160;
        int k = this.height / 6 - 12;
        int width = 150;
        int height = 20;

        this.addDrawableChild(ButtonWidget.builder(Texts.BTN_SELECT_CONTROLLER, (btn) -> {
                    if (this.client != null) {
                        this.client.setScreen(new SelectControllerScreen(this));
                    }
                })
                .dimensions(i, k, width, height).build());
        this.addDrawableChild(ButtonWidget.builder(Texts.BTN_CALIBRATE, (btn) -> {
                    if (this.client != null) {
                        this.client.setScreen(new CalibrationScreen(this));
                    }
                })
                .dimensions(j, k, width, height).build());
        k += 24;


        this.addDrawableChild(ButtonWidget.builder(Texts.BTN_SHOW_INPUT, (btn) -> {
                    if (this.client != null) {
                        this.client.setScreen(new ShowInputScreen(this));
                    }
                })
                .dimensions(i, k, width, height).build());
        this.addDrawableChild(ButtonWidget.builder(Texts.BTN_BTN_CONFIG, (btn) -> {
                    if (this.client != null) {
                        this.client.setScreen(new BtnConfigScreen(this));
                    }
                })
                .dimensions(j, k, width, height).build());
        k += 24;

        this.addDrawableChild(ButtonWidget.builder(Texts.BTN_LOAD_PRESET, (btn) -> {
                    if (this.client != null) {
                        this.client.setScreen(new PresetMenuScreen(this));
                    }
                })
                .dimensions(i, k, width, height).build());
        this.addDrawableChild(ButtonWidget.builder(Texts.BTN_POWER_PHYSICS_CONFIG, (btn) -> {
                    if (this.client != null) {
                        this.client.setScreen(new ThrustConfigScreen(this));
                    }
                })
                .dimensions(j, k, width, height).build());
        
        k += 24;
        String ratesTypeText = Fpv20Client.config1.rates.type;
        Text rateModelLabel = Text.translatable(TranslateKeys.BTN_RATES_MODEL, ratesTypeText != null ? ratesTypeText.toUpperCase() : "BETAFLIGHT");
        this.addDrawableChild(ButtonWidget.builder(rateModelLabel, (btn) -> {
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
                        this.client.setScreen(new OptionsMainScreen(this.parent));
                    }
                })
                .dimensions(i, k, width, height).build());
        Text fisheyeLabel = Text.translatable(TranslateKeys.BTN_FISHEYE, Fpv20Client.config1.enableFisheye ? Text.translatable("gui.yes") : Text.translatable("gui.no"));
        this.addDrawableChild(ButtonWidget.builder(fisheyeLabel, (btn) -> {
                    Fpv20Client.config1.enableFisheye = !Fpv20Client.config1.enableFisheye;
                    Fpv20Client.config1.save();
                    if (GlobalFlying.getFlying()) {
                        // Refresh shader immediately
                        GlobalFlying.setFlying(false);
                        GlobalFlying.setFlying(true);
                    }
                    if (this.client != null) {
                        this.client.setScreen(new OptionsMainScreen(this.parent));
                    }
                })
                .dimensions(j, k, width, height).build());


/////////////
        this.addDrawableChild(ButtonWidget.builder(Texts.BTN_FLY, (btn) -> {
                    GlobalFlying.setFlying(!GlobalFlying.getFlying());
                })
                .dimensions(0, 0, 40, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Fpv20Client.config.show_osd() ? Texts.BTN_OSD_ON : Texts.BTN_OSD_OFF, (btn) -> {
                Fpv20Client.config.setShow_osd(!Fpv20Client.config.show_osd());
                Fpv20Client.config.save();

                if (this.client != null) {
                    this.client.setScreen(new OptionsMainScreen(this.parent));
                }
            })
            .dimensions(45, 0, 80, 20).build());

        int mode = Fpv20Client.config.getSpeedDisplayMode();
        Text modeText;
        if (mode == 0) modeText = Text.translatable("gui.off");
        else if (mode == 1) modeText = Text.literal("b/s");
        else modeText = Text.literal("kb/h");

        Text speedBtnText = Text.translatable(TranslateKeys.BTN_SPEED_DISPLAY, modeText);

        this.addDrawableChild(ButtonWidget.builder(speedBtnText, (btn) -> {
                int nextMode = (Fpv20Client.config.getSpeedDisplayMode() + 1) % 3;
                Fpv20Client.config.setSpeedDisplayMode(nextMode);
                Fpv20Client.config.save();
                if (this.client != null) {
                    this.client.setScreen(new OptionsMainScreen(this.parent));
                }
            })
            .dimensions(130, 0, 80, 20).build());

//        this.addDrawable(new TextWidget(i, k, width, height, Text.of("ddd"), this.textRenderer));

//        this.addDrawable(new Value11Display(40, 40, 30, 50, () -> {
//            Controller controller = Fpv20Client.controller;
//            if (controller != null) {
//
//                try {
//                    return controller.floats[0];
//                } catch (NullPointerException n) {
//                    return 0;
//                }
//            } else {
//                return 0;
//            }
//        }));
    }

    @Override
    public void tick() {
        super.tick();
    }

}
