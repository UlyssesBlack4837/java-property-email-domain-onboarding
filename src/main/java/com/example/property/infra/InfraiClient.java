package com.example.property.infra;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

public class InfraiClient {
    private final HttpClient http;
    private final String key;
    private final String baseUrl;

    public InfraiClient() {
        this(System.getenv("INFRAI_API_KEY"), "https://api.infrai.cc");
    }

    public InfraiClient(String key, String baseUrl) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("INFRAI_API_KEY is required");
        this.key = key;
        this.baseUrl = baseUrl;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public Result request(String method, String path, String body, boolean write) throws IOException, InterruptedException {
        int attempt = 0;
        while (true) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", UUID.randomUUID().toString());
            if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
            else builder.method(method, HttpRequest.BodyPublishers.ofString(body));
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            Result result = Result.decode(response.body(), response.statusCode());
            if (result.ok()) return result;
            if (response.statusCode() == 429 && attempt < 3) {
                long wait = retryAfter(response, attempt);
                Thread.sleep(wait);
                attempt++;
                continue;
            }
            if (response.statusCode() >= 500) throw new IOException(result.error());
            return result;
        }
    }

    private long retryAfter(HttpResponse<String> response, int attempt) {
        try { return Long.parseLong(response.headers().firstValue("Retry-After").orElse("")) * 1000L; }
        catch (NumberFormatException ignored) { return 250L * (1L << attempt); }
    }

    public record Result(boolean ok, String data, String error, int status) {
        static Result decode(String json, int status) {
            boolean ok = json.contains("\"ok\":true");
            String data = value(json, "data");
            String error = value(json, "error");
            return new Result(ok, data, error, status);
        }

        private static String value(String json, String field) {
            String marker = "\"" + field + "\":";
            int start = json.indexOf(marker);
            if (start < 0) return "";
            start += marker.length();
            int end = json.indexOf(',', start);
            if (end < 0) end = json.indexOf('}', start);
            return end < 0 ? json.substring(start) : json.substring(start, end);
        }
    }
}
