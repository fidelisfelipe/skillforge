package com.skillforge.dojo.message;

/**
 * Response: Hub envia resultado de validação
 *
 * Publicado em: kata.validation.results
 * Consumido por: Hero (DojoMQConsumer)
 */
public record KataValidationResultMessage(
    String heroId,
    String kataId,
    boolean passed,
    int score,              // 0-100
    int xpEarned,
    String skill,           // e.g., "java-21-virtual-threads" (null if failed)
    String feedback,        // Error messages or success notes
    long timestamp
) {

    public KataValidationResultMessage {
        if (heroId == null || heroId.isBlank()) {
            throw new IllegalArgumentException("heroId required");
        }
        if (kataId == null || kataId.isBlank()) {
            throw new IllegalArgumentException("kataId required");
        }
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("score must be 0-100");
        }
        if (passed && (skill == null || skill.isBlank())) {
            throw new IllegalArgumentException("skill required if passed");
        }
    }
}
