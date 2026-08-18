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
        int y = this.height / 2 - 90;

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
                Component.literal("Drag Timer: " + (ModConfig.INSTANCE.draggableTimer ? "ON" : "OFF")),
                (b) -> {
                    ModConfig.INSTANCE.draggableTimer = !ModConfig.INSTANCE.draggableTimer;
                    b.setMessage(Component.literal("Drag Timer: " + (ModConfig.INSTANCE.draggableTimer ? "ON" : "OFF")));
                    ModConfig.save();
                }
        ).bounds(x, y + 50, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Auto Unfreeze: " + (ModConfig.INSTANCE.autoUnfreeze ? "ON" : "OFF")),
                (b) -> {
                    ModConfig.INSTANCE.autoUnfreeze = !ModConfig.INSTANCE.autoUnfreeze;
                    b.setMessage(Component.literal("Auto Unfreeze: " + (ModConfig.INSTANCE.autoUnfreeze ? "ON" : "OFF")));
                    ModConfig.save();
                }
        ).bounds(x, y + 100, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Авто-ТП /warp logo: " + (ModConfig.INSTANCE.autoWarpLogo ? "ON" : "OFF")),
                (b) -> {
                    ModConfig.INSTANCE.autoWarpLogo = !ModConfig.INSTANCE.autoWarpLogo;
                    b.setMessage(Component.literal("Авто-ТП /warp logo: " + (ModConfig.INSTANCE.autoWarpLogo ? "ON" : "OFF")));
                    ModConfig.save();
                }
        ).bounds(x, y + 75, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Режим вноса проверок: " + getLogModeLabel()),
                (b) -> {
                    ModConfig.INSTANCE.checkLogMode = "offline".equals(ModConfig.INSTANCE.checkLogMode) ? "online" : "offline";
                    b.setMessage(Component.literal("Режим вноса проверок: " + getLogModeLabel()));
                    ModConfig.save();
                }
        ).bounds(x, y + 125, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Done"),
                (b) -> this.onClose()
        ).bounds(x, y + 215, 200, 20).build());
    }

    private static String getLogModeLabel() {
        return "offline".equals(ModConfig.INSTANCE.checkLogMode) ? "Оффлайн (txt)" : "Онлайн (журнал)";
    }

    @Override
    public void render(GuiGraphics g, int mX, int mY, float p) {
        super.render(g, mX, mY, p);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 110, 0xFFFFFFFF);
        if (ModConfig.INSTANCE.apiToken.isEmpty()) {
            g.drawCenteredString(this.font, "§cAPI токен не задан — используй /vaguriassist setapi <token>",
                    this.width / 2, this.height / 2 + 145, 0xFFFF5555);
        } else {
            String masked = ModConfig.INSTANCE.apiToken.substring(0, Math.min(8, ModConfig.INSTANCE.apiToken.length())) + "...";
            g.drawCenteredString(this.font, "§7API токен: " + masked,
                    this.width / 2, this.height / 2 + 145, 0x888888);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
