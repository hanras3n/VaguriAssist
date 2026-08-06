package com.vaguriassist;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class GuiEditScreen extends Screen {

    private static final int SNAP_DISTANCE = 12;

    private final List<Element> elements = new ArrayList<>();
    private Element dragging = null;
    private boolean snapCenterX = false;
    private boolean snapCenterY = false;

    private static class Element {
        final String id;
        final String label;
        int x;
        int y;
        final int width;
        final int height;

        Element(String id, String label, int x, int y, int width, int height) {
            this.id = id;
            this.label = label;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    public GuiEditScreen() {
        super(Component.literal("VaguriAssist — Настройка GUI"));
    }

    @Override
    protected void init() {
        int hudW = this.font.width("VaguriAssist by Hanrasen");
        int hudDefX = this.width - hudW - 5;
        elements.add(new Element("hud", "Худ",
                ModConfig.INSTANCE.hudX != -1 ? ModConfig.INSTANCE.hudX : hudDefX,
                ModConfig.INSTANCE.hudY != -1 ? ModConfig.INSTANCE.hudY : 5,
                hudW, 9));

        int timerW = this.font.width("Проверка Steve [00:00]");
        int timerDefX = (this.width - timerW) / 2;
        elements.add(new Element("timer", "Таймер",
                ModConfig.INSTANCE.timerX != -1 ? ModConfig.INSTANCE.timerX : timerDefX,
                ModConfig.INSTANCE.timerY != -1 ? ModConfig.INSTANCE.timerY : this.height - 50,
                timerW, 9));

        int banW = 260;
        int banH = 80;
        int banDefX = this.width - banW - 10;
        int banDefY = this.height - banH - 10;
        elements.add(new Element("ban", "Бан-окно",
                ModConfig.INSTANCE.banWindowX != -1 ? ModConfig.INSTANCE.banWindowX : banDefX,
                ModConfig.INSTANCE.banWindowY != -1 ? ModConfig.INSTANCE.banWindowY : banDefY,
                banW, banH));

        this.addRenderableWidget(Button.builder(Component.literal("Готово"), b -> this.onClose())
                .bounds(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        for (Element e : elements) {
            if (event.x() >= e.x && event.x() <= e.x + e.width
                    && event.y() >= e.y && event.y() <= e.y + e.height) {
                dragging = e;
                snapCenterX = false;
                snapCenterY = false;
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double offsetX, double offsetY) {
        if (dragging != null) {
            dragging.x = (int) event.x() - (dragging.width / 2);
            dragging.y = (int) event.y() - (dragging.height / 2);

            int centerX = dragging.x + dragging.width / 2;
            int centerY = dragging.y + dragging.height / 2;

            snapCenterX = Math.abs(centerX - this.width / 2) <= SNAP_DISTANCE;
            snapCenterY = Math.abs(centerY - this.height / 2) <= SNAP_DISTANCE;

            if (snapCenterX) {
                dragging.x = this.width / 2 - dragging.width / 2;
            }
            if (snapCenterY) {
                dragging.y = this.height / 2 - dragging.height / 2;
            }

            dragging.x = Math.max(0, Math.min(this.width - dragging.width, dragging.x));
            dragging.y = Math.max(0, Math.min(this.height - dragging.height, dragging.y));
            return true;
        }
        return super.mouseDragged(event, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            switch (dragging.id) {
                case "hud":
                    ModConfig.INSTANCE.hudX = dragging.x;
                    ModConfig.INSTANCE.hudY = dragging.y;
                    break;
                case "timer":
                    ModConfig.INSTANCE.timerX = dragging.x;
                    ModConfig.INSTANCE.timerY = dragging.y;
                    break;
                case "ban":
                    ModConfig.INSTANCE.banWindowX = dragging.x;
                    ModConfig.INSTANCE.banWindowY = dragging.y;
                    break;
            }
            ModConfig.save();
            dragging = null;
            snapCenterX = false;
            snapCenterY = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font,
                "Перетащи элемент — он сам прилипнет к центру", this.width / 2, 24, 0xA0A0A0);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (snapCenterX) {
            guiGraphics.fill(centerX, 0, centerX + 1, this.height, 0xFFFF4444);
        }
        if (snapCenterY) {
            guiGraphics.fill(0, centerY, this.width, centerY + 1, 0xFFFF4444);
        }

        for (Element e : elements) {
            guiGraphics.fill(e.x, e.y, e.x + e.width, e.y + e.height, 0x66000000);
            guiGraphics.fill(e.x, e.y, e.x + e.width, e.y + 1, 0xFF888888);
            guiGraphics.fill(e.x, e.y + e.height - 1, e.x + e.width, e.y + e.height, 0xFF888888);
            guiGraphics.fill(e.x, e.y, e.x + 1, e.y + e.height, 0xFF888888);
            guiGraphics.fill(e.x + e.width - 1, e.y, e.x + e.width, e.y + e.height, 0xFF888888);

            guiGraphics.drawString(this.font, e.label, e.x + 4, e.y + 1, 0xFFFFAA00, false);

            if (e.id.equals("ban")) {
                guiGraphics.fill(e.x + 2, e.y + 12, e.x + e.width - 2, e.y + 14, 0xFFFF5555);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
