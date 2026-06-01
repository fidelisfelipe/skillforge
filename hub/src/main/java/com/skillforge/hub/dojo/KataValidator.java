package com.skillforge.hub.dojo;

import com.skillforge.dojo.message.KataValidationResultMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class KataValidator {

    private final RabbitTemplate rabbitTemplate;

    private static final Map<String, String> KATA_SKILLS = Map.ofEntries(
        Map.entry("KATA-001A", "java-21-virtual-threads"),
        Map.entry("KATA-001B", "java-21-virtual-threads"),
        Map.entry("KATA-001C", "java-21-virtual-threads"),
        Map.entry("KATA-002A", "java-21-records"),
        Map.entry("KATA-002B", "java-21-records"),
        Map.entry("KATA-002C", "java-21-records"),
        Map.entry("KATA-003A", "java-21-pattern-matching"),
        Map.entry("KATA-003B", "java-21-pattern-matching"),
        Map.entry("KATA-003C", "java-21-pattern-matching")
    );

    private static final Map<String, Integer> KATA_XP = Map.ofEntries(
        Map.entry("KATA-001A", 80),  Map.entry("KATA-001B", 100), Map.entry("KATA-001C", 120),
        Map.entry("KATA-002A", 80),  Map.entry("KATA-002B", 90),  Map.entry("KATA-002C", 110),
        Map.entry("KATA-003A", 90),  Map.entry("KATA-003B", 100), Map.entry("KATA-003C", 130)
    );

    @Async
    public void validateAsync(String heroId, String kataId, String cloneUrl, String branch) {
        log.info("🔍 Starting validation | kata={} hero={} branch={}", kataId, heroId, branch);
        Path tempDir = null;

        try {
            tempDir = Files.createTempDirectory("kata-validate-");

            // 1. Clone the solution branch
            int cloneExit = runProcess(tempDir.getParent(),
                "git", "clone", "-b", branch, "--depth", "1", cloneUrl, tempDir.toString());

            if (cloneExit != 0) {
                publishFailure(heroId, kataId, 0, "git clone failed — verify branch exists: " + branch);
                return;
            }

            // 2. Run mvn verify
            StringBuilder output = new StringBuilder();
            int mvnExit = runProcessWithOutput(tempDir, output, mvnExecutable(), "verify", "-B");

            boolean passed = mvnExit == 0;
            int score      = passed ? 100 : parseScore(output.toString());
            int xpEarned   = passed ? KATA_XP.getOrDefault(kataId, 80) : 0;
            String skill   = passed ? KATA_SKILLS.get(kataId) : null;
            String feedback = extractFeedback(output.toString(), passed);

            KataValidationResultMessage result = new KataValidationResultMessage(
                heroId, kataId, passed, score, xpEarned, skill, feedback, System.currentTimeMillis()
            );
            rabbitTemplate.convertAndSend("kata.validation.results", result);

            log.info("📤 Validation result | kata={} passed={} score={}/100 xp={}",
                kataId, passed, score, xpEarned);

        } catch (Exception e) {
            log.error("❌ Validation error | kata={} hero={}", kataId, heroId, e);
            publishFailure(heroId, kataId, 0, "Validation error: " + e.getMessage());
        } finally {
            cleanup(tempDir);
        }
    }

    private void publishFailure(String heroId, String kataId, int score, String feedback) {
        try {
            rabbitTemplate.convertAndSend("kata.validation.results",
                new KataValidationResultMessage(
                    heroId, kataId, false, score, 0, null, feedback, System.currentTimeMillis()));
        } catch (Exception e) {
            log.error("❌ Failed to publish failure result", e);
        }
    }

    // Parse "Tests run: 4, Failures: 2, Errors: 1" → proportional score
    private int parseScore(String output) {
        Pattern p = Pattern.compile("Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+)");
        int totalRun = 0, totalFail = 0;
        Matcher m = p.matcher(output);
        while (m.find()) {
            totalRun  += Integer.parseInt(m.group(1));
            totalFail += Integer.parseInt(m.group(2)) + Integer.parseInt(m.group(3));
        }
        if (totalRun == 0) return 0;
        return (Math.max(0, totalRun - totalFail) * 100) / totalRun;
    }

    private String extractFeedback(String output, boolean passed) {
        if (passed) return "All tests passed ✅";
        StringBuilder sb = new StringBuilder();
        for (String line : output.split("\n")) {
            if (line.contains("FAILED") || line.contains("ERROR") || line.contains("Tests run:")) {
                sb.append(line.trim()).append("\n");
            }
        }
        return sb.length() > 0 ? sb.toString().trim() : "Tests failed — run 'mvn verify' locally for details";
    }

    private String mvnExecutable() {
        return System.getProperty("os.name", "").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
    }

    private int runProcess(Path workDir, String... command) throws IOException, InterruptedException {
        return new ProcessBuilder(command)
            .directory(workDir.toFile())
            .inheritIO()
            .start()
            .waitFor();
    }

    private int runProcessWithOutput(Path workDir, StringBuilder output, String... command)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
            .directory(workDir.toFile())
            .redirectErrorStream(true)
            .start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.debug("[kata-validate] {}", line);
            }
        }
        return process.waitFor();
    }

    private void cleanup(Path dir) {
        if (dir == null || !Files.exists(dir)) return;
        try {
            Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
            log.debug("🧹 Cleaned up temp dir: {}", dir);
        } catch (IOException e) {
            log.warn("⚠️ Failed to cleanup temp dir: {}", dir);
        }
    }
}
