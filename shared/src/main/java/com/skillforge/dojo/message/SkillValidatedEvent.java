package com.skillforge.dojo.message;

/**
 * Broadcast: Hub notifica guilda inteira quando skill é validada
 *
 * Publicado em: skill.events (topic exchange)
 * Consumido por: Todos os heróis (via SSE broadcast)
 */
public record SkillValidatedEvent(
    String heroId,
    String kataId,
    String skill,
    int xpEarned,
    long timestamp
) {

    public SkillValidatedEvent {
        if (heroId == null || heroId.isBlank()) {
            throw new IllegalArgumentException("heroId required");
        }
        if (skill == null || skill.isBlank()) {
            throw new IllegalArgumentException("skill required");
        }
    }
}
