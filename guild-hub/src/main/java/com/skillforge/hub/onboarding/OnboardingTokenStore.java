package com.skillforge.hub.onboarding;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OnboardingTokenStore {

    private record Entry(String heroId, String githubLogin, int issueNumber, Instant expiresAt) {}

    private final ConcurrentHashMap<String, Entry> tokens = new ConcurrentHashMap<>();

    public String generate(String heroId, String githubLogin, int issueNumber) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, new Entry(heroId, githubLogin, issueNumber, Instant.now().plus(48, ChronoUnit.HOURS)));
        return token;
    }

    public boolean isValid(String token) {
        Entry entry = tokens.get(token);
        return entry != null && Instant.now().isBefore(entry.expiresAt());
    }

    public Optional<OnboardingRequest> consume(String token) {
        Entry entry = tokens.remove(token);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) return Optional.empty();
        return Optional.of(new OnboardingRequest(entry.heroId(), entry.githubLogin(), entry.issueNumber()));
    }

    @Scheduled(fixedDelay = 3_600_000)
    void evictExpired() {
        Instant now = Instant.now();
        tokens.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }

    public record OnboardingRequest(String heroId, String githubLogin, int issueNumber) {}
}
