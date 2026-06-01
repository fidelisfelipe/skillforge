package com.skillforge.template.dojo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = com.skillforge.hero.HeroApplication.class)
@TestPropertySource(properties = {
    "skillforge.hero.id=test-hero",
    "skillforge.hero.name=Test Hero",
    "skillforge.hero.local-storage.base-path=/tmp/test-dojo"
})
@DisplayName("HeroProperties Tests")
class HeroPropertiesTest {

    @Autowired
    private HeroProperties heroProperties;

    @Test
    @DisplayName("Should read hero ID from properties")
    void testHeroId() {
        assertEquals("test-hero", heroProperties.getId());
    }

    @Test
    @DisplayName("Should read hero name from properties")
    void testHeroName() {
        assertEquals("Test Hero", heroProperties.getName());
    }

    @Test
    @DisplayName("Should use custom local storage path")
    void testCustomLocalStoragePath() {
        String basePath = heroProperties.getLocalStorageBasePath();
        assertEquals("/tmp/test-dojo", basePath);
    }

    @Test
    @DisplayName("Should initialize localStorage object")
    void testLocalStorageInitialized() {
        assertNotNull(heroProperties.getLocalStorage());
        assertNotNull(heroProperties.getLocalStorage().getBasePath());
    }
}
