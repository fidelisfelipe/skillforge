package com.skillforge.dojo.message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KataValidationResultMessage Tests")
class KataValidationResultMessageTest {

    @Test
    @DisplayName("Should create valid passed message with skill")
    void testValidPassedMessage() {
        KataValidationResultMessage msg = new KataValidationResultMessage(
            "alice",
            "KATA-001",
            true,    // passed
            95,      // score
            100,     // xpEarned
            "java-21-virtual-threads",
            "All tests passed!",
            System.currentTimeMillis()
        );

        assertEquals("alice", msg.heroId());
        assertTrue(msg.passed());
        assertEquals(95, msg.score());
        assertEquals("java-21-virtual-threads", msg.skill());
    }

    @Test
    @DisplayName("Should create valid failed message without skill")
    void testValidFailedMessage() {
        KataValidationResultMessage msg = new KataValidationResultMessage(
            "bob",
            "KATA-002",
            false,   // not passed
            45,      // score
            0,       // xpEarned
            null,    // skill (optional for failed)
            "Test failed: wrong output",
            System.currentTimeMillis()
        );

        assertEquals("bob", msg.heroId());
        assertFalse(msg.passed());
        assertEquals(45, msg.score());
        assertNull(msg.skill());
    }

    @Test
    @DisplayName("Should reject invalid score > 100")
    void testInvalidScoreTooHigh() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new KataValidationResultMessage(
                "alice", "KATA-001", false, 101, 0, null, "msg", System.currentTimeMillis()
            )
        );
        assertTrue(exception.getMessage().contains("score"));
    }

    @Test
    @DisplayName("Should reject invalid score < 0")
    void testInvalidScoreTooLow() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new KataValidationResultMessage(
                "alice", "KATA-001", false, -1, 0, null, "msg", System.currentTimeMillis()
            )
        );
        assertTrue(exception.getMessage().contains("score"));
    }

    @Test
    @DisplayName("Should require skill when passed=true")
    void testPassedRequiresSkill() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new KataValidationResultMessage(
                "alice", "KATA-001", true, 95, 100, null, "msg", System.currentTimeMillis()
            )
        );
        assertTrue(exception.getMessage().contains("skill"));
    }

    @Test
    @DisplayName("Should reject blank heroId")
    void testInvalidBlankHeroId() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new KataValidationResultMessage(
                "", "KATA-001", false, 50, 0, null, "msg", System.currentTimeMillis()
            )
        );
        assertTrue(exception.getMessage().contains("heroId"));
    }

    @Test
    @DisplayName("Should accept score at boundaries (0 and 100)")
    void testBoundaryScores() {
        // Score = 0
        KataValidationResultMessage msg0 = new KataValidationResultMessage(
            "alice", "KATA-001", false, 0, 0, null, "Failed", System.currentTimeMillis()
        );
        assertEquals(0, msg0.score());

        // Score = 100
        KataValidationResultMessage msg100 = new KataValidationResultMessage(
            "bob", "KATA-002", true, 100, 200, "skill", "Perfect!", System.currentTimeMillis()
        );
        assertEquals(100, msg100.score());
    }
}
