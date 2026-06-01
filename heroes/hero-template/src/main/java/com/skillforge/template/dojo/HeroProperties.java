package com.skillforge.template.dojo;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Properties do Hero
 *
 * Lê de application-kata.yml:
 * skillforge.hero.id
 * skillforge.hero.name
 * skillforge.hero.local-storage.base-path
 */
@Component
@ConfigurationProperties(prefix = "skillforge.hero")
@Data
public class HeroProperties {

    private String id;
    private String name;

    private LocalStorage localStorage = new LocalStorage();

    @Data
    public static class LocalStorage {
        private String basePath;
        private String dojoPath = "dojo/";
    }

    public String getLocalStorageBasePath() {
        return localStorage.getBasePath() != null
            ? localStorage.getBasePath()
            : System.getProperty("user.home") + "/.skillforge/heroes/" + id;
    }
}
