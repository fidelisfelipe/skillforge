package com.skillforge.hub.onboarding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

@Component
public class GitHubOAuthClient {

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final String clientId;
    private final String clientSecret;

    public GitHubOAuthClient(
            @Value("${guild.github.oauth.client-id:}") String clientId,
            @Value("${guild.github.oauth.client-secret:}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public boolean isConfigured() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }

    public String buildAuthorizationUrl(String state, String callbackUrl) {
        return "https://github.com/login/oauth/authorize" +
               "?client_id=" + clientId +
               "&redirect_uri=" + callbackUrl +
               "&scope=user:email" +
               "&state=" + state;
    }

    public Optional<String> exchangeCodeForToken(String code) {
        try {
            String body = "client_id=%s&client_secret=%s&code=%s"
                .formatted(clientId, clientSecret, code);

            HttpResponse<String> response = http.send(
                HttpRequest.newBuilder()
                    .uri(URI.create("https://github.com/login/oauth/access_token"))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build(),
                HttpResponse.BodyHandlers.ofString());

            JsonNode json = mapper.readTree(response.body());
            String token = json.path("access_token").asText("");
            return token.isBlank() ? Optional.empty() : Optional.of(token);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<String> getUserLogin(String accessToken) {
        try {
            JsonNode user = githubGet("https://api.github.com/user", accessToken);
            String login = user.path("login").asText("");
            return login.isBlank() ? Optional.empty() : Optional.of(login);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<String> getPrimaryEmail(String accessToken) {
        try {
            JsonNode emails = githubGet("https://api.github.com/user/emails", accessToken);
            for (JsonNode entry : emails) {
                if (entry.path("primary").asBoolean() && entry.path("verified").asBoolean()) {
                    String email = entry.path("email").asText("");
                    if (!email.isBlank()) return Optional.of(email);
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private JsonNode githubGet(String url, String accessToken) throws Exception {
        HttpResponse<String> response = http.send(
            HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
        return mapper.readTree(response.body());
    }
}
