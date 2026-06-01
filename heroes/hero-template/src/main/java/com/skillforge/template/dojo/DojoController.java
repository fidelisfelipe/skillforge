package com.skillforge.template.dojo;

import com.skillforge.dojo.message.KataThemeRequestMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dojo Frontend Controller
 *
 * Serve a interface web e expõe endpoints para interação com Kata MVP
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class DojoController {

    private final RabbitTemplate rabbitTemplate;
    private final HeroProperties heroProperties;

    // SSE Emitters por heroId
    private static final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Serve a página HTML do Dojo
     */
    @GetMapping("/dojo")
    public String dojo(Model model) {
        String heroId = heroProperties.getId();
        log.info("🥋 Serving Dojo interface for hero: {}", heroId);
        model.addAttribute("heroId", heroId);
        return "dojo";
    }

    /**
     * SSE endpoint para notificações em tempo real
     */
    @GetMapping("/dojo/sse")
    public SseEmitter streamSse(@RequestParam String heroId) {
        log.info("📡 SSE connection opened for hero: {}", heroId);

        SseEmitter emitter = new SseEmitter(300_000L);  // 5 minutos timeout
        emitters.put(heroId, emitter);

        emitter.onCompletion(() -> {
            log.info("✅ SSE completed for hero: {}", heroId);
            emitters.remove(heroId);
        });

        emitter.onTimeout(() -> {
            log.info("⏱️ SSE timeout for hero: {}", heroId);
            emitters.remove(heroId);
        });

        return emitter;
    }

    /**
     * POST /dojo/api/request-themes
     * Hero pede lista de temas
     */
    @PostMapping("/dojo/api/request-themes")
    @ResponseBody
    public ResponseEntity<Map<String, String>> requestThemes() {
        String heroId = heroProperties.getId();
        log.info("📋 Hero {} requesting themes", heroId);

        KataThemeRequestMessage msg = new KataThemeRequestMessage(
            heroId,
            "LIST_THEMES",
            null,
            System.currentTimeMillis()
        );

        rabbitTemplate.convertAndSend("kata.requests", msg);
        log.info("📤 Theme request published to RabbitMQ");

        return ResponseEntity.ok(Map.of("status", "ok", "message", "Request sent to hub"));
    }

    /**
     * POST /dojo/api/choose-theme
     * Hero escolhe um tema
     */
    @PostMapping("/dojo/api/choose-theme")
    @ResponseBody
    public ResponseEntity<Map<String, String>> chooseTheme(@RequestBody Map<String, String> request) {
        String heroId = heroProperties.getId();
        String themeId = request.get("themeId");
        log.info("🎯 Hero {} choosing theme: {}", heroId, themeId);

        KataThemeRequestMessage msg = new KataThemeRequestMessage(
            heroId,
            "CHOOSE_THEME",
            themeId,
            System.currentTimeMillis()
        );

        rabbitTemplate.convertAndSend("kata.requests", msg);
        log.info("📤 Theme choice published to RabbitMQ");

        return ResponseEntity.ok(Map.of("status", "ok", "message", "Theme selected"));
    }

    /**
     * POST /dojo/api/request-different
     * Hero pede outra kata do mesmo tema
     */
    @PostMapping("/dojo/api/request-different")
    @ResponseBody
    public ResponseEntity<Map<String, String>> requestDifferent(@RequestBody Map<String, String> request) {
        String heroId = heroProperties.getId();
        String themeId = request.get("themeId");
        log.info("🔄 Hero {} requesting different kata from theme: {}", heroId, themeId);

        KataThemeRequestMessage msg = new KataThemeRequestMessage(
            heroId,
            "REQUEST_DIFFERENT",
            themeId,
            System.currentTimeMillis()
        );

        rabbitTemplate.convertAndSend("kata.requests", msg);
        log.info("📤 Different kata request published to RabbitMQ");

        return ResponseEntity.ok(Map.of("status", "ok", "message", "Requesting different kata"));
    }

    /**
     * Enviar notificação SSE para hero específico
     * Usado internamente por DojoMQConsumer
     */
    public static void sendSSEToHero(String heroId, String eventName, Object data) throws IOException {
        SseEmitter emitter = emitters.get(heroId);
        if (emitter == null) {
            log.warn("⚠️ No SSE emitter for hero: {}", heroId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                .id(eventName.toLowerCase())
                .name(eventName)
                .data(data)
                .build());
            log.debug("✅ SSE sent to hero {}: {}", heroId, eventName);
        } catch (IOException e) {
            log.error("❌ Failed to send SSE to hero {}: {}", heroId, e.getMessage());
            emitters.remove(heroId);
            throw e;
        }
    }
}
