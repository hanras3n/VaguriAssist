package com.vaguriassist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class VaguriHUD {
    public static void render(GuiGraphics guiGraphics) {
        if (!ModConfig.INSTANCE.hudEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        String text = "VaguriAssist by Hanrasen";
        int x = mc.getWindow().getGuiScaledWidth() - mc.font.width(text) - 5;
        drawGoldShimmerText(guiGraphics, mc.font, text, x, 5);

        // Таймер проверки: по центру над хотбаром, белый -> жёлтый -> красный
        String nick = VaguriAssistClient.getCurrentNick();
        if (!nick.isEmpty()) {
            long elapsed = VaguriAssistClient.getCheckElapsedMs();
            String timerText = "Проверка " + nick + " [" + formatTime(elapsed) + "]";
            int color = elapsed >= VaguriAssistClient.FIVE_MIN_MS ? 0xFFFF5555
                    : elapsed >= VaguriAssistClient.WARN_AT_MS ? 0xFFFFAA00 : 0xFFFFFFFF;
            int timerX = (mc.getWindow().getGuiScaledWidth() - mc.font.width(timerText)) / 2;
            int timerY = mc.getWindow().getGuiScaledHeight() - 50;
            guiGraphics.drawString(mc.font, timerText, timerX, timerY, color, true);
        }
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
