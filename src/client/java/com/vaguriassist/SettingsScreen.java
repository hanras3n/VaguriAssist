package com.vaguriassist;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingsScreen extends Screen {
    public SettingsScreen() {
        super(Component.literal("VaguriAssist Settings"));
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = this.height / 2 - 60;

        this.addRenderableWidget(Button.builder(
                Component.literal("Show HUD: " + (ModConfig.INSTANCE.hudEnabled ? "ON" : "OFF")),
                (b) -> {
                    ModConfig.INSTANCE.hudEnabled = !ModConfig.INSTANCE.hudEnabled;
                    b.setMessage(Component.literal("Show HUD: " + (ModConfig.INSTANCE.hudEnabled ? "ON" : "OFF")));
                    ModConfig.save();
                }
        ).bounds(x, y, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Drag Ban Window: " + (ModConfig.INSTANCE.draggableBanWindow ? "ON" : "OFF")),
                (b) -> {
                    ModConfig.INSTANCE.draggableBanWindow = !ModConfig.INSTANCE.draggableBanWindow;
                    b.setMessage(Component.literal("Drag Ban Window: " + (ModConfig.INSTANCE.draggableBanWindow ? "ON" : "OFF")));
                    ModConfig.save();
                }
        ).bounds(x, y + 25, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Debug Mode: " + (ModConfig.INSTANCE.DEBUG_MODE ? "ON" : "OFF")),
                (b) -> {
                    ModConfig.INSTANCE.DEBUG_MODE = !ModConfig.INSTANCE.DEBUG_MODE;
                    b.setMessage(Component.literal("Debug Mode: " + (ModConfig.INSTANCE.DEBUG_MODE ? "ON" : "OFF")));
                    ModConfig.save();
                }
        ).bounds(x, y + 50, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Done"),
                (b) -> this.onClose()
        ).bounds(x, y + 95, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mX, int mY, float p) {
        super.render(g, mX, mY, p);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 80, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
