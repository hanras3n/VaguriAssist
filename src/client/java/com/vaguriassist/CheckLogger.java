package com.vaguriassist;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class CheckLogger {

    private static final File FILE = new File("C:/VaguriAssist/проверки.txt");
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private CheckLogger() {
    }

    public static void log(String text) {
        try {
            FILE.getParentFile().mkdirs();
            try (PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(FILE, true), StandardCharsets.UTF_8))) {
                writer.println("[" + LocalDateTime.now().format(FORMAT) + "] " + text);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void logCheck(String nick, String reason, String mode, int anarchyNumber, boolean online) {
        if (online) {
            return;
        }
        log("Проверка: " + nick
                + " | Режим: " + mode
                + " | Анка: #" + anarchyNumber
                + " | Причина: " + reason);
    }

    public static void logEnd(String nick, String result, boolean destroyStash, String banReason) {
        log("Завершение: " + nick
                + " | Результат: " + result
                + " | Снос стеша: " + (destroyStash ? "да" : "нет")
                + " | banReason: " + (banReason == null ? "-" : banReason));
    }

    public static void logBan(String nick, String reason) {
        log("Проверка: " + nick + modeInfo() + " | Результат: забанили (" + reason + ")");
    }

    public static void logRelease(String nick) {
        log("Проверка: " + nick + modeInfo() + " | Результат: отпустили");
    }

    private static String modeInfo() {
        String mode = VaguriAssistClient.getDetectedMode();
        String server = VaguriAssistClient.getDetectedServer();
        String result = "";
        if (mode != null && !mode.isEmpty()) {
            result += " | Режим: " + mode;
        }
        if (!server.isEmpty()) {
            result += " | Анка: #" + server;
        }
        return result;
    }
}
