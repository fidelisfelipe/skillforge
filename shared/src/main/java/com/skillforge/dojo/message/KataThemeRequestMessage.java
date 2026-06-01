package com.skillforge.dojo.message;

/**
 * Request: Hero pede temas ou escolhe um
 *
 * Publicado em: kata.requests
 * Consumido por: Hub (KataMQConsumer)
 */
public record KataThemeRequestMessage(
    String heroId,
    String requestType,      // LIST_THEMES, CHOOSE_THEME, NEXT_KATA, REQUEST_DIFFERENT
    String themeId,          // null if LIST_THEMES
    long timestamp
) {

    // Validation in compact constructor
    public KataThemeRequestMessage {
        if (heroId == null || heroId.isBlank()) {
            throw new IllegalArgumentException("heroId required");
        }
        if (requestType == null) {
            throw new IllegalArgumentException("requestType required");
        }
        if (timestamp <= 0) {
            throw new IllegalArgumentException("timestamp must be positive");
        }
    }
}
