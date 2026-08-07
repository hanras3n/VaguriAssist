package com.vaguriassist;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class BanScreen extends Screen {

	private final String nick;

	private EditBox reasonBox;
	private EditBox durationBox;
	private EditBox paragraphBox;
	private Checkbox ipCheckbox;

	public BanScreen(String nick) {
		super(Component.literal("VaguriAssist — Выдача бана"));
		this.nick = nick;
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int left = cx - 125;

		this.addRenderableWidget(Button.builder(Component.literal("Неадекват"), b -> autoBan("Неадекват", 30))
				.bounds(left, 50, 122, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Признание"), b -> autoBan("Признание", 20))
				.bounds(cx + 3, 50, 122, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Отказ"), b -> autoBan("Отказ", 30))
				.bounds(left, 75, 122, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Время вышло"), b -> autoBan("Время вышло", 30))
				.bounds(cx + 3, 75, 122, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Лив с проверки"), b -> autoBan("Лив с проверки", 30))
				.bounds(left, 100, 122, 20).build());

		this.reasonBox = new EditBox(this.font, left, 137, 250, 20, Component.literal("Своя причина"));
		this.reasonBox.setMaxLength(64);
		this.reasonBox.setHint(Component.literal("например: Игнор"));

		this.durationBox = new EditBox(this.font, left, 173, 122, 20, Component.literal("Срок (дней)"));
		this.durationBox.setMaxLength(8);
		this.durationBox.setValue("30");

		this.paragraphBox = new EditBox(this.font, cx + 3, 173, 122, 20, Component.literal("Пункт"));
		this.paragraphBox.setMaxLength(16);
		this.paragraphBox.setValue("2.4");

		this.addRenderableWidget(this.reasonBox);
		this.addRenderableWidget(this.durationBox);
		this.addRenderableWidget(this.paragraphBox);

		this.ipCheckbox = Checkbox.builder(Component.literal("Бан по IP (/banip)"), this.font)
				.pos(left, 199)
				.selected(true)
				.build();
		this.addRenderableWidget(this.ipCheckbox);

		this.addRenderableWidget(Button.builder(Component.literal("ВЫДАТЬ БАН"), b -> sendCustomBan())
				.bounds(left, 225, 250, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Отпустить"), b -> release())
				.bounds(left, 246, 250, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> this.onClose())
				.bounds(this.width - 100, 8, 88, 20).build());
	}

	private void autoBan(String reason, int defaultDays) {
		BanSender.sendWithUnfreeze(this.nick, defaultDays, "2.4", reason, true);
		CheckLogger.logBan(this.nick, reason + ", " + defaultDays + "д, пункт 2.4");
		this.onClose();
	}

	private void sendCustomBan() {
		String reason = this.reasonBox.getValue().trim();
		if (reason.isEmpty()) {
			this.reasonBox.setFocused(true);
			return;
		}
		int days = parseDays(this.durationBox.getValue(), 30);
		String paragraph = this.paragraphBox.getValue().trim();
		if (paragraph.isEmpty()) {
			paragraph = "2.4";
		}
		BanSender.sendWithUnfreeze(this.nick, days, paragraph, reason, this.ipCheckbox.selected());
		CheckLogger.logBan(this.nick, reason + ", " + days + "д, пункт " + paragraph);
		this.onClose();
	}

	private void release() {
		BanSender.sendFreezing(this.nick);
		CheckLogger.logRelease(this.nick);
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
		guiGraphics.drawCenteredString(this.font,
				Component.literal("На проверке ").append(Component.literal(this.nick).withStyle(ChatFormatting.YELLOW)),
				cx, 26, 0xA0A0A0);

		guiGraphics.drawString(this.font, "Причина:", left, 38, 0xA0A0A0);
		guiGraphics.drawString(this.font, "Своя причина", left, 127, 0xA0A0A0);
		guiGraphics.drawString(this.font, "Срок (дней)", left, 163, 0xA0A0A0);
		guiGraphics.drawString(this.font, "Пункт", cx + 3, 163, 0xA0A0A0);
	}
}
