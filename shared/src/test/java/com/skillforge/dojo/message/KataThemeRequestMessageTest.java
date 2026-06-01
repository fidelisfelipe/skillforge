package com.skillforge.dojo.message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KataThemeRequestMessage Tests")
class KataThemeRequestMessageTest {

    @Test
    @DisplayName("Should create valid message with all fields")
    void testValidMessage() {
        long timestamp = System.currentTimeMillis();

        KataThemeRequestMessage msg = new KataThemeRequestMessage(
            "alice",
            "LIST_THEMES",
            null,
            timestamp
        );

        assertEquals("alice", msg.heroId());
        assertEquals("LIST_THEMES", msg.requestType());
        assertNull(msg.themeId());
        assertEquals(timestamp, msg.timestamp());
    }

    @Test
    @DisplayName("Should reject blank heroId")
    void testInvalidBlankHeroId() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new KataThemeRequestMessage("", "LIST_THEMES", null, System.currentTimeMillis())
        );
        assertTrue(exception.getMessage().contains("heroId"));
    }

    @Test
    @DisplayName("Should reject null heroId")
    void testInvalidNullHeroId() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new KataThemeRequestMessage(null, "LIST_THEMES", null, System.currentTimeMillis())
        );
        assertTrue(exception.getMessage().contains("heroId"));
    }

    @Test
    @DisplayName("Should reject null requestType")
    void testInvalidNullRequestType() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new KataThemeRequestMessage("alice", null, null, System.currentTimeMillis())
        );
        assertTrue(exception.getMessage().contains("requestType"));
    }

    @Test
    @DisplayName("Should reject invalid timestamp")
    void testInvalidTimestamp() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new KataThemeRequestMessage("alice", "LIST_THEMES", null, 0)
        );
        assertTrue(exception.getMessage().contains("timestamp"));
    }

    @Test
    @DisplayName("Should accept CHOOSE_THEME with themeId")
    void testChooseThemeWithThemeId() {
        KataThemeRequestMessage msg = new KataThemeRequestMessage(
            "bob",
            "CHOOSE_THEME",
            "virtual-threads",
            System.currentTimeMillis()
        );

        assertEquals("bob", msg.heroId());
        assertEquals("CHOOSE_THEME", msg.requestType());
        assertEquals("virtual-threads", msg.themeId());
    }
}
