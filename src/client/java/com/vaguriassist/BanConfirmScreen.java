package com.vaguriassist;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class BanConfirmScreen extends Screen {
    private final String command;

    private int boxX;
    private int boxY;
    private final int boxWidth = Math.max(120, (int) (260 * ModConfig.INSTANCE.banScale));
    private final int boxHeight = Math.max(60, (int) (80 * ModConfig.INSTANCE.banScale));

    private boolean isDragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    private Button confirmBtn;
    private Button cancelBtn;

    public BanConfirmScreen(String command) {
        super(Component.literal("Confirm Ban"));
        this.command = command;
    }

    @Override
    protected void init() {
        if (ModConfig.INSTANCE.banWindowX != -1 && ModConfig.INSTANCE.banWindowY != -1) {
            boxX = ModConfig.INSTANCE.banWindowX;
            boxY = ModConfig.INSTANCE.banWindowY;
        } else {
            boxX = this.width - boxWidth - 10;
            boxY = this.height - boxHeight - 10;
        }

        boxX = Math.max(0, Math.min(this.width - boxWidth, boxX));
        boxY = Math.max(0, Math.min(this.height - boxHeight, boxY));

        confirmBtn = Button.builder(
                Component.literal("Забанить"),
                (b) -> {
                    String clean = command.startsWith("/") ? command.substring(1) : command;
                    String[] parts = clean.split(" ");
                    String nick = parts.length >= 2 ? parts[1] : "";
                    if (nick.isEmpty()) {
                        VaguriAssistClient.sendCommand(command);
                    } else {
                        BanSender.sendFreezing(nick);
                        VaguriAssistClient.scheduleBan(command, 100);
                    }
                    CheckLogger.logBan(nick, "автобай, " + command);
                    VaguriAssistClient.clearCurrentNick();
                    this.onClose();
                }
        ).bounds(boxX + 15, boxY + 55, 110, 20).build();

        cancelBtn = Button.builder(
                Component.literal("Отмена"),
                (b) -> this.onClose()
        ).bounds(boxX + boxWidth - 125, boxY + 55, 110, 20).build();

        this.addRenderableWidget(confirmBtn);
        this.addRenderableWidget(cancelBtn);
    }

    @Override
    public void render(GuiGraphics g, int mX, int mY, float p) {
        // Основной фон
        g.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xE0000000);

        // Рамка
        g.fill(boxX, boxY, boxX + boxWidth, boxY + 1, 0xFF555555); // Верх
        g.fill(boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, 0xFF555555); // Низ
        g.fill(boxX, boxY, boxX + 1, boxY + boxHeight, 0xFF555555); // Лево
        g.fill(boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF555555); // Право

        // Шапка окна
        g.fill(boxX + 1, boxY + 1, boxX + boxWidth - 1, boxY + 14, 0xFFFF5555);
        g.drawCenteredString(this.font, "Подтвердить бан", boxX + boxWidth / 2, boxY + 4, 0xFFFFFFFF);

        // Текст с командой (цвет передается прямо в drawString)
        g.drawString(this.font, "Команда:", boxX + 15, boxY + 22, 0xFFAAAAAA, false);

        // Обрезаем команду, если она слишком длинная
        String displayCmd = command;
        if (this.font.width(command) > boxWidth - 30) {
            while (this.font.width(displayCmd + "...") > boxWidth - 30 && displayCmd.length() > 0) {
                displayCmd = displayCmd.substring(0, displayCmd.length() - 1);
            }
            displayCmd += "...";
        }
        g.drawString(this.font, displayCmd, boxX + 15, boxY + 34, 0xFFFFAA00, false);

        super.render(g, mX, mY, p);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (ModConfig.INSTANCE.draggableBanWindow) {
            double mouseX = event.x();
            double mouseY = event.y();
            if (mouseX >= boxX && mouseX <= boxX + boxWidth && mouseY >= boxY && mouseY <= boxY + 14) {
                isDragging = true;
                dragOffsetX = (int) mouseX - boxX;
                dragOffsetY = (int) mouseY - boxY;
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double offsetX, double offsetY) {
        if (isDragging) {
            double mouseX = event.x();
            double mouseY = event.y();
            boxX = (int) mouseX - dragOffsetX;
            boxY = (int) mouseY - dragOffsetY;

            boxX = Math.max(0, Math.min(this.width - boxWidth, boxX));
            boxY = Math.max(0, Math.min(this.height - boxHeight, boxY));

            confirmBtn.setX(boxX + 15);
            confirmBtn.setY(boxY + 55);
            cancelBtn.setX(boxX + 135);
            cancelBtn.setY(boxY + 55);
            return true;
        }
        return super.mouseDragged(event, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (isDragging) {
            ModConfig.INSTANCE.banWindowX = boxX;
            ModConfig.INSTANCE.banWindowY = boxY;
            ModConfig.save();
        }
        isDragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
