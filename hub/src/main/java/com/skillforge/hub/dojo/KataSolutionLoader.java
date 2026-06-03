package com.skillforge.hub.dojo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillforge.hub.github.GitHubClient;
import com.skillforge.hub.service.HeroRegistryService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads closed PRs from skillforge-katas on startup and restores the solution history.
 * Source of truth is GitHub — no hardcoded data in code.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KataSolutionLoader {

    @Value("${guild.github.token:}")
    private String token;

    @Value("${guild.github.owner:fidelisfelipe}")
    private String owner;

    private final KataProgressService kataProgress;
    private final ObjectMapper objectMapper;
    private final HeroRegistryService registry;
    private final GitHubClient gitHubClient;

    private static final Pattern BRANCH_PATTERN =
        Pattern.compile("^kata-([0-9]+[a-z]+)-", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCORE_PATTERN =
        Pattern.compile("\\*\\*Score\\*\\*\\s*\\|\\s*(\\d+)/100");
    private static final Pattern XP_PATTERN =
        Pattern.compile("\\*\\*XP earned\\*\\*\\s*\\|\\s*(\\d+)");
    private static final Pattern SKILL_PATTERN =
        Pattern.compile("\\*\\*Skill unlocked\\*\\*\\s*\\|\\s*`([^`]+)`");

    @PostConstruct
    void loadFromGitHub() {
        if (token == null || token.isBlank()) {
            log.warn("⚠️ GITHUB_TOKEN not set — kata solution history will be empty until a new submission arrives");
            return;
        }
        try {
            String kataRepo = owner + "/skillforge-katas";
            RestTemplate http = new RestTemplate();
            HttpHeaders headers = githubHeaders();

            String url = "https://api.github.com/repos/" + kataRepo
                + "/pulls?state=closed&per_page=100&sort=created&direction=asc";
            ResponseEntity<String> resp = http.exchange(url, HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

            JsonNode prs = objectMapper.readTree(resp.getBody());
            List<JsonNode> prList = new ArrayList<>();
            prs.forEach(prList::add);

            int loaded = 0;
            for (JsonNode pr : prList) {
                String branch = pr.path("head").path("ref").asText();
                String body   = pr.path("body").asText("");
                int prNumber  = pr.path("number").asInt();
                String closedAt = pr.path("closed_at").asText("");

                Matcher branchMatcher = BRANCH_PATTERN.matcher(branch);
                if (!branchMatcher.find()) continue;

                String kataId = "KATA-" + branchMatcher.group(1).toUpperCase();
                String heroId = extractHeroId(body);
                if (heroId == null) continue;

                long ts = closedAt.isBlank() ? System.currentTimeMillis()
                    : Instant.parse(closedAt).toEpochMilli();

                String commentsUrl = "https://api.github.com/repos/" + kataRepo
                    + "/issues/" + prNumber + "/comments";
                ResponseEntity<String> commentsResp = http.exchange(commentsUrl, HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);

                JsonNode comments = objectMapper.readTree(commentsResp.getBody());
                for (JsonNode comment : comments) {
                    String commentBody = comment.path("body").asText("");
                    if (!commentBody.startsWith("✅")) continue;

                    int score    = parseGroup(SCORE_PATTERN, commentBody, 100);
                    int xpEarned = parseGroup(XP_PATTERN, commentBody, 80);
                    String skill = parseSkill(commentBody);
                    kataProgress.recordAt(kataId, heroId, score, xpEarned, ts);
                    creditKataToHeroIssue(heroId, kataId, xpEarned, skill);
                    loaded++;
                    break;
                }
            }

            log.info("📥 Loaded {} kata solution(s) from GitHub PR history ({})", loaded, kataRepo);

        } catch (Exception e) {
            log.warn("⚠️ Failed to load kata solutions from GitHub: {}", e.getMessage());
        }
    }

    private void creditKataToHeroIssue(String heroId, String kataId, int xpEarned, String skill) {
        registry.getHeroById(heroId).ifPresent(hero -> {
            try {
                boolean credited = gitHubClient.addQuestXp(hero.issueNumber(), "kata-" + kataId.toLowerCase(), xpEarned);
                if (credited) {
                    log.info("💰 Retroactive kata XP credited | hero={} kata={} xp={}", heroId, kataId, xpEarned);
                }
                if (skill != null) {
                    gitHubClient.addLabel(hero.issueNumber(), "skill-validated:" + skill);
                }
            } catch (Exception e) {
                log.warn("⚠️ Failed to retroactively credit kata XP | hero={} kata={}: {}", heroId, kataId, e.getMessage());
            }
        });
    }

    private String parseSkill(String commentBody) {
        Matcher m = SKILL_PATTERN.matcher(commentBody);
        return m.find() ? m.group(1) : null;
    }

    private String extractHeroId(String body) {
        return body.lines()
            .filter(l -> l.toLowerCase().startsWith("heroid:"))
            .map(l -> l.substring(7).trim())
            .filter(v -> !v.isBlank())
            .findFirst()
            .orElse(null);
    }

    private int parseGroup(Pattern p, String text, int defaultVal) {
        Matcher m = p.matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : defaultVal;
    }

    private HttpHeaders githubHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "token " + token);
        h.set("Accept", "application/vnd.github.v3+json");
        return h;
    }
}
