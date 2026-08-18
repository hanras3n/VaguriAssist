package com.vaguriassist;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class JournalAPI {

    private static final String BASE_URL = "https://journal.holyworld.me/srv/api/v1";
    private static final JournalAPI INSTANCE = new JournalAPI();
    private final HttpClient httpClient;

    private JournalAPI() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static JournalAPI getInstance() {
        return INSTANCE;
    }

    private HttpRequest.Builder createRequest(String endpoint) {
        String token = ModConfig.INSTANCE.apiToken;
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("x-token", token)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15));
    }

    public CompletableFuture<Boolean> startCheckout(String username, String reason, String mode, int anarchyNumber) {
        if (ModConfig.INSTANCE.apiToken.isEmpty()) {
            ToastNotification.show("API-токен не задан! /vaguriassist setapi", "error");
            return CompletableFuture.completedFuture(false);
        }
        JsonObject body = new JsonObject();
        body.addProperty("anarchyNumber", anarchyNumber);
        body.addProperty("mode", mode);
        body.addProperty("reason", reason);
        body.addProperty("username", username);
        body.addProperty("isPvpAnarchy", false);

        HttpRequest request = createRequest("/checkout/start")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                        boolean success = json.has("success") && json.get("success").getAsBoolean();
                        if (success) {
                            ToastNotification.show("Проверка " + username + " начата в журнале!", "success");
                        } else {
                            ToastNotification.show("Не удалось начать проверку в журнале.", "error");
                        }
                        return success;
                    } else {
                        ToastNotification.show("Ошибка API: код " + response.statusCode(), "error");
                        return false;
                    }
                })
                .exceptionally(ex -> {
                    ToastNotification.show("Ошибка сети: " + ex.getMessage(), "error");
                    return false;
                });
    }

    public CompletableFuture<Boolean> endCheckout(String result, Boolean destroyStash, String banReason) {
        if (ModConfig.INSTANCE.apiToken.isEmpty()) return CompletableFuture.completedFuture(false);
        JsonObject body = new JsonObject();
        body.addProperty("result", result);
        if (destroyStash != null) body.addProperty("destroyStash", destroyStash);
        if (banReason != null) body.addProperty("banReason", banReason);

        HttpRequest request = createRequest("/checkout/end")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                        boolean success = json.has("success") && json.get("success").getAsBoolean();
                        if (success) {
                            ToastNotification.show("Проверка завершена (результат: " + result + ")", "success");
                        } else {
                            ToastNotification.show("Не удалось завершить проверку.", "error");
                        }
                        return success;
                    } else {
                        ToastNotification.show("Ошибка API: код " + response.statusCode(), "error");
                        return false;
                    }
                })
                .exceptionally(ex -> {
                    ToastNotification.show("Ошибка сети: " + ex.getMessage(), "error");
                    return false;
                });
    }

    public CompletableFuture<Boolean> checkApiStatus() {
        if (ModConfig.INSTANCE.apiToken.isEmpty()) return CompletableFuture.completedFuture(false);
        HttpRequest request = createRequest("/status").GET().build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() == 200)
                .exceptionally(ex -> false);
    }

    public CompletableFuture<JsonObject> getUserInfo() {
        if (ModConfig.INSTANCE.apiToken.isEmpty()) {
            ToastNotification.show("API-токен не задан!", "error");
            return CompletableFuture.completedFuture(null);
        }
        HttpRequest request = createRequest("/me").GET().build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonParser.parseString(response.body()).getAsJsonObject();
                    } else {
                        ToastNotification.show("Ошибка API /me: код " + response.statusCode(), "error");
                        return null;
                    }
                })
                .exceptionally(ex -> {
                    ToastNotification.show("Ошибка сети: " + ex.getMessage(), "error");
                    return null;
                });
    }

    public CompletableFuture<JsonObject> getStats() {
        if (ModConfig.INSTANCE.apiToken.isEmpty()) {
            ToastNotification.show("API-токен не задан!", "error");
            return CompletableFuture.completedFuture(null);
        }
        HttpRequest request = createRequest("/stats").GET().build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonParser.parseString(response.body()).getAsJsonObject();
                    } else {
                        ToastNotification.show("Ошибка API /stats: код " + response.statusCode(), "error");
                        return null;
                    }
                })
                .exceptionally(ex -> {
                    ToastNotification.show("Ошибка сети: " + ex.getMessage(), "error");
                    return null;
                });
    }

    public CompletableFuture<JsonObject> getCheckoutStatus() {
        if (ModConfig.INSTANCE.apiToken.isEmpty()) {
            ToastNotification.show("API-токен не задан!", "error");
            return CompletableFuture.completedFuture(null);
        }
        HttpRequest request = createRequest("/checkout/status").GET().build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonParser.parseString(response.body()).getAsJsonObject();
                    } else {
                        ToastNotification.show("Ошибка API /checkout/status: код " + response.statusCode(), "error");
                        return null;
                    }
                })
                .exceptionally(ex -> {
                    ToastNotification.show("Ошибка сети: " + ex.getMessage(), "error");
                    return null;
                });
    }

    public CompletableFuture<JsonArray> getStaffList() {
        if (ModConfig.INSTANCE.apiToken.isEmpty()) {
            ToastNotification.show("API-токен не задан!", "error");
            return CompletableFuture.completedFuture(null);
        }
        HttpRequest request = createRequest("/staff").GET().build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonParser.parseString(response.body()).getAsJsonArray();
                    } else {
                        ToastNotification.show("Ошибка API /staff: код " + response.statusCode(), "error");
                        return null;
                    }
                })
                .exceptionally(ex -> {
                    ToastNotification.show("Ошибка сети: " + ex.getMessage(), "error");
                    return null;
                });
    }
}
