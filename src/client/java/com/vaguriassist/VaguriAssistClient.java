package com.vaguriassist;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.regex.Pattern;

public class VaguriAssistClient implements ClientModInitializer {

	private static final Pattern CLEAN_COLOR_CODES = Pattern.compile("§[0-9a-fk-orA-FK-OR]");

	private static KeyMapping banKey;
	private static KeyMapping settingsKey;
	private static String currentNick = "";
	private static boolean sendingCommand = false;
	private static String pendingBanCommand = "";
	private static long pendingBanTime = 0;
	private static Runnable pendingScreen = null;

	public static final long WARN_AT_MS = 4 * 60_000L + 30_000L;
	public static final long FIVE_MIN_MS = 5 * 60_000L;
	private static long checkStartTime = 0;
	private static boolean warningPlayed = false;

	public static String getCurrentNick() {
		return currentNick;
	}

	public static void setCurrentNick(String nick) {
		currentNick = nick == null ? "" : nick;
		if (currentNick.isEmpty()) {
			checkStartTime = 0;
		} else {
			checkStartTime = System.currentTimeMillis();
		}
		warningPlayed = false;
	}

	public static long getCheckElapsedMs() {
		if (checkStartTime == 0) {
			return 0;
		}
		return System.currentTimeMillis() - checkStartTime;
	}

	public static void clearCurrentNick() {
		currentNick = "";
	}

	public static void sendCommand(String command) {
		sendingCommand = true;
		ChatScanner.setModSendingCommand(true);
		try {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.getConnection() != null) {
				minecraft.getConnection().sendCommand(command.substring(1));
			}
			if (minecraft.player != null) {
				minecraft.player.displayClientMessage(
						Component.literal("§8[§6VaguriAssist§8] §fОтправлено: §7" + command), false);
			}
		} finally {
			sendingCommand = false;
			ChatScanner.setModSendingCommand(false);
		}
		clearCurrentNick();
	}

	public static void sendRaw(String command) {
		sendingCommand = true;
		ChatScanner.setModSendingCommand(true);
		try {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.getConnection() != null) {
				minecraft.getConnection().sendCommand(command.substring(1));
			}
		} finally {
			sendingCommand = false;
			ChatScanner.setModSendingCommand(false);
		}
	}

	public static void scheduleBan(String command, long delayMs) {
		pendingBanCommand = command;
		pendingBanTime = System.currentTimeMillis() + delayMs;
	}

	@Override
	public void onInitializeClient() {
		ModConfig.load();
		ChatScanner.init();

		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath("vaguriassist", "category"));
		banKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.vaguriassist.ban", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, category
		));
		settingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.vaguriassist.settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!pendingBanCommand.isEmpty() && System.currentTimeMillis() >= pendingBanTime) {
				String command = pendingBanCommand;
				pendingBanCommand = "";
				sendCommand(command);
			}
			if (pendingScreen != null) {
				pendingScreen.run();
				pendingScreen = null;
			}
			if (checkStartTime != 0) {
				long elapsed = System.currentTimeMillis() - checkStartTime;
				if (!warningPlayed && elapsed >= WARN_AT_MS) {
					warningPlayed = true;
					if (client.player != null) {
						client.player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
					}
				}
			}
			while (banKey.consumeClick()) {
				if (currentNick.isEmpty()) {
					if (client.player != null) {
						client.player.displayClientMessage(Component.literal(
								"§8[§6VaguriAssist§8] §fНет игрока на проверке — используй §e/freezing <ник>"), false);
					}
				} else {
					Minecraft.getInstance().setScreen(new BanScreen(currentNick));
				}
			}
			while (settingsKey.consumeClick()) {
				Minecraft.getInstance().setScreen(new SettingsScreen());
			}
		});

		ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
			if (sendingCommand) {
				return true;
			}
			String clean = CLEAN_COLOR_CODES.matcher(command).replaceAll("");
			if (clean.startsWith("freezing ") || clean.startsWith("frz ")
					|| clean.startsWith("hm freezing ") || clean.startsWith("hm frz ")) {
				String[] parts = clean.split(" ");
				String nick = parts[parts.length - 1].trim();
				if (!nick.isEmpty()) {
					setCurrentNick(nick);
				}
			}
			return true;
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("vaguriassist")
					.then(ClientCommandManager.literal("setvk")
							.then(ClientCommandManager.argument("link", StringArgumentType.greedyString())
									.executes(ctx -> {
										String link = StringArgumentType.getString(ctx, "link").trim();
										ModConfig config = ModConfig.get();
										config.vk = link;
										config.save();
										ctx.getSource().sendFeedback(Component.literal("VK ссылка сохранена: " + link));
										return 1;
									})))
					.then(ClientCommandManager.literal("vk")
							.executes(ctx -> {
								ctx.getSource().sendFeedback(Component.literal("Текущая VK ссылка: " + ModConfig.get().vk));
								return 1;
							})));
			dispatcher.register(ClientCommandManager.literal("va")
					.then(ClientCommandManager.literal("ban")
							.executes(ctx -> {
								pendingScreen = () -> Minecraft.getInstance().setScreen(new ManualBanScreen());
								return 1;
							})));
		});

		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath("vaguriassist", "hud"),
				VaguriAssistClient::renderHud
		);
	}

	private static void renderHud(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		VaguriHUD.render(guiGraphics);
	}
}
