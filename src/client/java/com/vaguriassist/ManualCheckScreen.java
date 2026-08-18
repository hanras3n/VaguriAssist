package com.vaguriassist;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ManualCheckScreen extends Screen {

    private static final String[][] MODES = {
            {"classic", "Classic"},
            {"lite", "Lite"},
            {"lite120", "Lite 1.20"}
    };

    private static final String[][] REASONS = {
            {"report", "Репорт"},
            {"checkout", "Проверка"},
            {"autobuy", "Автобай"},
            {"autosell", "Автоселл"},
            {"customka", "Кастомка"},
            {"candidate", "Кандидат"}
    };

    private static final int WIN_W = 300;
    private static final int WIN_H = 320;

    private int winX;
    private int winY;
    private boolean dragging;
    private int dragDX;
    private int dragDY;

    private EditBox nickBox;
    private EditBox serverBox;
    private EditBox customReasonBox;

    private String prefillNick = "";

    private String selectedMode = "classic";
    private String selectedReason = null;

    public ManualCheckScreen() {
        super(Component.literal("VaguriAssist — Внести проверку вручную"));
    }

    public ManualCheckScreen(String nick) {
        this();
        if (nick != null && !nick.isEmpty()) {
            this.prefillNick = nick;
        }
    }

    @Override
    protected void init() {
        int defX = (this.width - WIN_W) / 2;
        int defY = Math.max(4, (this.height - WIN_H) / 2);
        this.winX = ModConfig.INSTANCE.checkWindowX != -1 ? ModConfig.INSTANCE.checkWindowX : defX;
        this.winY = ModConfig.INSTANCE.checkWindowY != -1 ? ModConfig.INSTANCE.checkWindowY : defY;
        clampWindow();

        int bx = winX + 10;
        int bw = WIN_W - 20;

        this.nickBox = new EditBox(this.font, bx, winY + 30, bw, 20, Component.literal("Ник игрока"));
        this.nickBox.setMaxLength(32);
        this.nickBox.setHint(Component.literal("Ник игрока"));
        if (!prefillNick.isEmpty()) {
            this.nickBox.setValue(prefillNick);
        }
        this.addRenderableWidget(this.nickBox);

        this.serverBox = new EditBox(this.font, bx, winY + 70, bw, 20, Component.literal("Номер анархии"));
        this.serverBox.setMaxLength(8);
        this.serverBox.setValue("0");
        this.addRenderableWidget(this.serverBox);

        int btnW = (WIN_W - 20 - 12) / 3;
        int btnH = 20;
        int modesY = winY + 108;
        for (int i = 0; i < MODES.length; i++) {
            final String mode = MODES[i][0];
            final String label = MODES[i][1];
            int col = i % 3;
            this.addRenderableWidget(Button.builder(
                    Component.literal(label),
                    b -> {
                        selectedMode = mode;
                        b.setMessage(Component.literal("► " + label));
                        refreshReasonLabels();
                    }
            ).bounds(winX + 10 + col * (btnW + 6), modesY, btnW, btnH).build());
        }
        refreshModeLabels();

        int reasonsY = winY + 140;
        int rBtnW = (WIN_W - 20 - 6) / 2;
        for (int i = 0; i < REASONS.length; i++) {
            final String reason = REASONS[i][0];
            final String label = REASONS[i][1];
            int col = i % 2;
            int row = i / 2;
            this.addRenderableWidget(Button.builder(
                    Component.literal(label),
                    b -> {
                        selectedReason = reason;
                        refreshReasonLabels();
                    }
            ).bounds(winX + 10 + col * (rBtnW + 6), reasonsY + row * (btnH + 4), rBtnW, btnH).build());
        }

        this.customReasonBox = new EditBox(this.font, bx, winY + 234, bw, 20, Component.literal("Своя причина (опц.)"));
        this.customReasonBox.setMaxLength(32);
        this.customReasonBox.setHint(Component.literal("Своя причина (опц.)"));
        this.addRenderableWidget(this.customReasonBox);

        this.addRenderableWidget(Button.builder(
                Component.literal("Внести проверку"),
                b -> submit()
        ).bounds(winX + 10, winY + WIN_H - 28, bw, 22).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Закрыть"),
                b -> this.onClose()
        ).bounds(winX + WIN_W - 93, winY + 8, 83, 20).build());
    }

    private void refreshModeLabels() {
        var widgets = this.children().stream()
                .filter(w -> w instanceof Button)
                .map(w -> (Button) w)
                .toList();
        int reasonBtnCount = REASONS.length;
        int submitAndClose = 2;
        int modeBtnStart = widgets.size() - reasonBtnCount - submitAndClose - MODES.length;
        for (int i = 0; i < MODES.length; i++) {
            int idx = modeBtnStart + i;
            if (idx >= 0 && idx < widgets.size()) {
                Button btn = widgets.get(idx);
                String label = MODES[i][1];
                btn.setMessage(Component.literal(selectedMode.equals(MODES[i][0]) ? "► " + label : label));
            }
        }
    }

    private void refreshReasonLabels() {
        var widgets = this.children().stream()
                .filter(w -> w instanceof Button)
                .map(w -> (Button) w)
                .toList();
        int reasonBtnStart = widgets.size() - REASONS.length - 2; // минус submit и close
        for (int i = 0; i < REASONS.length; i++) {
            int idx = reasonBtnStart + i;
            if (idx >= 0 && idx < widgets.size()) {
                Button btn = widgets.get(idx);
                String label = REASONS[i][1];
                if (selectedReason != null && selectedReason.equals(REASONS[i][0])) {
                    btn.setMessage(Component.literal("► " + label));
                } else {
                    btn.setMessage(Component.literal(label));
                }
            }
        }
    }

    private void submit() {
        String nick = this.nickBox.getValue().trim();
        if (nick.isEmpty()) {
            this.nickBox.setFocused(true);
            return;
        }
        int anarchyNumber;
        try {
            anarchyNumber = Integer.parseInt(this.serverBox.getValue().trim());
        } catch (Exception e) {
            anarchyNumber = 0;
        }
        String reason = selectedReason;
        if (reason == null) {
            String custom = this.customReasonBox.getValue().trim().toLowerCase();
            if (!custom.isEmpty()) {
                reason = custom;
            }
        }
        if (reason == null) {
            reason = "checkout";
        }
        final String reasonText = reason;

        if ("online".equals(ModConfig.INSTANCE.checkLogMode)) {
            JournalAPI.getInstance().startCheckout(nick, reason, selectedMode, anarchyNumber)
                    .thenAccept(success -> {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            if (success) {
                                mc.player.displayClientMessage(Component.literal(
                                        "§8[§6VaguriAssist§8] §aВнесено в журнал: §e" + nick), false);
                            } else {
                                mc.player.displayClientMessage(Component.literal(
                                        "§8[§6VaguriAssist§8] §cНе внесено — причина: §e" + reasonText), false);
                            }
                        }
                    });
        } else {
            CheckLogger.logCheck(nick, reason, selectedMode, anarchyNumber, false);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal(
                        "§8[§6VaguriAssist§8] §aПроверка внесена (оффлайн): §e" + nick), false);
            }
        }
        this.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mX, int mY, float p) {
        g.fill(winX, winY, winX + WIN_W, winY + WIN_H, 0xE0000000);

        g.fill(winX, winY, winX + WIN_W, winY + 1, 0xFF888888);
        g.fill(winX, winY + WIN_H - 1, winX + WIN_W, winY + WIN_H, 0xFF888888);
        g.fill(winX, winY, winX + 1, winY + WIN_H, 0xFF888888);
        g.fill(winX + WIN_W - 1, winY, winX + WIN_W, winY + WIN_H, 0xFF888888);

        g.fill(winX + 1, winY + 1, winX + WIN_W - 1, winY + 14, 0xFFFF5555);
        g.drawCenteredString(this.font, "Внести проверку вручную", winX + WIN_W / 2, winY + 4, 0xFFFFFFFF);

        g.drawString(this.font, "Ник игрока", winX + 12, winY + 18, 0xA0A0A0);
        g.drawString(this.font, "Номер анархии", winX + 12, winY + 58, 0xA0A0A0);
        g.drawCenteredString(this.font, "Режим:", winX + WIN_W / 2, winY + 96, 0x55FF55);
        g.drawCenteredString(this.font, "Причина:", winX + WIN_W / 2, winY + 128, 0x55FF55);
        String modeLabel = "Режим: " + selectedMode.toUpperCase();
        g.drawCenteredString(this.font, modeLabel, winX + WIN_W / 2, winY + WIN_H - 56, 0x55FFFF);

        String nickDisplay = this.nickBox.getValue().isEmpty() ? "—" : this.nickBox.getValue();
        g.drawCenteredString(this.font, Component.literal("Игрок: ").append(
                Component.literal(nickDisplay).withStyle(ChatFormatting.YELLOW)), winX + WIN_W / 2, winY + WIN_H - 44, 0xA0A0A0);

        super.render(g, mX, mY, p);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        int mx = (int) event.x();
        int my = (int) event.y();
        boolean inTitle = mx >= winX && mx < winX + WIN_W && my >= winY && my < winY + 14;
        boolean onClose = mx >= winX + WIN_W - 93 && mx < winX + WIN_W && my >= winY + 8 && my < winY + 28;
        if (inTitle && !onClose) {
            dragging = true;
            dragDX = mx - winX;
            dragDY = my - winY;
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double offsetX, double offsetY) {
        if (dragging) {
            winX = (int) event.x() - dragDX;
            winY = (int) event.y() - dragDY;
            clampWindow();
            repositionWidgets();
            return true;
        }
        return super.mouseDragged(event, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            ModConfig.INSTANCE.checkWindowX = winX;
            ModConfig.INSTANCE.checkWindowY = winY;
            ModConfig.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    private void clampWindow() {
        winX = Math.max(0, Math.min(this.width - WIN_W, winX));
        winY = Math.max(0, Math.min(this.height - WIN_H, winY));
    }

    private void repositionWidgets() {
        int bx = winX + 10;
        int bw = WIN_W - 20;
        this.nickBox.setPosition(bx, winY + 30);
        this.serverBox.setPosition(bx, winY + 70);
        this.customReasonBox.setPosition(bx, winY + 234);

        var widgets = this.children().stream().filter(w -> w instanceof Button).map(w -> (Button) w).toList();
        int btnW = (WIN_W - 20 - 12) / 3;
        int btnH = 20;
        int modesY = winY + 108;
        for (int i = 0; i < MODES.length && i < 3; i++) {
            widgets.get(i).setPosition(winX + 10 + i * (btnW + 6), modesY);
        }
        int rBtnW = (WIN_W - 20 - 6) / 2;
        int reasonsY = winY + 140;
        int reasonBtnStart = widgets.size() - REASONS.length - 1 - 1;
        for (int i = 0; i < REASONS.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int idx = reasonBtnStart + i;
            if (idx >= 0 && idx < widgets.size()) {
                widgets.get(idx).setPosition(winX + 10 + col * (rBtnW + 6), reasonsY + row * (btnH + 4));
            }
        }
        int submitIdx = widgets.size() - 2;
        int closeIdx = widgets.size() - 1;
        widgets.get(submitIdx).setPosition(winX + 10, winY + WIN_H - 28);
        widgets.get(closeIdx).setPosition(winX + WIN_W - 93, winY + 8);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
