package com.skillforge.hub.dojo;

import com.skillforge.dojo.message.KataThemeResponseMessage.KataDelivery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Service
@Slf4j
public class GitHubService {

    @Value("${guild.github.token:}")
    private String githubToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void createKataIssue(String heroId, KataDelivery kata) {
        log.info("📝 [STUB] Would create GitHub issue for hero {} | kata: {}", heroId, kata.kataId());
    }

    public void commentOnPR(String repoFullName, int prNumber, String body) {
        String url = "https://api.github.com/repos/" + repoFullName + "/issues/" + prNumber + "/comments";
        try {
            restTemplate.exchange(url, HttpMethod.POST,
                new HttpEntity<>(Map.of("body", body), githubHeaders()), String.class);
            log.info("💬 Comment posted on PR #{} in {}", prNumber, repoFullName);
        } catch (Exception e) {
            log.warn("⚠️ Failed to comment on PR #{}: {}", prNumber, e.getMessage());
        }
    }

    public void closePR(String repoFullName, int prNumber) {
        String url = "https://api.github.com/repos/" + repoFullName + "/pulls/" + prNumber;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "token " + githubToken)
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"state\":\"closed\"}"))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 300) {
                log.info("🔒 PR #{} closed in {}", prNumber, repoFullName);
            } else {
                log.warn("⚠️ Failed to close PR #{}: HTTP {}", prNumber, response.statusCode());
            }
        } catch (Exception e) {
            log.warn("⚠️ Failed to close PR #{}: {}", prNumber, e.getMessage());
        }
    }

    private HttpHeaders githubHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + githubToken);
        headers.set("Accept", "application/vnd.github.v3+json");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
