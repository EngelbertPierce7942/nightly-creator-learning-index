package education.creatorindex.infrai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public final class InfraiRestClient {
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final String apiKey;

    public InfraiRestClient(HttpClient http, ObjectMapper mapper, String baseUrl, String apiKey) {
        this.http = http;
        this.mapper = mapper;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public JsonNode post(String path, Map<String, ?> body, String idempotencyKey) {
        try {
            String json = mapper.writeValueAsString(body);
            for (int attempt = 0; attempt < 4; attempt++) {
                HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .timeout(Duration.ofSeconds(45))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .header("Idempotency-Key", idempotencyKey)
                        .method("POST", HttpRequest.BodyPublishers.ofString(json))
                        .build();
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode envelope = mapper.readTree(response.body());
                if (response.statusCode() == 429) {
                    pause(response.headers().firstValue("Retry-After").orElse(null), attempt);
                    continue;
                }
                if (!envelope.path("ok").asBoolean(false)) {
                    JsonNode error = envelope.path("error");
                    throw new InfraiException(error.path("code").asText("INFRAI_ERROR"),
                            error.path("message").asText("Infrai request was rejected"), response.statusCode());
                }
                if (response.statusCode() >= 500) {
                    throw new IOException("Infrai transport response " + response.statusCode());
                }
                return envelope.path("data");
            }
            throw new IOException("Retry budget exhausted");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Infrai request interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("Infrai transport request failed", e);
        }
    }

    private static void pause(String retryAfter, int attempt) throws InterruptedException {
        long millis;
        try {
            millis = retryAfter == null ? 250L << attempt : Long.parseLong(retryAfter) * 1000L;
        } catch (NumberFormatException ignored) {
            millis = 250L << attempt;
        }
        Thread.sleep(Math.min(millis, 8_000L));
    }
}
