package com.skillforge.hub.dojo;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class KataProgressService {

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault());

    public record KataSolution(
        String heroId,
        String kataId,
        int score,
        int xpEarned,
        long timestamp
    ) {
        public String formattedTime() { return FMT.format(Instant.ofEpochMilli(timestamp)); }
    }

    // kataId → solutions (passed only)
    private final Map<String, List<KataSolution>> byKata = new ConcurrentHashMap<>();

    // chronological feed of all solutions
    private final List<KataSolution> recentFeed = new CopyOnWriteArrayList<>();

    // KATA-001A foi resolvido pelo fidelisdev — semeado porque o store é in-memory
    @PostConstruct
    void seedKnownSolutions() {
        record("KATA-001A", "fidelisdev", 100, 80);
    }

    public void record(String kataId, String heroId, int score, int xpEarned) {
        KataSolution sol = new KataSolution(heroId, kataId, score, xpEarned, System.currentTimeMillis());
        byKata.computeIfAbsent(kataId, k -> new CopyOnWriteArrayList<>()).add(sol);
        recentFeed.add(0, sol);
        if (recentFeed.size() > 50) recentFeed.removeLast();
    }

    public List<KataSolution> getSolutionsFor(String kataId) {
        return byKata.getOrDefault(kataId, List.of());
    }

    public boolean isSolved(String kataId) {
        return byKata.containsKey(kataId) && !byKata.get(kataId).isEmpty();
    }

    public List<KataSolution> getRecentFeed() {
        return Collections.unmodifiableList(recentFeed);
    }

    public int getTotalSolutions() {
        return byKata.values().stream().mapToInt(List::size).sum();
    }

    public int getSolvedKataCount() {
        return (int) byKata.values().stream().filter(l -> !l.isEmpty()).count();
    }
}
