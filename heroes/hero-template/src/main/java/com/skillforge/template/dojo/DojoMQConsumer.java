package com.skillforge.template.dojo;

import com.skillforge.dojo.message.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * Hero Consumer: Consome respostas de Kata da fila
 *
 * Responsabilidades:
 * 1. Receber temas disponíveis do hub
 * 2. Receber kata entregues
 * 3. Receber resultados de validação
 * 4. Atualizar cache local
 * 5. Notificar frontend via SSE
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DojoMQConsumer {

    private final DojoCacheService cacheService;
    private final KataLocalService kataLocalService;
    private final HeroProperties heroProperties;

    /**
     * Consome respostas de tema/kata
     */
    @RabbitListener(queues = "kata.responses")
    public void handleKataResponse(KataThemeResponseMessage message) {

        String heroId = heroProperties.getId();

        // Validar que é para este hero
        if (!message.heroId().equals(heroId)) {
            log.debug("⏭️ Ignoring message for different hero: {}", message.heroId());
            return;
        }

        log.info("📨 Received kata response | Status: {}", message.status());

        try {
            switch (message.status()) {

                case "THEMES_LISTED" -> handleThemesListed(message);

                case "KATA_DELIVERED" -> handleKataDelivered(message);

                case "ERROR" -> handleError(message);

                default -> log.warn("⚠️ Unknown response status: {}", message.status());
            }

        } catch (IOException e) {
            log.error("❌ Error sending SSE notification", e);
        }
    }

    /**
     * Recebeu lista de temas disponíveis
     */
    private void handleThemesListed(KataThemeResponseMessage message)
        throws IOException {

        // 1. Salvar em cache
        cacheService.setAvailableThemes(message.themes());
        log.debug("💾 Cached {} available themes", message.themes().size());

        // 2. Notificar frontend via SSE
        DojoController.sendSSEToHero(message.heroId(), "THEMES_LOADED", Map.of(
            "status", "success",
            "themesCount", message.themes().size(),
            "themes", message.themes()
        ));

        log.info("✅ Themes list sent to frontend");
    }

    /**
     * Recebeu um kata entregue pelo hub
     */
    private void handleKataDelivered(KataThemeResponseMessage message)
        throws IOException {

        KataThemeResponseMessage.KataDelivery kata = message.currentKata();

        // 1. Salvar em cache
        cacheService.setCurrentKata(kata);
        log.debug("💾 Cached current kata: {}", kata.kataId());

        // 2. Salvar arquivo local marcando kata aceito
        try {
            kataLocalService.saveAcceptedKata(kata);
        } catch (Exception e) {
            log.warn("⚠️ Failed to save kata locally", e);
        }

        // 3. Notificar frontend via SSE
        DojoController.sendSSEToHero(message.heroId(), "KATA_DELIVERED", Map.of(
            "status", "success",
            "kataId", kata.kataId(),
            "themeId", kata.themeId(),
            "title", kata.title(),
            "xpReward", kata.xpReward(),
            "difficulty", kata.difficulty(),
            "spec", kata.spec(),
            "solutionTemplatePath", kata.solutionTemplatePath()
        ));

        log.info("✅ Kata delivered: {}", kata.kataId());
    }

    /**
     * Recebeu erro do hub
     */
    private void handleError(KataThemeResponseMessage message)
        throws IOException {

        String errorMsg = message.errorMessage() != null
            ? message.errorMessage()
            : "Unknown error";

        log.warn("⚠️ Hub error: {}", errorMsg);

        DojoController.sendSSEToHero(message.heroId(), "ERROR", Map.of(
            "status", "error",
            "message", errorMsg
        ));
    }

    /**
     * Consome resultados de validação de kata
     */
    @RabbitListener(queues = "kata.validation.results")
    public void handleValidationResult(KataValidationResultMessage message)
        throws IOException {

        String heroId = heroProperties.getId();

        if (!message.heroId().equals(heroId)) {
            log.debug("⏭️ Ignoring validation result for different hero");
            return;
        }

        log.info("📨 Kata validation result received | Kata: {} | Passed: {}",
            message.kataId(), message.passed());

        if (message.passed()) {
            handleValidationPassed(message);
        } else {
            handleValidationFailed(message);
        }
    }

    /**
     * Kata passou na validação
     */
    private void handleValidationPassed(KataValidationResultMessage message)
        throws IOException {

        // 1. Atualizar cache
        cacheService.markKataAsCompleted(message.kataId());
        log.debug("💾 Marked kata as completed: {}", message.kataId());

        // 2. Notificar frontend
        DojoController.sendSSEToHero(message.heroId(), "KATA_VALIDATED", Map.of(
            "status", "success",
            "kataId", message.kataId(),
            "skill", message.skill(),
            "score", message.score(),
            "xpEarned", message.xpEarned(),
            "message", "🎉 Kata validated! Skill unlocked: " + message.skill()
        ));

        log.info("✅ KATA VALIDATED: {} | Score: {} | XP: {} | Skill: {}",
            message.kataId(), message.score(), message.xpEarned(), message.skill());
    }

    /**
     * Kata falhou na validação
     */
    private void handleValidationFailed(KataValidationResultMessage message)
        throws IOException {

        log.warn("❌ KATA VALIDATION FAILED: {} | Score: {}/100",
            message.kataId(), message.score());

        // Notificar frontend com feedback
        DojoController.sendSSEToHero(message.heroId(), "KATA_VALIDATION_FAILED", Map.of(
            "status", "failed",
            "kataId", message.kataId(),
            "score", message.score(),
            "feedback", message.feedback(),
            "message", "❌ Kata validation failed. Check feedback and try again."
        ));
    }
}
