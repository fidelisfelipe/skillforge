package com.skillforge.template.dojo;

import com.skillforge.dojo.message.KataThemeResponseMessage.KataTheme;
import com.skillforge.dojo.message.KataThemeResponseMessage.KataDelivery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = com.skillforge.hero.HeroApplication.class)
@DisplayName("DojoCacheService Tests")
class DojoCacheServiceTest {

    @Autowired
    private DojoCacheService cacheService;

    private List<KataTheme> mockThemes;
    private KataDelivery mockKata;

    @BeforeEach
    void setUp() {
        cacheService.clear();

        mockThemes = List.of(
            new KataTheme("virtual-threads", "Virtual Threads", "Desc", "intermediate", "Ref", List.of("KATA-001")),
            new KataTheme("records", "Records", "Desc", "beginner", "Ref", List.of("KATA-002"))
        );

        mockKata = new KataDelivery(
            "KATA-001",
            "virtual-threads",
            "Virtual Threads Kata",
            "# Problem...",
            "https://github.com/.../template",
            100,
            "intermediate",
            System.currentTimeMillis()
        );
    }

    @Test
    @DisplayName("Should cache available themes")
    void testSetAndGetAvailableThemes() {
        cacheService.setAvailableThemes(mockThemes);

        var cached = cacheService.getAvailableThemes();
        assertNotNull(cached);
        assertEquals(2, cached.size());
        assertEquals("virtual-threads", cached.get(0).id());
    }

    @Test
    @DisplayName("Should return null for uncached themes")
    void testGetUncachedThemes() {
        var cached = cacheService.getAvailableThemes();
        assertNull(cached);
    }

    @Test
    @DisplayName("Should cache current kata")
    void testSetAndGetCurrentKata() {
        cacheService.setCurrentKata(mockKata);

        var cached = cacheService.getCurrentKata();
        assertNotNull(cached);
        assertEquals("KATA-001", cached.kataId());
        assertEquals("Virtual Threads Kata", cached.title());
    }

    @Test
    @DisplayName("Should mark kata as completed")
    void testMarkKataAsCompleted() {
        assertFalse(cacheService.isKataCompleted("KATA-001"));

        cacheService.markKataAsCompleted("KATA-001");

        assertTrue(cacheService.isKataCompleted("KATA-001"));
    }

    @Test
    @DisplayName("Should track multiple completed katas")
    void testMultipleCompletedKatas() {
        cacheService.markKataAsCompleted("KATA-001");
        cacheService.markKataAsCompleted("KATA-002");
        cacheService.markKataAsCompleted("KATA-003");

        assertTrue(cacheService.isKataCompleted("KATA-001"));
        assertTrue(cacheService.isKataCompleted("KATA-002"));
        assertTrue(cacheService.isKataCompleted("KATA-003"));
    }

    @Test
    @DisplayName("Should clear all cache")
    void testClearCache() {
        cacheService.setAvailableThemes(mockThemes);
        cacheService.setCurrentKata(mockKata);
        cacheService.markKataAsCompleted("KATA-001");

        cacheService.clear();

        assertNull(cacheService.getAvailableThemes());
        assertNull(cacheService.getCurrentKata());
        assertFalse(cacheService.isKataCompleted("KATA-001"));
    }

    @Test
    @DisplayName("Should handle cache updates")
    void testUpdateCache() {
        var kata1 = new KataDelivery("KATA-001", "theme1", "Title1", "Spec", "Path", 100, "beginner", System.currentTimeMillis());
        var kata2 = new KataDelivery("KATA-002", "theme2", "Title2", "Spec", "Path", 150, "intermediate", System.currentTimeMillis());

        cacheService.setCurrentKata(kata1);
        assertEquals("KATA-001", cacheService.getCurrentKata().kataId());

        cacheService.setCurrentKata(kata2);
        assertEquals("KATA-002", cacheService.getCurrentKata().kataId());
    }
}
