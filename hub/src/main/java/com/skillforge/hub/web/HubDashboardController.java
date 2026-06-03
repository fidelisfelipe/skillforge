package com.skillforge.hub.web;

import com.skillforge.hub.GuildDashboardProperties;
import com.skillforge.hub.dojo.GitHubForksService;
import com.skillforge.hub.dojo.KataProgressService;
import com.skillforge.hub.dojo.KataService;
import com.skillforge.hub.service.HeroPresenceService;
import com.skillforge.hub.service.HeroRegistryService;
import com.skillforge.hub.service.QuestBoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Controller
public class HubDashboardController {

    private final HeroRegistryService registry;
    private final QuestBoardService questBoard;
    private final KataService kataService;
    private final KataProgressService kataProgress;
    private final GitHubForksService forksService;
    private final GuildDashboardProperties dashboardProps;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // @Lazy quebra a dependência circular: HeroPresenceService → HubDashboardController
    @Autowired @Lazy
    private HeroPresenceService presence;

    public HubDashboardController(HeroRegistryService registry, QuestBoardService questBoard,
                                  KataService kataService, KataProgressService kataProgress,
                                  GitHubForksService forksService,
                                  GuildDashboardProperties dashboardProps) {
        this.registry = registry;
        this.questBoard = questBoard;
        this.kataService = kataService;
        this.kataProgress = kataProgress;
        this.forksService = forksService;
        this.dashboardProps = dashboardProps;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        populateModel(model);
        return "dashboard-v2";
    }

    private void populateModel(Model model) {
        model.addAttribute("heroCount",       registry.getHeroCount());
        model.addAttribute("totalXp",         registry.getTotalXp());
        model.addAttribute("openQuests",      questBoard.getOpenCount());
        model.addAttribute("completedQuests", questBoard.getCompletedCount());
        model.addAttribute("leaderboard",     registry.getLeaderboard());
        model.addAttribute("skillDist",       registry.getSkillDistribution());
        model.addAttribute("questsByRarity",  questBoard.getCountByRarity());
        model.addAttribute("lastFetchMs",     registry.getLastFetchMs());
        model.addAttribute("onlineHeroes",    presence != null ? presence.getOnlineHeroes() : Set.of());
        var themes = kataService.getThemeEntries();
        model.addAttribute("kataThemes",         themes);
        model.addAttribute("kataSolutions",       kataProgress);
        model.addAttribute("kataCount",           kataService.getKataCount());
        model.addAttribute("kataSolvedCount",     kataProgress.getSolvedKataCount());
        model.addAttribute("kataChapterCount",    themes.size());
        model.addAttribute("kataActiveChapters",  themes.stream().filter(t -> !t.katas().isEmpty()).count());
        var forksByLogin = forksService.getForks().stream()
            .collect(Collectors.toMap(
                f -> f.login().toLowerCase(), f -> f, (a, b) -> a));

        var statsMap    = kataProgress.getHeroStats().stream()
            .collect(Collectors.toMap(KataProgressService.HeroStats::heroId, s -> s));
        var onlineSet   = presence != null ? presence.getOnlineHeroes() : Set.<String>of();
        var lastSeenMap = presence != null ? presence.getLastSeen()     : Map.<String, Instant>of();

        // HeroStatusView por hero registrado
        var allHeroViews = registry.getHeroes().stream()
            .filter(h -> dashboardProps.isShowGenericHeroes() || !isGenericHero(h.heroId(), h.githubLogin()))
            .map(h -> {
                String login = h.githubLogin() != null ? h.githubLogin().toLowerCase() : "";
                return new HeroStatusView(
                    h.heroId(), h.heroName(), h.avatarUrl(), h.githubLogin(),
                    onlineSet.contains(h.heroId()),
                    lastSeenLabel(lastSeenMap.get(h.heroId()), onlineSet.contains(h.heroId())),
                    forksByLogin.containsKey(login),
                    Optional.ofNullable(statsMap.get(h.heroId()))
                        .map(KataProgressService.HeroStats::katasSolved).orElse(0),
                    h.xp()
                );
            }).toList();

        // Agrupa por dev (githubLogin); heroes sem login ficam sozinhos
        var devViews = allHeroViews.stream()
            .collect(Collectors.groupingBy(
                h -> h.githubLogin() != null && !h.githubLogin().isBlank()
                    ? h.githubLogin().toLowerCase()
                    : "__" + h.heroId()
            ))
            .entrySet().stream()
            .map(e -> {
                String key   = e.getKey();
                String login = key.startsWith("__") ? "" : key;
                var fork     = forksByLogin.get(login);
                String avatarUrl = fork != null ? fork.avatarUrl()
                    : e.getValue().stream().map(HeroStatusView::avatarUrl)
                        .filter(a -> a != null && !a.isBlank()).findFirst().orElse("");
                String profileUrl  = fork != null ? fork.profileUrl() : "";
                String displayName = !login.isBlank()
                    ? e.getValue().get(0).githubLogin()
                    : e.getValue().get(0).heroId();
                var heroes = e.getValue().stream()
                    .sorted(Comparator.comparing(HeroStatusView::online).reversed()
                        .thenComparingInt(HeroStatusView::katasSolved).reversed())
                    .toList();
                return new DevView(displayName, avatarUrl, profileUrl, fork != null, heroes);
            })
            .sorted(Comparator.comparing(DevView::anyOnline).reversed()
                .thenComparingInt(DevView::totalKatas).reversed())
            .toList();

        model.addAttribute("devViews",  devViews);
        model.addAttribute("forkCount", forksService.getForkCount());
    }

    @GetMapping(value = "/events", produces = "text/event-stream")
    public SseEmitter events() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));

        Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("GUILD_STATE")
                        .data("{\"heroes\":%d,\"openQuests\":%d,\"completedQuests\":%d,\"totalXp\":%d}"
                                .formatted(registry.getHeroCount(),
                                        questBoard.getOpenCount(),
                                        questBoard.getCompletedCount(),
                                        registry.getTotalXp())));
            } catch (IOException ignored) {}
        });

        return emitter;
    }

    public record HeroStatusView(
        String heroId,
        String heroName,
        String avatarUrl,
        String githubLogin,
        boolean online,
        String lastSeenLabel,
        boolean hasFork,
        int katasSolved,
        int xp
    ) {}

    public record DevView(
        String githubLogin,
        String avatarUrl,
        String profileUrl,
        boolean hasFork,
        List<HeroStatusView> heroes
    ) {
        public boolean anyOnline() { return heroes.stream().anyMatch(HeroStatusView::online); }
        public int totalKatas()    { return heroes.stream().mapToInt(HeroStatusView::katasSolved).sum(); }
        public int totalXp()       { return heroes.stream().mapToInt(HeroStatusView::xp).sum(); }
    }

    private static boolean isGenericHero(String heroId, String githubLogin) {
        return githubLogin == null || githubLogin.isBlank()
            || githubLogin.equalsIgnoreCase(heroId);
    }

    private static String lastSeenLabel(Instant ts, boolean online) {
        if (online) return "online";
        if (ts == null) return "nunca visto";
        long secs = Duration.between(ts, Instant.now()).getSeconds();
        if (secs < 60)    return "há " + secs + "s";
        if (secs < 3600)  return "há " + (secs / 60) + "min";
        if (secs < 86400) return "há " + (secs / 3600) + "h";
        return "há " + (secs / 86400) + "d";
    }

    public void broadcast(String eventName, String data) {
        List.copyOf(emitters).forEach(e -> {
            try {
                e.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception ex) {
                emitters.remove(e);
            }
        });
    }
}