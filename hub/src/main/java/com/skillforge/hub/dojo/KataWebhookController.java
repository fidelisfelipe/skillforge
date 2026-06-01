package com.skillforge.hub.dojo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@Slf4j
@RequiredArgsConstructor
public class KataWebhookController {

    private final KataValidator kataValidator;
    private final ObjectMapper objectMapper;

    private static final Set<String> HANDLED_ACTIONS = Set.of("opened", "synchronize", "reopened");
    private static final Pattern KATA_BRANCH_PATTERN = Pattern.compile("^kata-([0-9]+[a-z]+)-", Pattern.CASE_INSENSITIVE);

    @PostMapping("/kata/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "") String event,
            @RequestBody String rawPayload) {

        if (!"pull_request".equals(event)) {
            log.debug("Ignoring GitHub event: {}", event);
            return ResponseEntity.ok("ignored");
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String action = root.path("action").asText();

            if (!HANDLED_ACTIONS.contains(action)) {
                return ResponseEntity.ok("ignored");
            }

            JsonNode pr       = root.path("pull_request");
            String body       = pr.path("body").asText("");
            String branchRef  = pr.path("head").path("ref").asText();
            String cloneUrl   = pr.path("head").path("repo").path("clone_url").asText();

            String heroId       = extractField(body, "heroId");
            String kataId       = extractKataId(branchRef);
            int prNumber        = pr.path("number").asInt();
            String repoFullName = root.path("repository").path("full_name").asText();

            if (heroId == null || kataId == null) {
                log.warn("⚠️ Webhook rejected — branch={} heroId={} kataId={}", branchRef, heroId, kataId);
                return ResponseEntity.badRequest()
                    .body("PR body must contain 'heroId: <id>' and branch must match 'kata-NNNx-*'");
            }

            log.info("🔔 Kata submission received | kata={} hero={} PR=#{} repo={}", kataId, heroId, prNumber, repoFullName);
            kataValidator.validateAsync(heroId, kataId, cloneUrl, branchRef, prNumber, repoFullName);

            return ResponseEntity.ok("accepted");

        } catch (Exception e) {
            log.error("❌ Webhook processing error", e);
            return ResponseEntity.internalServerError().body("error");
        }
    }

    private String extractField(String body, String field) {
        if (body == null || body.isBlank()) return null;
        return body.lines()
            .filter(l -> l.toLowerCase().startsWith(field.toLowerCase() + ":"))
            .map(l -> l.substring(field.length() + 1).trim())
            .filter(v -> !v.isBlank())
            .findFirst()
            .orElse(null);
    }

    // "kata-001a-hero-solution" → "KATA-001A"
    private String extractKataId(String branchRef) {
        if (branchRef == null) return null;
        Matcher m = KATA_BRANCH_PATTERN.matcher(branchRef);
        return m.find() ? "KATA-" + m.group(1).toUpperCase() : null;
    }
}
