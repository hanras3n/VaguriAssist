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

    public static void logBan(String nick, String reason) {
        log("Проверка: " + nick + serverSuffix() + " | Результат: забанили (" + reason + ")");
    }

    public static void logRelease(String nick) {
        log("Проверка: " + nick + serverSuffix() + " | Результат: отпустили");
    }

    private static String serverSuffix() {
        String server = VaguriAssistClient.getDetectedServer();
        return server.isEmpty() ? "" : " | Анка: #" + server;
    }
}
