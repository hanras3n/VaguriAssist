package com.vaguriassist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class VaguriHUD {
    public static void render(GuiGraphics guiGraphics) {
        if (!ModConfig.INSTANCE.hudEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        String text = "VaguriAssist by Hanrasen";
        int x = ModConfig.INSTANCE.hudX != -1 ? ModConfig.INSTANCE.hudX
                : screenWidth - mc.font.width(text) - 5;
        int y = ModConfig.INSTANCE.hudY != -1 ? ModConfig.INSTANCE.hudY : 5;
        drawGoldShimmerText(guiGraphics, mc.font, text, x, y);

        // Таймер проверки: переливается, меняет цвет при предупреждении и после 5 минут
        String nick = VaguriAssistClient.getCurrentNick();
        if (!nick.isEmpty()) {
            long elapsed = VaguriAssistClient.getCheckElapsedMs();
            String timerText = "Проверка " + nick + " [" + formatTime(elapsed) + "]";
            int color;
            if (elapsed >= VaguriAssistClient.FIVE_MIN_MS) {
                color = shimmerBetween(0xFFFF2020, 0xFFFF8080);
            } else if (elapsed >= VaguriAssistClient.WARN_AT_MS) {
                color = shimmerBetween(0xFFFFAA00, 0xFFFF5050);
            } else {
                color = shimmerBetween(0xFFB0B0B0, 0xFFFFFFFF);
            }
            int timerX = ModConfig.INSTANCE.timerX != -1 ? ModConfig.INSTANCE.timerX
                    : (screenWidth - mc.font.width(timerText)) / 2;
            int timerY = ModConfig.INSTANCE.timerY != -1 ? ModConfig.INSTANCE.timerY
                    : screenHeight - 50;
            guiGraphics.drawString(mc.font, timerText, timerX, timerY, color, true);
        }
    }

    private static int shimmerBetween(int colorA, int colorB) {
        float t = (System.currentTimeMillis() % 1200) / 1200.0f;
        float wave = (float) Math.sin(t * Math.PI * 2);
        float p = (wave + 1.0f) / 2.0f;
        int r = (int) (((colorA >> 16) & 0xFF) + ((((colorB >> 16) & 0xFF) - ((colorA >> 16) & 0xFF)) * p));
        int g = (int) (((colorA >> 8) & 0xFF) + ((((colorB >> 8) & 0xFF) - ((colorA >> 8) & 0xFF)) * p));
        int b = (int) ((colorA & 0xFF) + (((colorB & 0xFF) - (colorA & 0xFF)) * p));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private static void drawGoldShimmerText(GuiGraphics g, Font font, String text, int x, int y) {
        if (text.isEmpty()) return;
        int offset = 0;
        long time = System.currentTimeMillis();
        float timeShift = (time % 3000) / 3000.0f;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            float progress = (float) i / text.length();
            float shiftedProgress = (progress + timeShift) % 1.0f;

            float wave = (float) Math.sin(shiftedProgress * Math.PI * 2);
            float pulse = (wave + 1.0f) / 2.0f;

            int r = (int) (204 + (255 - 204) * pulse);
            int gr = (int) (153 + (255 - 153) * pulse);
            int b = (int) (0 + (153 - 0) * pulse);
            int color = (0xFF << 24) | (r << 16) | (gr << 8) | b;

            g.drawString(font, String.valueOf(c), x + offset, y, color, true);
            offset += font.width(String.valueOf(c));
        }
    }
}
