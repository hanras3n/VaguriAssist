package com.vaguriassist;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
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
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VaguriAssistClient implements ClientModInitializer {

	private static final Pattern CLEAN_COLOR_CODES = Pattern.compile("§[0-9a-fk-orA-FK-OR]");
	private static final Pattern MODE_NUMBER_PATTERN = Pattern.compile("#\\s*(\\d+)");

	private static KeyMapping banKey;
	private static KeyMapping settingsKey;
	private static String currentNick = "";
	private static String currentMode = "Проверка";
	private static boolean sendingCommand = false;
	private static String pendingBanCommand = "";
	private static long pendingBanTime = 0;
	private static String pendingRawCommand = "";
	private static long pendingRawTime = 0;
	private static Runnable pendingScreen = null;

	public static final long WARN_AT_MS = 4 * 60_000L + 30_000L;
	public static final long FIVE_MIN_MS = 5 * 60_000L;
	private static long checkStartTime = 0;
	private static boolean warningPlayed = false;

	public static final long TIMER_ANIM_MS = 800;
	private static long timerAnimStartTime = 0;
	private static int timerAnimFromX = -1;
	private static boolean timerAnimating = false;

	private static String detectedMode = null;
	private static String detectedServer = "";
	private static String checkedPlayerName = null;
	private static String tabHeader = "";
	private static String tabFooter = "";

	private static boolean draggingTimer = false;
	private static int timerDragOffsetX = 0;
	private static int timerDragOffsetY = 0;

	public static String getDetectedMode() {
		return detectedMode;
	}

	public static String getDetectedServer() {
		return detectedServer;
	}

	public static void onTabListUpdate(net.minecraft.network.chat.Component header, net.minecraft.network.chat.Component footer) {
		tabHeader = header != null ? header.getString() : "";
		tabFooter = footer != null ? footer.getString() : "";
	}

	public static void updateScoreboardMode() {
		detectedMode = null;
		detectedServer = "";
		scanTabForMode(tabHeader);
		scanTabForMode(tabFooter);
		scanTabForServer(tabHeader);
		if (detectedServer.isEmpty()) {
			scanTabForFirstNumber(tabHeader);
		}
	}

	private static void scanTabForMode(String text) {
		if (text == null || text.isEmpty()) {
			return;
		}
		for (String line : text.split("\n")) {
			if (line == null || line.isEmpty()) {
				continue;
			}
			String clean = CLEAN_COLOR_CODES.matcher(line).replaceAll("").toLowerCase();
			if (clean.contains("lite120") || clean.contains("lite 1.20") || clean.contains("1.20")) {
				detectedMode = "lite120";
			} else if (clean.contains("lite")) {
				detectedMode = "lite";
			} else if (clean.contains("classic")) {
				detectedMode = "classic";
			}
		}
	}

	private static void scanTabForServer(String text) {
		if (text == null || text.isEmpty()) {
			return;
		}
		for (String line : text.split("\n")) {
			if (line == null || line.isEmpty()) {
				continue;
			}
			String clean = CLEAN_COLOR_CODES.matcher(line).replaceAll("").toLowerCase();
			boolean hasMode = clean.contains("lite") || clean.contains("classic") || clean.contains("1.20");
			if (!hasMode) {
				continue;
			}
			String num = lastNumber(line);
			if (num != null) {
				detectedServer = num;
				return;
			}
		}
	}

	private static void scanTabForFirstNumber(String text) {
		if (text == null || text.isEmpty()) {
			return;
		}
		for (String line : text.split("\n")) {
			String num = lastNumber(line);
			if (num != null) {
				detectedServer = num;
				return;
			}
		}
	}

	private static String lastNumber(String raw) {
		if (raw == null) {
			return null;
		}
		Matcher matcher = MODE_NUMBER_PATTERN.matcher(raw);
		String num = null;
		while (matcher.find()) {
			num = matcher.group(1);
		}
		return num;
	}

	private static String cleanModeName(String before) {
		int start = 0;
		int end = before.length();
		while (start < end && !isNameChar(before.charAt(start))) {
			start++;
		}
		while (end > start && !isNameChar(before.charAt(end - 1))) {
			end--;
		}
		String name = before.substring(start, end);
		return name.replaceAll("\\s+", " ").trim();
	}

	private static boolean isNameChar(char c) {
		return Character.isLetterOrDigit(c) || c == '(' || c == ')' || c == '.' || c == ' ';
	}

	public static String getCurrentNick() {
		return currentNick;
	}

	public static String getCurrentMode() {
		return currentMode;
	}

	public static void setCurrentMode(String mode) {
		currentMode = mode;
	}

	public static boolean isTimerAnimating() {
		return timerAnimating;
	}

	public static int getTimerAnimFromX() {
		return timerAnimFromX;
	}

	public static float getTimerAnimProgress() {
		if (!timerAnimating) {
			return 1.0f;
		}
		float progress = (System.currentTimeMillis() - timerAnimStartTime) / (float) TIMER_ANIM_MS;
		if (progress >= 1.0f) {
			ModConfig.INSTANCE.timerX = -1;
			ModConfig.save();
			finishTimerAnim();
			return 1.0f;
		}
		return progress;
	}

	public static void finishTimerAnim() {
		timerAnimating = false;
	}

	public static void setCurrentNick(String nick) {
		currentNick = nick == null ? "" : nick;
		if (currentNick.isEmpty()) {
			checkStartTime = 0;
		} else {
			checkStartTime = System.currentTimeMillis();
			timerAnimFromX = ModConfig.INSTANCE.timerX;
			timerAnimStartTime = System.currentTimeMillis();
			timerAnimating = true;
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
		timerAnimating = false;
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

	public static void submitCheck() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		String nick = getCurrentNick();
		String msg = "§8[§6VaguriAssist§8] §aПроверка внесена"
				+ (nick.isEmpty() ? "" : ": §e" + nick)
				+ " §f(режим: §e" + currentMode + "§f)";
		mc.player.displayClientMessage(Component.literal(msg), false);
	}

	public static void scheduleBan(String command, long delayMs) {
		pendingBanCommand = command;
		pendingBanTime = System.currentTimeMillis() + delayMs;
	}

	public static void scheduleRaw(String command, long delayMs) {
		pendingRawCommand = command;
		pendingRawTime = System.currentTimeMillis() + delayMs;
	}

    public static void playCheckCall(String nick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.4f);
        pendingScreen = () -> mc.setScreen(new CheckScreen(nick));
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
			updateScoreboardMode();
			String playerName = client.player != null ? client.player.getGameProfile().name() : null;
			if (playerName != null && !playerName.equals(checkedPlayerName)) {
				checkedPlayerName = playerName;
				String allowed = ModConfig.INSTANCE.allowedNick;
				if (!allowed.isEmpty() && !allowed.equalsIgnoreCase(playerName)) {
					client.disconnectFromWorld(Component.literal(
							"§cVaguriAssist: доступ только для ника §e" + allowed + "§c. Игра остановлена."));
				}
			}
			if (!pendingBanCommand.isEmpty() && System.currentTimeMillis() >= pendingBanTime) {
				String command = pendingBanCommand;
				pendingBanCommand = "";
				sendCommand(command);
			}
			if (!pendingRawCommand.isEmpty() && System.currentTimeMillis() >= pendingRawTime) {
				String command = pendingRawCommand;
				pendingRawCommand = "";
				sendRaw(command);
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
			if (ModConfig.INSTANCE.draggableTimer && !currentNick.isEmpty() && client.screen == null) {
				long handle = client.getWindow().handle();
				double[] mxArr = new double[1];
				double[] myArr = new double[1];
				GLFW.glfwGetCursorPos(handle, mxArr, myArr);
				float guiScale = (float) client.getWindow().getGuiScale();
				int mouseX = (int) (mxArr[0] / guiScale);
				int mouseY = (int) (myArr[0] / guiScale);
				boolean mouseDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
				int tw = VaguriHUD.getCachedTimerW();
				int th = VaguriHUD.getCachedTimerH();
				if (mouseDown && !draggingTimer && tw > 0) {
					int tx = VaguriHUD.getCachedTimerX();
					int ty = VaguriHUD.getCachedTimerY();
					if (mouseX >= tx && mouseX <= tx + tw && mouseY >= ty && mouseY <= ty + th) {
						draggingTimer = true;
						timerDragOffsetX = mouseX - tx;
						timerDragOffsetY = mouseY - ty;
					}
				}
				if (draggingTimer) {
					if (mouseDown) {
						ModConfig.INSTANCE.timerX = mouseX - timerDragOffsetX;
						ModConfig.INSTANCE.timerY = mouseY - timerDragOffsetY;
						ModConfig.INSTANCE.timerX = Math.max(0, Math.min(client.getWindow().getGuiScaledWidth() - tw, ModConfig.INSTANCE.timerX));
						ModConfig.INSTANCE.timerY = Math.max(0, Math.min(client.getWindow().getGuiScaledHeight() - th, ModConfig.INSTANCE.timerY));
					} else {
						draggingTimer = false;
						ModConfig.save();
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
			if (clean.startsWith("freezing ") || clean.startsWith("frz ")) {
				String[] parts = clean.split(" ");
				String nick = parts[parts.length - 1].trim();
				if (!nick.isEmpty()) {
					setCurrentNick(nick);
					playCheckCall(nick);
					if (ModConfig.INSTANCE.autoWarpLogo) {
						scheduleRaw("/warp logo", 20);
					}
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
							}))
					.then(ClientCommandManager.literal("ban")
							.executes(ctx -> {
								pendingScreen = () -> Minecraft.getInstance().setScreen(new ManualBanScreen());
								return 1;
							}))
				.then(ClientCommandManager.literal("check")
						.executes(ctx -> {
							pendingScreen = () -> Minecraft.getInstance().setScreen(new CheckScreen());
							return 1;
						}))
				.then(ClientCommandManager.literal("addcheck")
						.executes(ctx -> {
							pendingScreen = () -> Minecraft.getInstance().setScreen(new ManualCheckScreen());
							return 1;
						}))
				.then(ClientCommandManager.literal("end")
						.executes(ctx -> {
							pendingScreen = () -> Minecraft.getInstance().setScreen(
									new CheckEndScreen(VaguriAssistClient.getCurrentNick(), true, "2.4"));
							return 1;
						}))
				.then(ClientCommandManager.literal("apicheck")
						.executes(ctx -> {
							if (ModConfig.INSTANCE.apiToken.isEmpty()) {
								ctx.getSource().sendFeedback(Component.literal("§cAPI токен не задан! /vaguriassist setapi <token>"));
								return 1;
							}
							ctx.getSource().sendFeedback(Component.literal("§7Проверка доступности API..."));
							JournalAPI.getInstance().checkApiStatus().thenAccept(ok -> {
								if (ok) {
									ctx.getSource().sendFeedback(Component.literal("§aAPI работает! (статус OK)"));
								} else {
									ctx.getSource().sendFeedback(Component.literal("§cAPI недоступно — ошибка запроса или нет сети"));
								}
							});
							return 1;
						}))
			.then(ClientCommandManager.literal("guisetting")
						.executes(ctx -> {
							pendingScreen = () -> Minecraft.getInstance().setScreen(new GuiEditScreen());
							return 1;
						}))
				.then(ClientCommandManager.literal("setapi")
						.then(ClientCommandManager.argument("token", StringArgumentType.greedyString())
								.executes(ctx -> {
									String token = StringArgumentType.getString(ctx, "token").trim();
									ModConfig.INSTANCE.apiToken = token;
									ModConfig.save();
									if (token.isEmpty()) {
										ctx.getSource().sendFeedback(Component.literal("§cAPI токен очищен."));
									} else {
										String masked = token.substring(0, Math.min(8, token.length())) + "...";
										ctx.getSource().sendFeedback(Component.literal("§aAPI токен сохранён: " + masked));
									}
									return 1;
								})))
				.then(ClientCommandManager.literal("api")
						.executes(ctx -> {
							if (ModConfig.INSTANCE.apiToken.isEmpty()) {
								ctx.getSource().sendFeedback(Component.literal("§cAPI токен не задан. Используй /vaguriassist setapi <token>"));
							} else {
								String masked = ModConfig.INSTANCE.apiToken.substring(0, Math.min(8, ModConfig.INSTANCE.apiToken.length())) + "...";
								ctx.getSource().sendFeedback(Component.literal("§7Текущий токен: " + masked));
							}
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
