package com.skillforge.hub.dojo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stub: HeroService (usado por KataMQConsumer)
 *
 * TODO Phase 5.1: Implementar validação real contra banco de heróis
 */
@Service
@Slf4j
public class HeroService {

    /**
     * Check if hero exists (stub: sempre true para MVP)
     */
    public boolean exists(String heroId) {
        if (heroId == null || heroId.isBlank()) {
            return false;
        }
        // TODO: Verificar contra banco de heróis
        log.debug("Hero exists check: {} (stub mode)", heroId);
        return true;
    }
}
