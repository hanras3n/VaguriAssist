package com.vaguriassist;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CheckScreen extends Screen {

	private static final String[] MODES = {"Репорт", "Проверка", "Кандидат", "Автобай", "Автоселл"};

	private final Button[] modeButtons = new Button[MODES.length];
	private String selectedMode;
	private EditBox reasonBox;

	public CheckScreen() {
		super(Component.literal("VaguriAssist — Внести проверку"));
		this.selectedMode = VaguriAssistClient.getCurrentMode();
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int left = cx - 125;

		for (int i = 0; i < MODES.length; i++) {
			final String mode = MODES[i];
			this.modeButtons[i] = Button.builder(Component.literal(mode), b -> {
						selectedMode = mode;
						updateModeButtons();
					})
					.bounds(left + (i % 3) * 128, 50 + (i / 3) * 25, 124, 20).build();
			this.addRenderableWidget(this.modeButtons[i]);
		}
		updateModeButtons();

		this.reasonBox = new EditBox(this.font, left, 130, 250, 20, Component.literal("Причина"));
		this.reasonBox.setMaxLength(64);
		this.reasonBox.setHint(Component.literal("например: Игнор"));
		this.addRenderableWidget(this.reasonBox);

		this.addRenderableWidget(Button.builder(Component.literal("Сохранить"), b -> saveCheck())
				.bounds(left, 160, 250, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> this.onClose())
				.bounds(this.width - 100, 8, 88, 20).build());
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
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);

		int cx = this.width / 2;
		int left = cx - 125;

		guiGraphics.drawCenteredString(this.font, this.title, cx, 12, 0xFFFFFF);
		String nick = VaguriAssistClient.getCurrentNick();
		guiGraphics.drawCenteredString(this.font,
				nick.isEmpty() ? Component.literal("Нет игрока на проверке")
						: Component.literal("Игрок: ").append(Component.literal(nick)),
				cx, 26, 0xA0A0A0);

		guiGraphics.drawString(this.font, "Режим:", left, 40, 0xA0A0A0);
		guiGraphics.drawString(this.font, "Причина", left, 120, 0xA0A0A0);
		guiGraphics.drawCenteredString(this.font,
				"Выбран режим: " + selectedMode, cx, 187, 0x55FF55);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
