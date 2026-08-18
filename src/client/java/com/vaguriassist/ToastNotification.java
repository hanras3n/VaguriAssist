package com.vaguriassist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ToastNotification {

    private static final List<Toast> toasts = new ArrayList<>();
    private static final long DURATION_MS = 3000;
    private static final long SLIDE_MS = 300;

    public static void show(String text, String type) {
        toasts.add(new Toast(text, type, System.currentTimeMillis()));
    }

    public static void render(GuiGraphics g, float partialTick) {
        if (toasts.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        long now = System.currentTimeMillis();

        String side = ModConfig.INSTANCE.toastSide;
        int toastWidth = Math.max(160, font.width(textPlaceholder()) + 30);
        int toastHeight = Math.max(20, ModConfig.INSTANCE.toastHeight / 5);

        int startY = ModConfig.INSTANCE.toastY != -1 ? ModConfig.INSTANCE.toastY : ModConfig.INSTANCE.toastHeight;

        Iterator<Toast> it = toasts.iterator();
        int index = 0;
        while (it.hasNext()) {
            Toast toast = it.next();
            long age = now - toast.createdAt;

            if (age > DURATION_MS + SLIDE_MS) {
                it.remove();
                continue;
            }

            float progress = 1.0f;
            if (age < SLIDE_MS) {
                progress = age / (float) SLIDE_MS;
            } else if (age > DURATION_MS) {
                progress = 1.0f - (age - DURATION_MS) / (float) SLIDE_MS;
            }
            progress = Math.max(0, Math.min(1, progress));

            int bgColor, textColor;
            switch (toast.type) {
                case "success":
                    bgColor = 0xCC22AA44;
                    textColor = 0xFF55FF55;
                    break;
                case "error":
                    bgColor = 0xCCAA2222;
                    textColor = 0xFFFF5555;
                    break;
                case "warning":
                    bgColor = 0xCCAA8822;
                    textColor = 0xFFFFAA00;
                    break;
                default:
                    bgColor = 0xCC333333;
                    textColor = 0xFFFFFFFF;
            }

            int y = startY + index * (toastHeight + 4);
            int xOffset;
            if ("right".equals(side)) {
                xOffset = (int) ((screenWidth + 10) * (1.0f - progress));
                int tx = screenWidth - toastWidth - 4;
                drawToast(g, font, toast.text, tx, y, toastWidth, toastHeight, bgColor, textColor, progress);
            } else {
                xOffset = (int) (-(screenWidth + 10) * (1.0f - progress));
                int tx = 4;
                drawToast(g, font, toast.text, tx, y, toastWidth, toastHeight, bgColor, textColor, progress);
            }

            index++;
        }
    }

    private static void drawToast(GuiGraphics g, Font font, String text, int x, int y, int w, int h, int bgColor, int textColor, float alpha) {
        int a = (int) (0xFF * alpha);
        int bg = (a << 24) | (bgColor & 0x00FFFFFF);
        g.fill(x, y, x + w, y + h, bg);

        int borderColor = (a << 24) | (textColor & 0x00FFFFFF);
        g.fill(x, y, x + w, y + 1, borderColor);
        g.fill(x, y + h - 1, x + w, y + h, borderColor);
        g.fill(x, y, x + 1, y + h, borderColor);
        g.fill(x + w - 1, y, x + w, y + h, borderColor);

        int textA = (int) (0xFF * alpha);
        int tc = (textA << 24) | (textColor & 0x00FFFFFF);
        g.drawString(font, text, x + 8, y + (h - font.lineHeight) / 2, tc, true);
    }

    private static String textPlaceholder() {
        StringBuilder sb = new StringBuilder();
        for (Toast t : toasts) {
            if (t.text.length() > sb.length()) sb.setLength(0);
            sb.append(t.text);
        }
        return sb.length() > 0 ? sb.toString() : "VaguriAssist";
    }

    private static class Toast {
        final String text;
        final String type;
        final long createdAt;

        Toast(String text, String type, long createdAt) {
            this.text = text;
            this.type = type;
            this.createdAt = createdAt;
        }
    }
}
