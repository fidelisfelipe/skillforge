package com.skillforge.hub.dojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Loads kata catalog from kata-catalog.yml at startup.
 */
@Component
@Slf4j
public class KataCatalogLoader {

    private List<ThemeEntry> themes = List.of();

    @PostConstruct
    void load() {
        try {
            var mapper = new ObjectMapper(new YAMLFactory());
            mapper.findAndRegisterModules();
            var resource = new ClassPathResource("kata-catalog.yml");
            var catalog = mapper.readValue(resource.getInputStream(), Catalog.class);
            this.themes = catalog.themes() != null ? catalog.themes() : List.of();
            log.info("📚 Kata catalog loaded: {} themes, {} katas",
                themes.size(),
                themes.stream().mapToInt(t -> t.katas().size()).sum());
        } catch (Exception e) {
            log.error("❌ Failed to load kata-catalog.yml — falling back to empty catalog", e);
        }
    }

    public List<ThemeEntry> getThemes() {
        return themes;
    }

    // --- YAML model ---

    public record Catalog(List<ThemeEntry> themes) {}

    public record ThemeEntry(
        String id,
        String name,
        String description,
        String difficulty,
        String certReference,
        List<KataEntry> katas
    ) {}

    public record KataEntry(
        String id,
        String title,
        String difficulty,
        int xpReward,
        String templateBranch,
        String className,
        String spec
    ) {}
}
