package com.skillforge.dojo.message;

/**
 * Request: Hero submete kata para validação
 *
 * Publicado em: kata.submission
 * Consumido por: Hub (KataSubmissionConsumer) - TODO Phase 5.4
 */
public record KataSubmissionMessage(
    String heroId,
    String kataId,
    String gitBranch,       // e.g., "feat/kata-001-virtual-threads"
    String commitSha,       // Latest commit
    long timestamp
) {

    public KataSubmissionMessage {
        if (heroId == null || heroId.isBlank()) {
            throw new IllegalArgumentException("heroId required");
        }
        if (kataId == null || kataId.isBlank()) {
            throw new IllegalArgumentException("kataId required");
        }
        if (gitBranch == null || gitBranch.isBlank()) {
            throw new IllegalArgumentException("gitBranch required");
        }
        if (commitSha == null || commitSha.isBlank()) {
            throw new IllegalArgumentException("commitSha required");
        }
    }
}
