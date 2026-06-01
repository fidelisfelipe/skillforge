package com.skillforge.hero.domain;

import java.util.List;

public record HeroManifest(
        String heroId,
        String heroName,
        String heroClass,
        List<String> skills,
        String endpoint,
        String model,
        String specialty,
        int level,
        int xp
) {
    public HeroManifest {
        if (heroId == null || heroId.isBlank()) throw new IllegalArgumentException("heroId required");
        if (skills == null) skills = List.of();
        if (model == null || model.isBlank()) model = "phi3:mini";
        if (level <= 0) level = 1;
    }

    public HeroLevel heroLevel() {
        return HeroLevel.fromLevel(level);
    }
}