package com.skillforge.template.dojo;

import com.skillforge.dojo.message.KataThemeResponseMessage.KataTheme;
import com.skillforge.dojo.message.KataThemeResponseMessage.KataDelivery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache local para dados de Kata
 *
 * Armazena:
 * - Temas disponíveis
 * - Kata atual aceito
 * - Katas completados
 */
@Service
@Slf4j
public class DojoCacheService {

    private final java.util.Map<String, Object> cache = new ConcurrentHashMap<>();

    /**
     * Salvar temas disponíveis em cache
     */
    public void setAvailableThemes(List<KataTheme> themes) {
        cache.put("available_themes", themes);
        log.debug("💾 Cached {} themes", themes.size());
    }

    /**
     * Get temas em cache
     */
    @SuppressWarnings("unchecked")
    public List<KataTheme> getAvailableThemes() {
        return (List<KataTheme>) cache.get("available_themes");
    }

    /**
     * Salvar kata atual em cache
     */
    public void setCurrentKata(KataDelivery kata) {
        cache.put("current_kata", kata);
        log.debug("💾 Cached current kata: {}", kata.kataId());
    }

    /**
     * Get kata atual
     */
    public KataDelivery getCurrentKata() {
        return (KataDelivery) cache.get("current_kata");
    }

    /**
     * Marcar kata como completado
     */
    public void markKataAsCompleted(String kataId) {
        cache.put("completed_" + kataId, true);
        log.debug("💾 Marked kata as completed: {}", kataId);
    }

    /**
     * Check se kata foi completado
     */
    public boolean isKataCompleted(String kataId) {
        return cache.containsKey("completed_" + kataId);
    }

    /**
     * Limpar cache
     */
    public void clear() {
        cache.clear();
        log.debug("💾 Cache cleared");
    }
}
