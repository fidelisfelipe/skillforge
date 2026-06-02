package com.skillforge.hub.dojo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class GitHubForksService {

    @Value("${guild.github.token:}")
    private String token;

    @Value("${guild.github.owner:fidelisfelipe}")
    private String owner;

    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd/MM/yy").withZone(ZoneId.systemDefault());

    private List<ForkEntry> forks = List.of();

    public record ForkEntry(String login, String avatarUrl, String profileUrl, String forkedAt) {}

    @PostConstruct
    void load() {
        if (token == null || token.isBlank()) {
            log.warn("⚠️ GITHUB_TOKEN not set — fork list will be empty");
            return;
        }
        try {
            RestTemplate http = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "token " + token);
            headers.set("Accept", "application/vnd.github.v3+json");

            String url = "https://api.github.com/repos/" + owner
                + "/skillforge/forks?per_page=100&sort=newest";

            ResponseEntity<String> resp = http.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            JsonNode arr = objectMapper.readTree(resp.getBody());
            List<ForkEntry> list = new ArrayList<>();
            arr.forEach(fork -> {
                String login   = fork.path("owner").path("login").asText("");
                String avatar  = fork.path("owner").path("avatar_url").asText("");
                String profile = fork.path("owner").path("html_url").asText("");
                String rawDate = fork.path("created_at").asText("");
                String date    = rawDate.isBlank() ? "—"
                    : FMT.format(Instant.parse(rawDate));
                if (!login.isBlank()) list.add(new ForkEntry(login, avatar, profile, date));
            });

            this.forks = List.copyOf(list);
            log.info("🍴 {} fork(s) of {}/skillforge loaded", forks.size(), owner);

        } catch (Exception e) {
            log.warn("⚠️ Failed to load forks from GitHub: {}", e.getMessage());
        }
    }

    public List<ForkEntry> getForks() { return forks; }
    public int getForkCount() { return forks.size(); }
}
