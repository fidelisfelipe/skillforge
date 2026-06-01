package com.skillforge.hub.dojo;

import com.skillforge.dojo.message.KataThemeResponseMessage.KataDelivery;
import com.skillforge.dojo.message.KataThemeResponseMessage.KataTheme;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class KataService {

    private static final String REPO_BASE = "https://github.com/fidelisfelipe/skillforge-katas/tree/";

    private final KataCatalogLoader catalogLoader;

    private final Map<String, KataTheme>    themes   = new LinkedHashMap<>();
    private final Map<String, KataDelivery> katas    = new LinkedHashMap<>();
    private final Map<String, Map<String, Integer>> heroProgress = new ConcurrentHashMap<>();

    @PostConstruct
    void buildFromCatalog() {
        for (var theme : catalogLoader.getThemes()) {
            List<String> kataIds = theme.katas().stream()
                .map(KataCatalogLoader.KataEntry::id)
                .toList();

            themes.put(theme.id(), new KataTheme(
                theme.id(), theme.name(), theme.description(),
                theme.difficulty(), theme.certReference(), kataIds
            ));

            for (var k : theme.katas()) {
                katas.put(k.id(), new KataDelivery(
                    k.id(), theme.id(), k.title(),
                    k.spec() != null ? k.spec().strip() : "",
                    REPO_BASE + k.templateBranch(),
                    k.xpReward(), k.difficulty(),
                    System.currentTimeMillis()
                ));
            }
        }
        log.info("✅ KataService ready: {} themes, {} katas", themes.size(), katas.size());
    }

    public List<KataTheme> getAvailableThemes(String heroId) {
        return new ArrayList<>(themes.values());
    }

    public KataDelivery getKata(String kataId) {
        return katas.get(kataId);
    }

    public List<KataDelivery> getAllKatas() {
        return new ArrayList<>(katas.values());
    }

    public List<KataCatalogLoader.ThemeEntry> getThemeEntries() {
        return catalogLoader.getThemes();
    }

    public int getKataCount() {
        return katas.size();
    }

    public boolean themeExists(String themeId) {
        return themeId != null && themes.containsKey(themeId);
    }

    public KataDelivery assignKata(String heroId, String themeId) {
        return assignKata(heroId, themeId, false);
    }

    public KataDelivery assignKata(String heroId, String themeId, boolean assignNext) {
        if (themeId == null) {
            log.warn("assignKata called with null themeId for hero: {}", heroId);
            return null;
        }
        KataTheme theme = themes.get(themeId);
        if (theme == null) return null;

        List<String> kataIds = theme.kataIds();
        Map<String, Integer> progress = heroProgress.computeIfAbsent(heroId, k -> new ConcurrentHashMap<>());

        int index = progress.getOrDefault(themeId, 0);
        if (assignNext) {
            index = (index + 1) % kataIds.size();
            progress.put(themeId, index);
        }

        String kataId = kataIds.get(index);
        KataDelivery kata = katas.get(kataId);

        if (kata == null) {
            log.warn("Kata not found in catalog: {}", kataId);
            return null;
        }

        log.info("Assigned kata {} (index={}) to hero {} from theme {}", kataId, index, heroId, themeId);
        return kata;
    }
}
