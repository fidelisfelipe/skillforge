package com.skillforge.hub.dojo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stub: QuestService (usado por KataMQConsumer)
 *
 * TODO: Integrar com quest system existente
 */
@Service
@Slf4j
public class QuestService {

    /**
     * Publish quest completion
     */
    public void publishQuestCompletion(String heroId, String questId) {
        log.info("🎯 [STUB] Would publish quest completion: hero={} | quest={}",
            heroId, questId);

        // TODO: Implementar publicação real de conclusão de quest
    }
}
