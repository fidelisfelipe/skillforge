package com.skillforge.hub.dojo;

import com.skillforge.dojo.message.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Hub Consumer: Consome mensagens de requisição de Kata da fila
 *
 * Responsabilidades:
 * 1. Listar temas disponíveis
 * 2. Atribuir kata baseado em tema
 * 3. Publicar respostas na fila kata.responses
 * 4. Criar issues no GitHub para rastreamento
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KataMQConsumer {

    private final KataService kataService;
    private final HeroService heroService;
    private final SkillGraphService skillGraphService;
    private final GitHubService githubService;
    private final QuestService questService;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Consome requisições de tema/kata da fila
     */
    @RabbitListener(queues = "kata.requests")
    public void handleKataRequest(KataThemeRequestMessage message) {

        String heroId = message.heroId();
        long startTime = System.currentTimeMillis();

        log.info("🎯 Processing kata request from hero: {} | Type: {}",
            heroId, message.requestType());

        try {
            switch (message.requestType()) {

                case "LIST_THEMES" -> handleListThemes(heroId);

                case "CHOOSE_THEME" -> handleChooseTheme(heroId, message.themeId());

                case "NEXT_KATA" -> handleNextKata(heroId, message.themeId());

                case "REQUEST_DIFFERENT" -> handleRequestDifferent(heroId, message.themeId());

                default -> sendError(heroId,
                    "Unknown request type: " + message.requestType());
            }

            long duration = System.currentTimeMillis() - startTime;
            log.debug("✅ Kata request processed in {}ms", duration);

        } catch (Exception e) {
            log.error("❌ Error processing kata request for hero: {}", heroId, e);
            sendError(heroId, "Internal error: " + e.getMessage());
        }
    }

    /**
     * LIST_THEMES: Retorna temas disponíveis (excluindo já completados)
     */
    private void handleListThemes(String heroId) {
        List<KataThemeResponseMessage.KataTheme> themes =
            kataService.getAvailableThemes(heroId);

        KataThemeResponseMessage response = new KataThemeResponseMessage(
            heroId,
            themes,
            null,
            "THEMES_LISTED",
            null,
            System.currentTimeMillis()
        );

        publishResponse(response);
        log.info("📋 Listed {} themes for hero: {}", themes.size(), heroId);
    }

    /**
     * CHOOSE_THEME: Atribui um kata do tema escolhido
     */
    private void handleChooseTheme(String heroId, String themeId) {

        // 1. Validar hero existe
        if (!heroService.exists(heroId)) {
            sendError(heroId, "Hero not found: " + heroId);
            return;
        }

        // 2. Validar tema existe
        if (!kataService.themeExists(themeId)) {
            sendError(heroId, "Theme not found: " + themeId);
            return;
        }

        // 3. Atribuir kata do tema
        KataThemeResponseMessage.KataDelivery kata =
            kataService.assignKata(heroId, themeId);

        if (kata == null) {
            sendError(heroId, "No kata available for theme: " + themeId);
            return;
        }

        // 4. Criar issue no GitHub para rastreamento
        try {
            githubService.createKataIssue(heroId, kata);
        } catch (Exception e) {
            log.warn("⚠️ Failed to create GitHub issue for kata: {}", kata.kataId(), e);
            // Continua mesmo se falhar (não bloqueia fluxo)
        }

        // 5. Publicar resposta
        KataThemeResponseMessage response = new KataThemeResponseMessage(
            heroId,
            null,
            kata,
            "KATA_DELIVERED",
            null,
            System.currentTimeMillis()
        );

        publishResponse(response);
        log.info("🎯 Assigned kata {} from theme {} to hero: {}",
            kata.kataId(), themeId, heroId);
    }

    /**
     * NEXT_KATA: Avança para próximo kata do tema (sem completar o atual)
     */
    private void handleNextKata(String heroId, String themeId) {

        KataThemeResponseMessage.KataDelivery kata =
            kataService.assignKata(heroId, themeId, /* assignNext= */ true);

        if (kata == null) {
            sendError(heroId, "No more kata available for theme: " + themeId);
            return;
        }

        // Criar issue
        try {
            githubService.createKataIssue(heroId, kata);
        } catch (Exception e) {
            log.warn("⚠️ Failed to create GitHub issue", e);
        }

        KataThemeResponseMessage response = new KataThemeResponseMessage(
            heroId,
            null,
            kata,
            "KATA_DELIVERED",
            null,
            System.currentTimeMillis()
        );

        publishResponse(response);
        log.info("⏭️  Assigned next kata {} from theme {} to hero: {}",
            kata.kataId(), themeId, heroId);
    }

    /**
     * REQUEST_DIFFERENT: Hero não quer este kata, quer outro do mesmo tema
     */
    private void handleRequestDifferent(String heroId, String themeId) {
        // Mesmo que NEXT_KATA
        handleNextKata(heroId, themeId);
    }

    /**
     * Publica resposta na fila para hero receber
     */
    private void publishResponse(KataThemeResponseMessage response) {
        rabbitTemplate.convertAndSend("kata.responses", response);
        log.debug("📤 Response published for hero: {}", response.heroId());
    }

    /**
     * Publica erro na fila
     */
    private void sendError(String heroId, String errorMessage) {
        KataThemeResponseMessage error = new KataThemeResponseMessage(
            heroId,
            null,
            null,
            "ERROR",
            errorMessage,
            System.currentTimeMillis()
        );

        publishResponse(error);
        log.warn("⚠️ Error sent to hero {}: {}", heroId, errorMessage);
    }
}
