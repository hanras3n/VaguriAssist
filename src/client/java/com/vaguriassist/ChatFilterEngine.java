package com.vaguriassist;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ChatFilterEngine {

	public static boolean matches(ChatTabData tab, String messageText) {
		if (tab.filters.isEmpty()) return false;
		for (ChatTabData.ChatFilterData filter : tab.filters) {
			if (matchesFilter(filter, messageText)) {
				return true;
			}
		}
		return false;
	}

	private static boolean matchesFilter(ChatTabData.ChatFilterData filter, String text) {
		if (filter.pattern == null || filter.pattern.isEmpty()) return false;
		String lowerText = text.toLowerCase();
		String lowerPattern = filter.pattern.toLowerCase();

		switch (filter.type) {
			case "contains":
				return lowerText.contains(lowerPattern);
			case "starts_with":
				return lowerText.startsWith(lowerPattern);
			case "regex":
				try {
					return Pattern.matches(filter.pattern, text);
				} catch (PatternSyntaxException e) {
					return false;
				}
			default:
				return lowerText.contains(lowerPattern);
		}
	}

	public static boolean isBlacklisted(String messageText, String senderName) {
		ModConfig config = ModConfig.INSTANCE;
		String lowerText = messageText.toLowerCase();
		for (String word : config.blacklistedWords) {
			if (!word.isEmpty() && lowerText.contains(word.toLowerCase())) {
				return true;
			}
		}
		if (senderName != null) {
			String lowerSender = senderName.toLowerCase();
			for (String player : config.blacklistedPlayers) {
				if (!player.isEmpty() && lowerSender.equals(player.toLowerCase())) {
					return true;
				}
			}
		}
		return false;
	}

	public static int getHighlightColor(String messageText) {
		ModConfig config = ModConfig.INSTANCE;
		String lowerText = messageText.toLowerCase();
		for (HighlightEntry entry : config.highlightEntries) {
			if (entry.word != null && !entry.word.isEmpty() && lowerText.contains(entry.word.toLowerCase())) {
				return entry.color;
			}
		}
		return -1;
	}

	public static boolean mentionsPlayer(String messageText, String playerName) {
		if (playerName == null || playerName.isEmpty()) return false;
		return messageText.toLowerCase().contains(playerName.toLowerCase());
	}

	public static boolean isPrivateMessage(String messageText) {
		String lower = messageText.toLowerCase();
		return lower.startsWith("от [") || lower.startsWith("to [") || lower.contains("[лс]") || lower.contains("[pm]");
	}
}
