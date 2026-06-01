package com.skillforge.hub.dojo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stub: SkillGraphService (usado por KataMQConsumer)
 *
 * TODO: Integrar com skill graph existente
 */
@Service
@Slf4j
public class SkillGraphService {

    /**
     * Validate or update skill graph
     */
    public void publishSkillValidated(String heroId, String skill, int xpEarned) {
        log.info("📊 [STUB] Would publish skill validation: hero={} | skill={} | xp={}",
            heroId, skill, xpEarned);

        // TODO: Integrar com skill graph existente
        // - Update hero skill node
        // - Unlock next skills
        // - Publish event para SSE broadcast
    }
}
