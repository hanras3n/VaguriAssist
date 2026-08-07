package com.vaguriassist;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class CheckScreen extends Screen {

	private static final String[] MODES = {"Репорт", "Проверка", "Кандидат", "Автобай", "Автоселл"};
	private static final int WINDOW_W = 380;
	private static final int WINDOW_H = 200;

	private final Button[] modeButtons = new Button[MODES.length];
	private String selectedMode;
	private EditBox reasonBox;
	private Button saveBtn;
	private Button closeBtn;

	private int winX;
	private int winY;
	private boolean dragging;
	private int dragDX;
	private int dragDY;

	public CheckScreen() {
		super(Component.literal("VaguriAssist — Внести проверку"));
		this.selectedMode = VaguriAssistClient.getCurrentMode();
	}

	@Override
	protected void init() {
		int defX = (this.width - WINDOW_W) / 2;
		int defY = Math.max(4, (this.height - WINDOW_H) / 2);
		this.winX = ModConfig.INSTANCE.checkWindowX != -1 ? ModConfig.INSTANCE.checkWindowX : defX;
		this.winY = ModConfig.INSTANCE.checkWindowY != -1 ? ModConfig.INSTANCE.checkWindowY : defY;
		clampWindow();

		for (int i = 0; i < MODES.length; i++) {
			final String mode = MODES[i];
			this.modeButtons[i] = Button.builder(Component.literal(mode), b -> {
						selectedMode = mode;
						updateModeButtons();
					})
					.bounds(winX + (i % 3) * 128, winY + 40 + (i / 3) * 25, 124, 20).build();
			this.addRenderableWidget(this.modeButtons[i]);
		}
		updateModeButtons();

		this.reasonBox = new EditBox(this.font, winX + 5, winY + 120, WINDOW_W - 10, 20, Component.literal("Причина"));
		this.reasonBox.setMaxLength(64);
		this.reasonBox.setHint(Component.literal("например: Игнор"));
		this.addRenderableWidget(this.reasonBox);

		this.saveBtn = Button.builder(Component.literal("Сохранить"), b -> saveCheck())
				.bounds(winX + 5, winY + 150, WINDOW_W - 10, 20).build();
		this.addRenderableWidget(this.saveBtn);

		this.closeBtn = Button.builder(Component.literal("Закрыть"), b -> this.onClose())
				.bounds(winX + WINDOW_W - 88, winY + 8, 88, 20).build();
		this.addRenderableWidget(this.closeBtn);
	}

	private void updateModeButtons() {
		for (int i = 0; i < MODES.length; i++) {
			String label = MODES[i].equals(selectedMode) ? "> " + MODES[i] : "  " + MODES[i];
			this.modeButtons[i].setMessage(Component.literal(label));
		}
	}

	private void saveCheck() {
		VaguriAssistClient.setCurrentMode(selectedMode);
		this.onClose();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
		int mx = (int) event.x();
		int my = (int) event.y();
		boolean inTitleBar = mx >= winX && mx < winX + WINDOW_W && my >= winY && my < winY + 22;
		boolean onClose = mx >= winX + WINDOW_W - 88 && mx < winX + WINDOW_W && my >= winY + 8 && my < winY + 28;
		if (inTitleBar && !onClose) {
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
		winX = Math.max(0, Math.min(this.width - WINDOW_W, winX));
		winY = Math.max(0, Math.min(this.height - WINDOW_H, winY));
	}

	private void repositionWidgets() {
		for (int i = 0; i < MODES.length; i++) {
			this.modeButtons[i].setPosition(winX + (i % 3) * 128, winY + 40 + (i / 3) * 25);
		}
		this.reasonBox.setPosition(winX + 5, winY + 120);
		this.saveBtn.setPosition(winX + 5, winY + 150);
		this.closeBtn.setPosition(winX + WINDOW_W - 88, winY + 8);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.fill(winX, winY, winX + WINDOW_W, winY + WINDOW_H, 0xCC000000);
		guiGraphics.fill(winX, winY, winX + WINDOW_W, winY + 1, 0xFF888888);
		guiGraphics.fill(winX, winY + WINDOW_H - 1, winX + WINDOW_W, winY + WINDOW_H, 0xFF888888);
		guiGraphics.fill(winX, winY, winX + 1, winY + WINDOW_H, 0xFF888888);
		guiGraphics.fill(winX + WINDOW_W - 1, winY, winX + WINDOW_W, winY + WINDOW_H, 0xFF888888);

		super.render(guiGraphics, mouseX, mouseY, partialTick);

		guiGraphics.drawCenteredString(this.font, this.title, winX + WINDOW_W / 2, winY + 14, 0xFFFFFF);

		String nick = VaguriAssistClient.getCurrentNick();
		guiGraphics.drawCenteredString(this.font,
				nick.isEmpty() ? Component.literal("Нет игрока на проверке")
						: Component.literal("Игрок: ").append(Component.literal(nick)),
				winX + WINDOW_W / 2, winY + 28, 0xA0A0A0);

		guiGraphics.drawString(this.font, "Режим:", winX + 5, winY + 32, 0xA0A0A0);
		guiGraphics.drawString(this.font, "Причина", winX + 5, winY + 112, 0xA0A0A0);
		guiGraphics.drawCenteredString(this.font,
				"Выбран режим: " + selectedMode, winX + WINDOW_W / 2, winY + 178, 0x55FF55);
		guiGraphics.drawCenteredString(this.font,
				"Перетащи за заголовок", winX + WINDOW_W / 2, winY + WINDOW_H - 10, 0x555555);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
