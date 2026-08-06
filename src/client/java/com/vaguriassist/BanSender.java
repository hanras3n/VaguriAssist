package com.vaguriassist;

public final class BanSender {

	private BanSender() {
	}

	public static void send(String nick, int durationDays, String paragraph, String reason, boolean byIp) {
		send(buildCommand(nick, durationDays, paragraph, reason, byIp));
	}

	public static void sendFreezing(String nick) {
		send("/freezing " + nick);
	}

	public static void send(String command) {
		VaguriAssistClient.sendCommand(command);
	}

	public static String buildCommand(String nick, int durationDays, String paragraph, String reason, boolean byIp) {
		return "/" + (byIp ? "banip" : "ban")
				+ " " + nick
				+ " " + durationDays + "d"
				+ " " + paragraph
				+ " (" + reason + ")"
				+ " | Вопросы? " + ModConfig.get().vk + " -s";
	}
}
