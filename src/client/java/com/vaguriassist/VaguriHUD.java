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

        // Таймер проверки: переливается розово-фиолетовым слева направо
        String nick = VaguriAssistClient.getCurrentNick();
        if (!nick.isEmpty()) {
            String timerText = "Проверка " + nick + " [" + formatTime(VaguriAssistClient.getCheckElapsedMs()) + "]";
            int timerX = ModConfig.INSTANCE.timerX != -1 ? ModConfig.INSTANCE.timerX
                    : (screenWidth - mc.font.width(timerText)) / 2;
            int timerY = ModConfig.INSTANCE.timerY != -1 ? ModConfig.INSTANCE.timerY
                    : screenHeight - 50;
            drawPinkPurpleShimmerText(guiGraphics, mc.font, timerText, timerX, timerY);
        }
    }

    private static void drawPinkPurpleShimmerText(GuiGraphics g, Font font, String text, int x, int y) {
        if (text.isEmpty()) return;
        int offset = 0;
        long time = System.currentTimeMillis();
        float timeShift = (time % 2000) / 2000.0f;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            float progress = (float) i / text.length();
            float shiftedProgress = (progress + timeShift) % 1.0f;

            float wave = (float) Math.sin(shiftedProgress * Math.PI * 2);
            float pulse = (wave + 1.0f) / 2.0f;

            int r = (int) (255 + (155 - 255) * pulse);
            int gr = (int) (105 + (48 - 105) * pulse);
            int b = (int) (180 + (255 - 180) * pulse);
            int color = (0xFF << 24) | (r << 16) | (gr << 8) | b;

            g.drawString(font, String.valueOf(c), x + offset, y, color, true);
            offset += font.width(String.valueOf(c));
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
