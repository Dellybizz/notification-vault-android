package com.dellybizz.notificationvault;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class ApiClient {
    private final String baseUrl;

    ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    DeviceProfile fetchProfile(String deviceToken) throws ApiException {
        JSONObject body = new JSONObject();
        try {
            body.put("deviceToken", deviceToken);
        } catch (Exception exception) {
            throw new ApiException("Could not create request.", 0, exception);
        }

        JSONObject response = postJson("/api/device/profile", body);
        JSONObject device = response.optJSONObject("device");
        if (device == null) throw new ApiException("Server returned an invalid device profile.", 0, null);

        List<DeviceProfile.ViewSource> sources = new ArrayList<>();
        JSONArray sourceArray = response.optJSONArray("viewSources");
        if (sourceArray != null) {
            for (int index = 0; index < sourceArray.length(); index++) {
                JSONObject source = sourceArray.optJSONObject(index);
                if (source == null) continue;
                sources.add(new DeviceProfile.ViewSource(
                        source.optString("id", ""),
                        source.optString("name", "Unnamed device"),
                        source.optString("type", "other")
                ));
            }
        }

        return new DeviceProfile(
                device.optString("id", ""),
                device.optString("name", "Unnamed device"),
                device.optString("type", "other"),
                device.optBoolean("canRecord", false),
                sources
        );
    }

    private JSONObject postJson(String path, JSONObject body) throws ApiException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(baseUrl + path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(15_000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "NotificationVaultAndroid/0.1");

            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(bytes);
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String payload = readAll(stream);

            if (status < 200 || status >= 300) {
                String message = "Server rejected the request.";
                if (!payload.isEmpty()) {
                    try {
                        JSONObject errorJson = new JSONObject(payload);
                        message = errorJson.optString("error", message);
                    } catch (Exception ignored) {
                        // Keep the safe generic message rather than exposing arbitrary HTML/server output.
                    }
                }
                throw new ApiException(message, status, null);
            }

            return payload.isEmpty() ? new JSONObject() : new JSONObject(payload);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException("Could not reach Notification Vault. Check your internet connection.", 0, exception);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) builder.append(line);
        }
        return builder.toString();
    }

    static final class ApiException extends Exception {
        final int statusCode;

        ApiException(String message, int statusCode, Throwable cause) {
            super(message, cause);
            this.statusCode = statusCode;
        }
    }
}
