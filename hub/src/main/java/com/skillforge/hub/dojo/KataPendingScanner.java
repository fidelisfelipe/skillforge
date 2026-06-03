package com.skillforge.hub.dojo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Polls open PRs in skillforge-katas periodically and on startup,
 * replacing the need for a GitHub webhook to trigger kata validation.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KataPendingScanner {

    @Value("${guild.github.token:}")
    private String token;

    @Value("${guild.github.owner:fidelisfelipe}")
    private String owner;

    private final KataValidator kataValidator;
    private final ObjectMapper objectMapper;

    private static final Pattern BRANCH_PATTERN =
        Pattern.compile("^kata-([a-z0-9]+)-", Pattern.CASE_INSENSITIVE);

    private final Set<Integer> inProgress = ConcurrentHashMap.newKeySet();

    @PostConstruct
    void init() {
        if (token == null || token.isBlank()) {
            log.warn("⚠️ KataPendingScanner: GITHUB_TOKEN not set — pending PR scan disabled");
        } else {
            log.info("🔄 KataPendingScanner started — polling {}/{} every {}ms (first run in 15s)",
                owner, "skillforge-katas",
                System.getProperty("skillforge.kata.scan-interval-ms", "300000"));
        }
    }

    // initialDelay gives KataSolutionLoader (PostConstruct) time to finish first
    @Scheduled(fixedDelayString = "${skillforge.kata.scan-interval-ms:300000}", initialDelay = 15_000)
    void scanPendingPRs() {
        if (token == null || token.isBlank()) return;

        try {
            String kataRepo = owner + "/skillforge-katas";
            log.info("🔍 Scanning open PRs in {}", kataRepo);
            RestTemplate http = new RestTemplate();
            HttpHeaders headers = githubHeaders();

            String url = "https://api.github.com/repos/" + kataRepo
                + "/pulls?state=open&per_page=100&sort=created&direction=asc";
            ResponseEntity<String> resp = http.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            JsonNode prs = objectMapper.readTree(resp.getBody());
            log.info("📋 {} open PR(s) found in {}", prs.size(), kataRepo);
            int triggered = 0;

            for (JsonNode pr : prs) {
                int prNumber = pr.path("number").asInt();
                if (inProgress.contains(prNumber)) {
                    log.info("⏭️ PR #{} skipped — validation already in progress", prNumber);
                    continue;
                }

                String branch   = pr.path("head").path("ref").asText();
                String body     = pr.path("body").asText("");
                String cloneUrl = pr.path("head").path("repo").path("clone_url").asText();

                Matcher branchMatcher = BRANCH_PATTERN.matcher(branch);
                if (!branchMatcher.find()) {
                    log.info("⏭️ PR #{} skipped — branch '{}' does not match kata pattern (kata-NNNx-*)", prNumber, branch);
                    continue;
                }

                String kataId = "KATA-" + branchMatcher.group(1).toUpperCase();
                String heroId = extractHeroId(body);
                if (heroId == null) {
                    log.info("⏭️ PR #{} skipped — no 'heroId:' found in PR body (kata={})", prNumber, kataId);
                    continue;
                }

                if (alreadyValidated(http, headers, kataRepo, prNumber)) {
                    log.info("⏭️ PR #{} skipped — already has a validation comment (kata={} hero={})", prNumber, kataId, heroId);
                    continue;
                }

                log.info("🔎 Pending kata PR found | PR=#{} kata={} hero={}", prNumber, kataId, heroId);
                inProgress.add(prNumber);
                kataValidator.validateAsync(heroId, kataId, cloneUrl, branch, prNumber, kataRepo)
                    .whenComplete((v, ex) -> inProgress.remove(prNumber));
                triggered++;
            }

            if (triggered > 0) {
                log.info("🚀 Triggered validation for {} pending kata PR(s)", triggered);
            } else {
                log.info("✅ No pending kata PRs to validate");
            }

        } catch (Exception e) {
            log.warn("⚠️ Kata pending scan failed: {}", e.getMessage());
        }
    }

    private boolean alreadyValidated(RestTemplate http, HttpHeaders headers,
                                     String kataRepo, int prNumber) {
        try {
            String url = "https://api.github.com/repos/" + kataRepo
                + "/issues/" + prNumber + "/comments";
            ResponseEntity<String> resp = http.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode comments = objectMapper.readTree(resp.getBody());
            for (JsonNode c : comments) {
                String commentBody = c.path("body").asText("");
                if (commentBody.startsWith("✅")) return true;
            }
        } catch (Exception e) {
            log.debug("Could not check comments for PR #{}: {}", prNumber, e.getMessage());
        }
        return false;
    }

    private String extractHeroId(String body) {
        return body.lines()
            .filter(l -> l.toLowerCase().startsWith("heroid:"))
            .map(l -> l.substring(7).trim())
            .filter(v -> !v.isBlank())
            .findFirst()
            .orElse(null);
    }

    private HttpHeaders githubHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "token " + token);
        h.set("Accept", "application/vnd.github.v3+json");
        return h;
    }
}
