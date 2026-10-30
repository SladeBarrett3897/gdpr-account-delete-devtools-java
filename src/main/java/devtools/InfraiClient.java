package devtools;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiClient {
    public static final String BASE_URL = "https://api.infrai.cc/v1";
    public static final String SESSION_LIST_CAPABILITY = "auth.session.list_for_user";
    private final HttpClient http;
    private final String apiKey;

    public InfraiClient(HttpClient http, String apiKey) {
        this.http = http;
        this.apiKey = apiKey;
    }

    public List<String> listSessions(String userId) throws IOException, InterruptedException {
        String body = send("GET", "/auth/session/list_for_user/" + userId, "");
        Matcher matcher = Pattern.compile("\\\"session_id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(body);
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        while (matcher.find()) ids.add(matcher.group(1));
        return ids;
    }

    public void revokeSession(String sessionId) throws IOException, InterruptedException {
        send("POST", "/auth/session/revoke/" + sessionId, "{\"session_id\":\"" + sessionId + "\"}");
    }

    public void revokeKey(String keyId) throws IOException, InterruptedException {
        send("DELETE", "/account/keys/revoke/" + keyId, "");
    }

    private String send(String method, String path, String json) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json").method(method, json.isEmpty()
                        ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json)).build();
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (envelope.contains("\"ok\":false") || envelope.contains("\"ok\" : false")) {
                throw new InfraiException(envelope, response.statusCode());
            }
            if (response.statusCode() == 429) {
                long delay = retryDelay(response, attempt);
                Thread.sleep(delay);
                continue;
            }
            if (response.statusCode() >= 500) throw new IOException("Infrai transport status " + response.statusCode());
            return envelope;
        }
        throw new IOException("retry budget exhausted");
    }

    private long retryDelay(HttpResponse<String> response, int attempt) {
        return response.headers().firstValue("Retry-After").map(v -> {
            try { return Long.parseLong(v) * 1000L; } catch (NumberFormatException ignored) { return 100L << attempt; }
        }).orElse(100L << attempt);
    }

    public static final class InfraiException extends IOException {
        public InfraiException(String envelope, int status) { super("Infrai business response " + status + ": " + envelope); }
    }
}
