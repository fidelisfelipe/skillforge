package com.skillforge.dojo.message;

import java.util.List;

/**
 * Response: Hub envia temas ou kata
 *
 * Publicado em: kata.responses
 * Consumido por: Hero (DojoMQConsumer)
 */
public record KataThemeResponseMessage(
    String heroId,
    List<KataTheme> themes,
    KataDelivery currentKata,
    String status,           // THEMES_LISTED, KATA_DELIVERED, ERROR
    String errorMessage,     // null if success
    long timestamp
) {

    public record KataTheme(
        String id,
        String name,
        String description,
        String difficulty,          // beginner, intermediate, advanced
        String certReference,       // e.g., "Oracle Java 21 - Module 7"
        List<String> kataIds
    ) {}

    public record KataDelivery(
        String kataId,
        String themeId,
        String title,
        String spec,                // Problem statement (markdown)
        String solutionTemplatePath,// Where to clone template
        int xpReward,
        String difficulty,
        long timestamp
    ) {}
}
