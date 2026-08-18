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
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 2.5f;

    private final List<Element> elements = new ArrayList<>();
    private Element dragging = null;
    private boolean snapCenterX = false;
    private boolean snapCenterY = false;

    private static class Element {
        final String id;
        final String label;
        int x;
        int y;
        final int baseWidth;
        final int baseHeight;
        float scale = 1.0f;
        boolean moving = false;
        boolean sliding = false;

        Element(String id, String label, int x, int y, int baseWidth, int baseHeight) {
            this.id = id;
            this.label = label;
            this.x = x;
            this.y = y;
            this.baseWidth = baseWidth;
            this.baseHeight = baseHeight;
        }

        int width() {
            return Math.max(1, (int) (baseWidth * scale));
        }

        int height() {
            return Math.max(1, (int) (baseHeight * scale));
        }
    }

    public GuiEditScreen() {
        super(Component.literal("VaguriAssist — Настройка GUI"));
    }

    @Override
    protected void init() {
        int hudW = this.font.width("VaguriAssist by Hanrasen");
        int hudDefX = this.width - (int) (hudW * ModConfig.INSTANCE.hudScale) - 5;
        Element hud = new Element("hud", "Худ",
                ModConfig.INSTANCE.hudX != -1 ? ModConfig.INSTANCE.hudX : hudDefX,
                ModConfig.INSTANCE.hudY != -1 ? ModConfig.INSTANCE.hudY : 5,
                hudW, 9);
        hud.scale = ModConfig.INSTANCE.hudScale;
        elements.add(hud);

        int timerW = this.font.width("Проверка Steve [00:00]");
        int timerDefX = (this.width - (int) (timerW * ModConfig.INSTANCE.timerScale)) / 2;
        Element timer = new Element("timer", "Таймер",
                ModConfig.INSTANCE.timerX != -1 ? ModConfig.INSTANCE.timerX : timerDefX,
                ModConfig.INSTANCE.timerY != -1 ? ModConfig.INSTANCE.timerY : this.height - 50,
                timerW, 9);
        timer.scale = ModConfig.INSTANCE.timerScale;
        elements.add(timer);

        Element ban = new Element("ban", "Бан-окно",
                ModConfig.INSTANCE.banWindowX != -1 ? ModConfig.INSTANCE.banWindowX : this.width - 260 - 10,
                ModConfig.INSTANCE.banWindowY != -1 ? ModConfig.INSTANCE.banWindowY : this.height - 80 - 10,
                260, 80);
        ban.scale = ModConfig.INSTANCE.banScale;
        elements.add(ban);

        int toastW = 160;
        int toastH = Math.max(20, ModConfig.INSTANCE.toastHeight / 5);
        int toastX = "right".equals(ModConfig.INSTANCE.toastSide)
                ? this.width - toastW - 4 : 4;
        int toastY = ModConfig.INSTANCE.toastY != -1 ? ModConfig.INSTANCE.toastY : ModConfig.INSTANCE.toastHeight;
        Element toast = new Element("toast", "Уведомления", toastX, toastY, toastW, toastH);
        toast.scale = 1.0f;
        elements.add(toast);

        this.addRenderableWidget(Button.builder(Component.literal("Сторона уведомлений: " + ModConfig.INSTANCE.toastSide),
                (b) -> {
                    ModConfig.INSTANCE.toastSide = "left".equals(ModConfig.INSTANCE.toastSide) ? "right" : "left";
                    b.setMessage(Component.literal("Сторона уведомлений: " + ModConfig.INSTANCE.toastSide));
                    ModConfig.save();
                }
        ).bounds(this.width / 2 - 100, this.height - 56, 200, 20).build());

        int[] heightOptions = {50, 75, 100, 125, 150, 200};
        int currentHeightIdx = 0;
        for (int i = 0; i < heightOptions.length; i++) {
            if (heightOptions[i] == ModConfig.INSTANCE.toastHeight) {
                currentHeightIdx = i;
                break;
            }
        }
        final int heightIdx = currentHeightIdx;
        this.addRenderableWidget(Button.builder(Component.literal("Высота уведомлений: " + ModConfig.INSTANCE.toastHeight),
                (b) -> {
                    int nextIdx = (heightIdx + 1) % heightOptions.length;
                    ModConfig.INSTANCE.toastHeight = heightOptions[nextIdx];
                    b.setMessage(Component.literal("Высота уведомлений: " + ModConfig.INSTANCE.toastHeight));
                    ModConfig.save();
                }
        ).bounds(this.width / 2 - 100, this.height - 32, 200, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Готово"), b -> this.onClose())
                .bounds(this.width / 2 - 50, this.height - 8, 100, 20).build());
    }

    private boolean isOnSlider(Element e, double mouseX, double mouseY) {
        int sliderY = e.y + e.height() + 6;
        return mouseX >= e.x && mouseX <= e.x + e.width() && mouseY >= sliderY && mouseY <= sliderY + 8;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        for (Element e : elements) {
            if (isOnSlider(e, event.x(), event.y())) {
                dragging = e;
                e.sliding = true;
                snapCenterX = false;
                snapCenterY = false;
                return true;
            }
        }
        for (Element e : elements) {
            if (event.x() >= e.x && event.x() <= e.x + e.width()
                    && event.y() >= e.y && event.y() <= e.y + e.height()) {
                dragging = e;
                e.moving = true;
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
            if (dragging.sliding) {
                int sliderWidth = dragging.width();
                float ratio = (float) (event.x() - dragging.x) / sliderWidth;
                ratio = Math.max(0.0f, Math.min(1.0f, ratio));
                dragging.scale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * ratio;
                clampElement(dragging);
                return true;
            }
            if (dragging.moving) {
                dragging.x = (int) event.x() - (dragging.width() / 2);
                dragging.y = (int) event.y() - (dragging.height() / 2);

                int centerX = dragging.x + dragging.width() / 2;
                int centerY = dragging.y + dragging.height() / 2;

                snapCenterX = Math.abs(centerX - this.width / 2) <= SNAP_DISTANCE;
                snapCenterY = Math.abs(centerY - this.height / 2) <= SNAP_DISTANCE;

                if (snapCenterX) {
                    dragging.x = this.width / 2 - dragging.width() / 2;
                }
                if (snapCenterY) {
                    dragging.y = this.height / 2 - dragging.height() / 2;
                }

                clampElement(dragging);
                return true;
            }
        }
        return super.mouseDragged(event, offsetX, offsetY);
    }

    private void clampElement(Element e) {
        e.x = Math.max(0, Math.min(this.width - e.width(), e.x));
        e.y = Math.max(0, Math.min(this.height - e.height(), e.y));
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            switch (dragging.id) {
                case "hud":
                    ModConfig.INSTANCE.hudX = dragging.x;
                    ModConfig.INSTANCE.hudY = dragging.y;
                    ModConfig.INSTANCE.hudScale = dragging.scale;
                    break;
                case "timer":
                    ModConfig.INSTANCE.timerX = dragging.x;
                    ModConfig.INSTANCE.timerY = dragging.y;
                    ModConfig.INSTANCE.timerScale = dragging.scale;
                    break;
                case "ban":
                    ModConfig.INSTANCE.banWindowX = dragging.x;
                    ModConfig.INSTANCE.banWindowY = dragging.y;
                    ModConfig.INSTANCE.banScale = dragging.scale;
                    break;
                case "toast":
                    ModConfig.INSTANCE.toastY = dragging.y;
                    break;
            }
            ModConfig.save();
            dragging.moving = false;
            dragging.sliding = false;
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
                "Перетащи элемент — прилипнет к центру. Ползунок под элементом меняет размер",
                this.width / 2, 24, 0xA0A0A0);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (snapCenterX) {
            guiGraphics.fill(centerX, 0, centerX + 1, this.height, 0xFFFF4444);
        }
        if (snapCenterY) {
            guiGraphics.fill(0, centerY, this.width, centerY + 1, 0xFFFF4444);
        }

        for (Element e : elements) {
            int w = e.width();
            int h = e.height();

            guiGraphics.fill(e.x, e.y, e.x + w, e.y + h, 0x66000000);
            guiGraphics.fill(e.x, e.y, e.x + w, e.y + 1, 0xFF888888);
            guiGraphics.fill(e.x, e.y + h - 1, e.x + w, e.y + h, 0xFF888888);
            guiGraphics.fill(e.x, e.y, e.x + 1, e.y + h, 0xFF888888);
            guiGraphics.fill(e.x + w - 1, e.y, e.x + w, e.y + h, 0xFF888888);

            guiGraphics.drawString(this.font, e.label, e.x + 4, e.y + 1, 0xFFFFAA00, false);

            if (e.id.equals("ban")) {
                guiGraphics.fill(e.x + 2, e.y + 12, e.x + w - 2, e.y + 14, 0xFFFF5555);
            }

            if (!e.id.equals("toast")) {
                int sliderY = e.y + h + 6;
                guiGraphics.fill(e.x, sliderY, e.x + w, sliderY + 4, 0xAA555555);
                int handleX = e.x + 2 + (int) (((e.scale - MIN_SCALE) / (MAX_SCALE - MIN_SCALE)) * (w - 4));
                guiGraphics.fill(handleX - 2, sliderY - 2, handleX + 2, sliderY + 6, 0xFF66CCFF);

                String scaleLabel = String.format("%s: x%.2f", e.label, e.scale);
                guiGraphics.drawString(this.font, scaleLabel, e.x + 4, sliderY + 6, 0xFFFFFFFF, false);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
