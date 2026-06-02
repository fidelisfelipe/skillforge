package com.skillforge.hub.dojo;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

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

    public void record(String kataId, String heroId, int score, int xpEarned) {
        recordAt(kataId, heroId, score, xpEarned, System.currentTimeMillis());
    }

    void recordAt(String kataId, String heroId, int score, int xpEarned, long timestamp) {
        KataSolution sol = new KataSolution(heroId, kataId, score, xpEarned, timestamp);
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

    public long getSolvedCount(List<KataCatalogLoader.KataEntry> katas) {
        return katas.stream().filter(k -> isSolved(k.id())).count();
    }

    public record HeroStats(String heroId, int katasSolved, int totalXp, String lastSeen) {}

    public List<HeroStats> getHeroStats() {
        Map<String, Set<String>> katasByHero   = new HashMap<>();
        Map<String, Integer>     xpByHero      = new HashMap<>();
        Map<String, Long>        lastTsByHero   = new HashMap<>();

        for (Map.Entry<String, List<KataSolution>> entry : byKata.entrySet()) {
            for (KataSolution sol : entry.getValue()) {
                katasByHero.computeIfAbsent(sol.heroId(), k -> new HashSet<>()).add(entry.getKey());
                xpByHero.merge(sol.heroId(), sol.xpEarned(), Integer::sum);
                lastTsByHero.merge(sol.heroId(), sol.timestamp(), Math::max);
            }
        }

        return katasByHero.entrySet().stream()
            .map(e -> new HeroStats(
                e.getKey(),
                e.getValue().size(),
                xpByHero.getOrDefault(e.getKey(), 0),
                FMT.format(Instant.ofEpochMilli(lastTsByHero.getOrDefault(e.getKey(), 0L)))
            ))
            .sorted(Comparator.comparingInt(HeroStats::katasSolved).reversed())
            .collect(Collectors.toList());
    }
}
