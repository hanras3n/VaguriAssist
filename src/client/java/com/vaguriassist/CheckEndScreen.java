package com.vaguriassist;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class CheckEndScreen extends Screen {

    private static final String[][] RESULTS = {
            {"ban", "Забанен"},
            {"clean", "Чист"},
            {"autobuy", "Автобай"},
            {"autosell", "Автоселл"}
    };

    private static final int WIN_W = 280;
    private static final int WIN_H = 250;

    private int winX;
    private int winY;
    private boolean dragging;
    private int dragDX;
    private int dragDY;

    private final String nick;
    private final boolean wasBanned;
    private final String defaultBanReason;

    private EditBox banReasonBox;
    private Checkbox stashCheckbox;

    private String selectedResult;

    public CheckEndScreen(String nick, boolean wasBanned, String defaultBanReason) {
        super(Component.literal("VaguriAssist — Завершение проверки"));
        this.nick = nick == null ? "" : nick;
        this.wasBanned = wasBanned;
        this.defaultBanReason = defaultBanReason == null ? "2.4" : defaultBanReason;
        this.selectedResult = wasBanned ? "ban" : "clean";
    }

    @Override
    protected void init() {
        int defX = (this.width - WIN_W) / 2;
        int defY = Math.max(4, (this.height - WIN_H) / 2);
        this.winX = ModConfig.INSTANCE.checkWindowX != -1 ? ModConfig.INSTANCE.checkWindowX : defX;
        this.winY = ModConfig.INSTANCE.checkWindowY != -1 ? ModConfig.INSTANCE.checkWindowY : defY;
        clampWindow();

        int btnW = 128;
        int btnH = 22;
        int startY = winY + 70;
        for (int i = 0; i < RESULTS.length; i++) {
            final String result = RESULTS[i][0];
            final String label = RESULTS[i][1];
            int col = i % 2;
            int row = i / 2;
            this.addRenderableWidget(Button.builder(
                    Component.literal(label),
                    b -> {
                        selectedResult = result;
                        refreshResultLabels();
                    }
            ).bounds(winX + 10 + col * (btnW + 6), startY + row * (btnH + 4), btnW, btnH).build());
        }

        this.stashCheckbox = Checkbox.builder(Component.literal("Снос стеша"), this.font)
                .pos(winX + 10, winY + 158)
                .selected(true)
                .build();
        this.addRenderableWidget(this.stashCheckbox);

        this.banReasonBox = new EditBox(this.font, winX + 10, winY + 184, WIN_W - 20, 20, Component.literal("Пункт бана"));
        this.banReasonBox.setMaxLength(16);
        this.banReasonBox.setValue(defaultBanReason);
        this.addRenderableWidget(this.banReasonBox);

        this.addRenderableWidget(Button.builder(
                Component.literal("Завершить проверку"),
                b -> submit()
        ).bounds(winX + 10, winY + WIN_H - 28, WIN_W - 20, 22).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Закрыть"),
                b -> this.onClose()
        ).bounds(winX + WIN_W - 93, winY + 8, 83, 20).build());
    }

    private void refreshResultLabels() {
        var widgets = this.children().stream()
                .filter(w -> w instanceof Button)
                .map(w -> (Button) w)
                .toList();
        int resultBtnStart = widgets.size() - RESULTS.length - 2;
        for (int i = 0; i < RESULTS.length; i++) {
            int idx = resultBtnStart + i;
            if (idx >= 0 && idx < widgets.size()) {
                Button btn = widgets.get(idx);
                String label = RESULTS[i][1];
                btn.setMessage(Component.literal(
                        selectedResult != null && selectedResult.equals(RESULTS[i][0]) ? "► " + label : label));
            }
        }
    }

    private boolean needsBanReason() {
        return "ban".equals(selectedResult) || "autobuy".equals(selectedResult) || "autosell".equals(selectedResult);
    }

    private void submit() {
        String result = selectedResult != null ? selectedResult : (wasBanned ? "ban" : "clean");
        boolean destroyStash = this.stashCheckbox.selected();
        String banReason = needsBanReason() ? this.banReasonBox.getValue().trim() : null;
        if (banReason != null && banReason.isEmpty()) {
            banReason = "2.4";
        }

        boolean online = "online".equals(ModConfig.INSTANCE.checkLogMode);
        final String banReasonFinal = banReason;
        if (online) {
            JournalAPI.getInstance().endCheckout(result, destroyStash, banReasonFinal)
                    .thenAccept(success -> {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            if (success) {
                                mc.player.displayClientMessage(Component.literal(
                                        "§8[§6VaguriAssist§8] §aПроверка завершена в журнале: §e" + nick
                                                + " §f(§e" + result + "§f)"), false);
                            } else {
                                mc.player.displayClientMessage(Component.literal(
                                        "§8[§6VaguriAssist§8] §cНе удалось завершить проверку — "
                                                + (needsBanReason() ? "причина: " + (banReasonFinal == null ? "-" : banReasonFinal) : "ошибка API")), false);
                            }
                        }
                    });
        } else {
            CheckLogger.logEnd(nick, result, destroyStash, banReasonFinal);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal(
                        "§8[§6VaguriAssist§8] §aПроверка завершена (оффлайн): §e" + nick
                                + " §f(§e" + result + "§f)"), false);
            }
        }
        VaguriAssistClient.clearCurrentNick();
        this.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mX, int mY, float p) {
        g.fill(winX, winY, winX + WIN_W, winY + WIN_H, 0xE0000000);

        g.fill(winX, winY, winX + WIN_W, winY + 1, 0xFF888888);
        g.fill(winX, winY + WIN_H - 1, winX + WIN_W, winY + WIN_H, 0xFF888888);
        g.fill(winX, winY, winX + 1, winY + WIN_H, 0xFF888888);
        g.fill(winX + WIN_W - 1, winY, winX + WIN_W, winY + WIN_H, 0xFF888888);

        g.fill(winX + 1, winY + 1, winX + WIN_W - 1, winY + 14, 0xFFFFAA00);
        g.drawCenteredString(this.font, "Завершение проверки", winX + WIN_W / 2, winY + 4, 0xFFFFFFFF);

        String nickDisplay = nick.isEmpty() ? "Нет игрока" : nick;
        g.drawCenteredString(this.font, Component.literal("Игрок: ").append(
                Component.literal(nickDisplay).withStyle(ChatFormatting.YELLOW)), winX + WIN_W / 2, winY + 24, 0xA0A0A0);
        g.drawCenteredString(this.font, "Результат проверки:", winX + WIN_W / 2, winY + 58, 0x55FF55);

        String logMode = "online".equals(ModConfig.INSTANCE.checkLogMode) ? "Онлайн (журнал)" : "Оффлайн (txt)";
        g.drawCenteredString(this.font, "Режим вноса: " + logMode, winX + WIN_W / 2, winY + WIN_H - 50, 0xA0A0A0);

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
        var widgets = this.children().stream().filter(w -> w instanceof Button).map(w -> (Button) w).toList();
        int btnW = 128;
        int btnH = 22;
        int startY = winY + 70;
        int resultBtnStart = widgets.size() - RESULTS.length - 2;
        for (int i = 0; i < RESULTS.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int idx = resultBtnStart + i;
            if (idx >= 0 && idx < widgets.size()) {
                widgets.get(idx).setPosition(winX + 10 + col * (btnW + 6), startY + row * (btnH + 4));
            }
        }
        this.stashCheckbox.setPosition(winX + 10, winY + 158);
        this.banReasonBox.setPosition(winX + 10, winY + 184);
        int submitIdx = widgets.size() - 2;
        int closeIdx = widgets.size() - 1;
        widgets.get(submitIdx).setPosition(winX + 10, winY + WIN_H - 28);
        widgets.get(closeIdx).setPosition(winX + WIN_W - 93, winY + 8);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
