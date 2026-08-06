package com.vaguriassist;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ManualBanScreen extends Screen {

	private EditBox nickBox;
	private EditBox reasonBox;
	private EditBox durationBox;
	private EditBox paragraphBox;
	private Checkbox ipCheckbox;

	public ManualBanScreen() {
		super(Component.literal("VaguriAssist — Бан без проверки"));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int left = cx - 125;

		this.nickBox = new EditBox(this.font, left, 40, 250, 20, Component.literal("Ник"));
		this.nickBox.setMaxLength(32);

		this.reasonBox = new EditBox(this.font, left, 80, 250, 20, Component.literal("Причина"));
		this.reasonBox.setMaxLength(64);
		this.reasonBox.setHint(Component.literal("например: Чит"));

		this.durationBox = new EditBox(this.font, left, 120, 122, 20, Component.literal("Срок (дней)"));
		this.durationBox.setMaxLength(8);
		this.durationBox.setValue("30");

		this.paragraphBox = new EditBox(this.font, cx + 3, 120, 122, 20, Component.literal("Пункт"));
		this.paragraphBox.setMaxLength(16);
		this.paragraphBox.setValue("2.4");

		this.addRenderableWidget(this.nickBox);
		this.addRenderableWidget(this.reasonBox);
		this.addRenderableWidget(this.durationBox);
		this.addRenderableWidget(this.paragraphBox);

		this.ipCheckbox = Checkbox.builder(Component.literal("Бан по IP (/banip)"), this.font)
				.pos(left, 146)
				.selected(true)
				.build();
		this.addRenderableWidget(this.ipCheckbox);

		this.addRenderableWidget(Button.builder(Component.literal("ВЫДАТЬ БАН"), b -> sendBan())
				.bounds(left, 180, 250, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> this.onClose())
				.bounds(this.width - 100, 8, 88, 20).build());
	}

	private void sendBan() {
		String nick = this.nickBox.getValue().trim();
		if (nick.isEmpty()) {
			this.nickBox.setFocused(true);
			return;
		}
		String reason = this.reasonBox.getValue().trim();
		if (reason.isEmpty()) {
			reason = "Без причины";
		}
		int days = parseDays(this.durationBox.getValue(), 30);
		String paragraph = this.paragraphBox.getValue().trim();
		if (paragraph.isEmpty()) {
			paragraph = "2.4";
		}
		BanSender.send(nick, days, paragraph, reason, this.ipCheckbox.selected());
		this.onClose();
	}

	private static int parseDays(String value, int defaultDays) {
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return defaultDays;
		}
		try {
			int days = Integer.parseInt(trimmed);
			return days > 0 ? days : defaultDays;
		} catch (NumberFormatException e) {
			return defaultDays;
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);

		int cx = this.width / 2;
		int left = cx - 125;

		guiGraphics.drawCenteredString(this.font, this.title, cx, 12, 0xFFFFFF);
		guiGraphics.drawString(this.font, "Ник", left, 28, 0xA0A0A0);
		guiGraphics.drawString(this.font, "Причина", left, 68, 0xA0A0A0);
		guiGraphics.drawString(this.font, "Срок (дней)", left, 108, 0xA0A0A0);
		guiGraphics.drawString(this.font, "Пункт", cx + 3, 108, 0xA0A0A0);
	}
}
