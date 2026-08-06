package com.vaguriassist;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatScanner {
    private enum State { IDLE, AWAITING_INITIAL_DUPE, AWAITING_REVERSE_DUPE, AWAITING_BAN_CHECK }

    private static State currentState = State.IDLE;
    private static String originalTarget = "";
    private static String mainTarget = "";
    private static String bannedTwink = "";
    private static int timeoutTicks = 0;
    private static Runnable scheduledAction = null;
    private static boolean isModSendingCommand = false;

    public static void setModSendingCommand(boolean value) {
        isModSendingCommand = value;
    }

    private static final Queue<String> pendingNicks = new LinkedList<>();
    private static final Set<String> processedNicks = new HashSet<>();
    private static int bulkDelayTicks = 0;
    private static final int BULK_DELAY = 3;
    private static final int TIMEOUT_TICKS = 200;

    private static final Pattern NICK_PATTERN = Pattern.compile("([a-zA-Z0-9_]+)");
    private static final Pattern BAN_TIME_PATTERN = Pattern.compile("(\\d+)\\s*д[\\s\\D]*?(\\d+)\\s*ч", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern CLEAN_COLOR_CODES = Pattern.compile("§[0-9a-fk-orA-FK-OR]");

    public static void init() {
        ClientSendMessageEvents.ALLOW_COMMAND.register((command) -> {
            if (isModSendingCommand) return true;

            String cleanCommand = CLEAN_COLOR_CODES.matcher(command).replaceAll("");

            if (ModConfig.INSTANCE.nvpMode) {
                if (cleanCommand.startsWith("freezing ") || cleanCommand.startsWith("frz ")) {
                    String[] parts = cleanCommand.split(" ");
                    if (parts.length >= 2) {
                        processedNicks.clear();
                        mainTarget = parts[1];
                        originalTarget = parts[1];
                        processedNicks.add(originalTarget.toLowerCase());
                        currentState = State.AWAITING_INITIAL_DUPE;
                        timeoutTicks = TIMEOUT_TICKS;
                        sendHiddenCommand("dupeip " + originalTarget);
                        if (ModConfig.INSTANCE.DEBUG_MODE) System.out.println("[VaguriAssist] NVP Mode: Auto dupeip for " + originalTarget);
                    }
                }
            }

            if (ModConfig.INSTANCE.hmMode) {
                if (cleanCommand.startsWith("hm freezing ") || cleanCommand.startsWith("hm frz ")) {
                    String[] parts = cleanCommand.split(" ");
                    if (parts.length >= 3) {
                        processedNicks.clear();
                        mainTarget = parts[2];
                        originalTarget = parts[2];
                        processedNicks.add(originalTarget.toLowerCase());
                        currentState = State.AWAITING_INITIAL_DUPE;
                        timeoutTicks = TIMEOUT_TICKS;
                        sendHiddenCommand("dupeip " + originalTarget);
                        if (ModConfig.INSTANCE.DEBUG_MODE) System.out.println("[VaguriAssist] HM Mode: Auto dupeip for " + originalTarget);
                    }
                }
            }

            if (cleanCommand.startsWith("dupeip ")) {
                String[] parts = cleanCommand.split(" ");
                if (parts.length >= 2) {
                    processedNicks.clear();
                    mainTarget = parts[1];
                    originalTarget = parts[1];
                    processedNicks.add(originalTarget.toLowerCase());
                    currentState = State.AWAITING_INITIAL_DUPE;
                    timeoutTicks = TIMEOUT_TICKS;
                    if (ModConfig.INSTANCE.DEBUG_MODE) System.out.println("[VaguriAssist] Started scanning for " + originalTarget);
                }
            }
            return true;
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (currentState == State.IDLE && pendingNicks.isEmpty()) return true;

            String plainText = message.getString();

            switch (currentState) {
                case AWAITING_INITIAL_DUPE:
                    if (plainText.contains(originalTarget)) {
                        List<String> allRedNicks = extractAllRedNicks(message);
                        if (!allRedNicks.isEmpty()) {
                            for (String nick : allRedNicks) {
                                String lower = nick.toLowerCase();
                                if (!processedNicks.contains(lower)) {
                                    processedNicks.add(lower);
                                    pendingNicks.add(nick);
                                    if (ModConfig.INSTANCE.DEBUG_MODE) System.out.println("[VaguriAssist] Queued nick: " + nick);
                                }
                            }
                            resetState();
                        }
                    }
                    break;
                case AWAITING_REVERSE_DUPE:
                    if (!mainTarget.isEmpty() && plainText.contains(mainTarget)) {
                        sendHiddenCommand("checkban " + bannedTwink);
                        currentState = State.AWAITING_BAN_CHECK;
                        timeoutTicks = TIMEOUT_TICKS;
                        if (ModConfig.INSTANCE.DEBUG_MODE) System.out.println("[VaguriAssist] Reverse dupe confirmed for " + bannedTwink + ", checking ban");
                    }
                    break;
                case AWAITING_BAN_CHECK:
                    Matcher m = BAN_TIME_PATTERN.matcher(plainText);
                    if (m.find()) {
                        try {
                            int days = Integer.parseInt(m.group(1));
                            int hours = Integer.parseInt(m.group(2));
                            double totalDays = days + (hours / 24.0);
                            int newBanDays = (int) Math.ceil(totalDays * 2.0);
                            String finalCommand = String.format("/banip %s %dd 2.9(%s)", mainTarget, newBanDays, bannedTwink);

                            pendingNicks.clear();
                            mainTarget = "";
                            scheduledAction = () -> {
                                Screen screen = new BanConfirmScreen(finalCommand);
                                Minecraft.getInstance().setScreen(screen);
                            };
                            if (ModConfig.INSTANCE.DEBUG_MODE) System.out.println("[VaguriAssist] Suggesting: " + finalCommand);
                        } catch (NumberFormatException e) {
                            System.err.println("[VaguriAssist] Failed to parse ban time");
                        }
                        resetState();
                    }
                    break;
            }
            return true;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!pendingNicks.isEmpty() && currentState == State.IDLE) {
                if (bulkDelayTicks > 0) {
                    bulkDelayTicks--;
                } else {
                    processNextNick();
                }
            }

            if (pendingNicks.isEmpty() && currentState == State.IDLE) {
                mainTarget = "";
            }

            if (timeoutTicks > 0) {
                timeoutTicks--;
                if (timeoutTicks == 0 && currentState != State.IDLE) {
                    if (ModConfig.INSTANCE.DEBUG_MODE) System.out.println("[VaguriAssist] State timeout, resetting.");
                    resetState();
                }
            }
            if (scheduledAction != null) {
                scheduledAction.run();
                scheduledAction = null;
            }
        });
    }

    private static void processNextNick() {
        String nick = pendingNicks.poll();
        if (nick == null) return;

        bannedTwink = nick;
        originalTarget = nick;
        currentState = State.AWAITING_REVERSE_DUPE;
        timeoutTicks = TIMEOUT_TICKS;
        sendHiddenCommand("dupeip " + nick);
        bulkDelayTicks = BULK_DELAY;
        if (ModConfig.INSTANCE.DEBUG_MODE) System.out.println("[VaguriAssist] Reverse-checking: " + nick + " (remaining: " + pendingNicks.size() + ")");
    }

    private static void sendHiddenCommand(String cmd) {
        isModSendingCommand = true;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.connection.sendCommand(cmd);
        isModSendingCommand = false;
    }

    private static void resetState() {
        currentState = State.IDLE;
        originalTarget = "";
        bannedTwink = "";
        timeoutTicks = 0;
    }

    private static List<String> extractAllRedNicks(Component component) {
        List<String> redTexts = new ArrayList<>();
        visitTextForRedColor(component, Style.EMPTY, redTexts);
        List<String> nicks = new ArrayList<>();
        for (String redText : redTexts) {
            String cleaned = redText.replace("*", "").trim();
            Matcher m = NICK_PATTERN.matcher(cleaned);
            if (m.find()) {
                String nick = m.group(1);
                if (!nicks.contains(nick)) nicks.add(nick);
            }
        }
        return nicks;
    }

    private static void visitTextForRedColor(Component component, Style parentStyle, List<String> out) {
        Style currentStyle = component.getStyle();
        TextColor color = currentStyle.getColor() != null ? currentStyle.getColor() : parentStyle.getColor();
        if (color != null && isRedColor(color)) {
            String str = component.getString();
            if (!str.isEmpty() && !str.trim().isEmpty()) out.add(str);
        }
        for (Component sibling : component.getSiblings()) {
            visitTextForRedColor(sibling, currentStyle, out);
        }
    }

    private static boolean isRedColor(TextColor color) {
        int rgb = color.getValue();
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return r > 100 && r > (g + 50) && r > (b + 50);
    }
}
