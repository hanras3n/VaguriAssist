package com.vaguriassist;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatTabManager {

	private static final Map<String, List<ChatMessage>> tabMessages = new HashMap<>();
	private static int unreadCount = 0;

	public static void init() {
		for (ChatTabData tab : ModConfig.INSTANCE.chatTabs) {
			tabMessages.put(tab.name, new ArrayList<>());
		}
	}

	public static void routeMessage(String text, Component component) {
		ModConfig config = ModConfig.INSTANCE;
		if (ChatFilterEngine.isBlacklisted(text, null)) return;

		boolean routed = false;
		for (ChatTabData tab : config.chatTabs) {
			if (ChatFilterEngine.matches(tab, text)) {
				addMessageToTab(tab.name, text, component);
				routed = true;
				if (!tab.hideFromMain) {
					addMessageToTab("Все", text, component);
				}
			}
		}

		if (!routed) {
			addMessageToTab("Все", text, component);
		}

		checkAlerts(text);
	}

	private static void addMessageToTab(String tabName, String text, Component component) {
		tabMessages.computeIfAbsent(tabName, k -> new ArrayList<>()).add(new ChatMessage(text, component));
		List<ChatMessage> messages = tabMessages.get(tabName);
		if (messages.size() > ModConfig.INSTANCE.maxChatHistoryLines) {
			messages.remove(0);
		}

		if (!tabName.equals("Все") && !tabName.equals(getActiveTab())) {
			unreadCount++;
		}
	}

	public static List<ChatMessage> getMessages(String tabName) {
		return tabMessages.getOrDefault(tabName, new ArrayList<>());
	}

	public static List<ChatMessage> getActiveMessages() {
		return getMessages(getActiveTab());
	}

	public static String getActiveTab() {
		String active = ModConfig.INSTANCE.activeTabName;
		if (active == null || active.isEmpty()) return "Все";
		return active;
	}

	public static void setActiveTab(String tabName) {
		ModConfig.INSTANCE.activeTabName = tabName;
		unreadCount = 0;
		ModConfig.save();
	}

	public static int getUnreadCount() {
		return unreadCount;
	}

	public static void clearUnread() {
		unreadCount = 0;
	}

	public static void clearMessages(String tabName) {
		tabMessages.put(tabName, new ArrayList<>());
	}

	public static void clearAllMessages() {
		tabMessages.clear();
		for (ChatTabData tab : ModConfig.INSTANCE.chatTabs) {
			tabMessages.put(tab.name, new ArrayList<>());
		}
		tabMessages.put("Все", new ArrayList<>());
	}

	public static void ensureAllTab() {
		if (!tabMessages.containsKey("Все")) {
			tabMessages.put("Все", new ArrayList<>());
		}
	}

	public static void ensureTab(String tabName) {
		tabMessages.computeIfAbsent(tabName, k -> new ArrayList<>());
	}

	public static void syncTabsFromConfig() {
		Map<String, List<ChatMessage>> newMap = new HashMap<>();
		newMap.put("Все", tabMessages.getOrDefault("Все", new ArrayList<>()));
		for (ChatTabData tab : ModConfig.INSTANCE.chatTabs) {
			newMap.put(tab.name, tabMessages.getOrDefault(tab.name, new ArrayList<>()));
		}
		tabMessages.clear();
		tabMessages.putAll(newMap);
	}

	private static void checkAlerts(String text) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		String playerName = mc.player.getGameProfile().name();

		if (ModConfig.INSTANCE.soundOnMention && ChatFilterEngine.mentionsPlayer(text, playerName)) {
			mc.player.playSound(SoundEvents.PLAYER_LEVELUP, 0.5f, 1.2f);
		}

		if (ModConfig.INSTANCE.soundOnPrivateMessage && ChatFilterEngine.isPrivateMessage(text)) {
			mc.player.playSound(SoundEvents.PLAYER_LEVELUP, 0.5f, 1.5f);
		}
	}

	public static List<ChatTabData> getTabs() {
		return ModConfig.INSTANCE.chatTabs;
	}

	public static void addTab(ChatTabData tab) {
		ModConfig.INSTANCE.chatTabs.add(tab);
		tabMessages.put(tab.name, new ArrayList<>());
		ModConfig.save();
	}

	public static void removeTab(String tabName) {
		ModConfig.INSTANCE.chatTabs.removeIf(t -> t.name.equals(tabName));
		tabMessages.remove(tabName);
		if (getActiveTab().equals(tabName)) {
			ModConfig.INSTANCE.activeTabName = "Все";
		}
		ModConfig.save();
	}

	public static void updateTab(String oldName, ChatTabData updated) {
		List<ChatMessage> oldMessages = tabMessages.remove(oldName);
		ModConfig.INSTANCE.chatTabs.removeIf(t -> t.name.equals(oldName));
		ModConfig.INSTANCE.chatTabs.add(updated);
		if (oldMessages != null) {
			tabMessages.put(updated.name, oldMessages);
		}
		if (getActiveTab().equals(oldName)) {
			ModConfig.INSTANCE.activeTabName = updated.name;
		}
		ModConfig.save();
	}

	public static void moveTab(int fromIndex, int toIndex) {
		if (fromIndex < 0 || fromIndex >= ModConfig.INSTANCE.chatTabs.size()) return;
		if (toIndex < 0 || toIndex >= ModConfig.INSTANCE.chatTabs.size()) return;
		ChatTabData tab = ModConfig.INSTANCE.chatTabs.remove(fromIndex);
		ModConfig.INSTANCE.chatTabs.add(toIndex, tab);
		ModConfig.save();
	}

	public static class ChatMessage {
		public final String text;
		public final Component component;
		public final long timestamp;

		public ChatMessage(String text, Component component) {
			this.text = text;
			this.component = component;
			this.timestamp = System.currentTimeMillis();
		}
	}
}
