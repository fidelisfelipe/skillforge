package com.skillforge.hub.dojo;

import com.skillforge.dojo.message.KataThemeResponseMessage.KataDelivery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stub: GitHubService (usado por KataMQConsumer)
 *
 * TODO Phase 5.4: Implementar integração real com GitHub API
 */
@Service
@Slf4j
public class GitHubService {

    /**
     * Create issue no GitHub para rastreamento de kata
     * Stub: apenas log para MVP
     */
    public void createKataIssue(String heroId, KataDelivery kata) {
        log.info("📝 [STUB] Would create GitHub issue for hero {} | kata: {}",
            heroId, kata.kataId());

        // TODO: Implementar criação real de issue
        // - Usar GitHub API
        // - Criar issue com template
        // - Adicionar labels (kata:{kataId}, hero:{heroId})
        // - Adicionar descrição com spec
    }
}
