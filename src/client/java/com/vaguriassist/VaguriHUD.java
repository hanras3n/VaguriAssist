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
        float hudScale = ModConfig.INSTANCE.hudScale;
        int x = ModConfig.INSTANCE.hudX != -1 ? ModConfig.INSTANCE.hudX
                : screenWidth - (int) (mc.font.width(text) * hudScale) - 5;
        int y = ModConfig.INSTANCE.hudY != -1 ? ModConfig.INSTANCE.hudY : 5;
        drawScaled(guiGraphics, x, y, hudScale,
                () -> drawGoldShimmerText(guiGraphics, mc.font, text, x, y));

		// Таймер проверки: переливается слева направо, меняет цвета по состоянию
		String nick = VaguriAssistClient.getCurrentNick();
		if (!nick.isEmpty()) {
			long elapsed = VaguriAssistClient.getCheckElapsedMs();
			String timerText = "Проверка " + nick + " [" + formatTime(elapsed) + "]";
			int colorA;
			int colorB;
			if (elapsed >= VaguriAssistClient.FIVE_MIN_MS) {
				colorA = 0xFFFF2828;
				colorB = 0xFFFFE000;
			} else if (elapsed >= VaguriAssistClient.WARN_AT_MS) {
				colorA = 0xFFFFC800;
				colorB = 0xFFFFFF60;
			} else {
				colorA = 0xFFFF69B4;
				colorB = 0xFF9B30FF;
			}
			float timerScale = ModConfig.INSTANCE.timerScale;
			int timerWidth = (int) (mc.font.width(timerText) * timerScale);
			int targetX = (screenWidth - timerWidth) / 2;
			int timerX;
			if (VaguriAssistClient.isTimerAnimating()) {
				float progress = VaguriAssistClient.getTimerAnimProgress();
				int fromX = VaguriAssistClient.getTimerAnimFromX();
				if (fromX == -1) {
					fromX = targetX;
				}
				timerX = fromX + (int) ((targetX - fromX) * easeOut(progress));
			} else {
				timerX = ModConfig.INSTANCE.timerX != -1 ? ModConfig.INSTANCE.timerX : targetX;
			}
			int timerY = ModConfig.INSTANCE.timerY != -1 ? ModConfig.INSTANCE.timerY
					: screenHeight - 50;
			drawScaled(guiGraphics, timerX, timerY, timerScale,
					() -> drawShimmerText(guiGraphics, mc.font, timerText, timerX, timerY, colorA, colorB));
		}

		// Правая панель: ник на проверке и режим
		if (!nick.isEmpty()) {
			String nickLine = "Проверка: " + nick;
			String detected = VaguriAssistClient.getDetectedMode();
			String modeLine = "Режим: " + (detected != null ? detected : VaguriAssistClient.getCurrentMode());
			String server = VaguriAssistClient.getDetectedServer();
			String serverLine = server.isEmpty() ? null : "Сервер: #" + server;
			int lines = serverLine != null ? 3 : 2;
			int panelW = Math.max(mc.font.width(nickLine), Math.max(mc.font.width(modeLine),
					serverLine != null ? mc.font.width(serverLine) : 0)) + 8;
			int panelX = screenWidth - panelW - 5;
			int lineHeight = mc.font.lineHeight + 2;
			int panelH = 4 + lineHeight * lines;
			int panelY = 5 + (int) (9 * ModConfig.INSTANCE.hudScale) + 2;
			guiGraphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0x88000000);
			guiGraphics.drawString(mc.font, nickLine, panelX + 4, panelY + 2, 0xFFFFFFFF, true);
			guiGraphics.drawString(mc.font, modeLine, panelX + 4, panelY + 2 + lineHeight, 0xFF55FF55, true);
			if (serverLine != null) {
				guiGraphics.drawString(mc.font, serverLine, panelX + 4, panelY + 2 + lineHeight * 2, 0xFF55FF55, true);
			}
		}
	}

	private static float easeOut(float t) {
		float u = 1.0f - t;
		return 1.0f - u * u * u;
	}

    private static void drawScaled(GuiGraphics g, int x, int y, float scale, Runnable draw) {
        if (scale == 1.0f) {
            draw.run();
            return;
        }
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        pose.translate(-x, -y);
        draw.run();
        pose.popMatrix();
    }

    private static void drawShimmerText(GuiGraphics g, Font font, String text, int x, int y, int colorA, int colorB) {
        if (text.isEmpty()) return;
        int offset = 0;
        long time = System.currentTimeMillis();
        float timeShift = (time % 2000) / 2000.0f;

        int r1 = (colorA >> 16) & 0xFF;
        int g1 = (colorA >> 8) & 0xFF;
        int b1 = colorA & 0xFF;
        int r2 = (colorB >> 16) & 0xFF;
        int g2 = (colorB >> 8) & 0xFF;
        int b2 = colorB & 0xFF;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            float progress = (float) i / text.length();
            float shiftedProgress = (progress + timeShift) % 1.0f;

            float wave = (float) Math.sin(shiftedProgress * Math.PI * 2);
            float pulse = (wave + 1.0f) / 2.0f;

            int r = (int) (r1 + (r2 - r1) * pulse);
            int gr = (int) (g1 + (g2 - g1) * pulse);
            int b = (int) (b1 + (b2 - b1) * pulse);
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
