package com.skillforge.hub.dojo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import(KataService.class)
@DisplayName("KataService Tests")
class KataServiceTest {

    @Autowired
    private KataService kataService;

    @Test
    @DisplayName("Should return list of available themes")
    void testGetAvailableThemes() {
        var themes = kataService.getAvailableThemes("alice");

        assertNotNull(themes);
        assertFalse(themes.isEmpty());
        assertEquals(3, themes.size());
    }

    @Test
    @DisplayName("Should find virtual-threads theme")
    void testThemeExistsVirtualThreads() {
        assertTrue(kataService.themeExists("virtual-threads"));
    }

    @Test
    @DisplayName("Should find records theme")
    void testThemeExistsRecords() {
        assertTrue(kataService.themeExists("records"));
    }

    @Test
    @DisplayName("Should find pattern-matching theme")
    void testThemeExistsPatternMatching() {
        assertTrue(kataService.themeExists("pattern-matching"));
    }

    @Test
    @DisplayName("Should not find nonexistent theme")
    void testThemeNotFound() {
        assertFalse(kataService.themeExists("nonexistent-theme"));
    }

    @Test
    @DisplayName("Should assign first kata for virtual-threads theme")
    void testAssignKataForVirtualThreads() {
        var kata = kataService.assignKata("alice", "virtual-threads");

        assertNotNull(kata);
        assertEquals("KATA-001A", kata.kataId());
        assertEquals("virtual-threads", kata.themeId());
        assertNotNull(kata.title());
    }

    @Test
    @DisplayName("Should return null for nonexistent theme")
    void testAssignKataForNonexistentTheme() {
        var kata = kataService.assignKata("bob", "nonexistent");

        assertNull(kata);
    }

    @Test
    @DisplayName("Should have correct kata metadata")
    void testKataMetadata() {
        var kata = kataService.assignKata("charlie", "virtual-threads");

        assertNotNull(kata);
        assertNotNull(kata.difficulty());
        assertTrue(kata.xpReward() > 0);
        assertNotNull(kata.spec());
        assertFalse(kata.spec().isBlank());
    }

    @Test
    @DisplayName("Should work with different heroes")
    void testAssignKataMultipleHeroes() {
        var kata1 = kataService.assignKata("dave", "virtual-threads");
        var kata2 = kataService.assignKata("eve", "records");

        assertNotNull(kata1);
        assertNotNull(kata2);
        assertEquals("KATA-001A", kata1.kataId());
        assertEquals("KATA-002A", kata2.kataId());
    }

    @Test
    @DisplayName("Should rotate to next kata when assignNext=true")
    void testAssignNextKataRotates() {
        String heroId = "rotation-test-hero";

        var kata1 = kataService.assignKata(heroId, "virtual-threads", false);
        assertNotNull(kata1);
        assertEquals("KATA-001A", kata1.kataId());

        var kata2 = kataService.assignKata(heroId, "virtual-threads", true);
        assertNotNull(kata2);
        assertEquals("KATA-001B", kata2.kataId());

        var kata3 = kataService.assignKata(heroId, "virtual-threads", true);
        assertNotNull(kata3);
        assertEquals("KATA-001C", kata3.kataId());
    }

    @Test
    @DisplayName("Should wrap around after last kata")
    void testAssignNextKataWrapsAround() {
        String heroId = "wraparound-test-hero";

        kataService.assignKata(heroId, "records", false);        // index 0: KATA-002A
        kataService.assignKata(heroId, "records", true);         // index 1: KATA-002B
        kataService.assignKata(heroId, "records", true);         // index 2: KATA-002C
        var wrapped = kataService.assignKata(heroId, "records", true); // wraps to index 0

        assertNotNull(wrapped);
        assertEquals("KATA-002A", wrapped.kataId());
    }
}
