package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PakhiUnitTest {

    @Test
    fun testPakhiIdFormat() {
        val pakhiIdPattern = Regex("^PK-\\d{6}$")
        assertTrue("PK-583921 should match pattern", pakhiIdPattern.matches("PK-583921"))
        assertTrue("PK-102938 should match pattern", pakhiIdPattern.matches("PK-102938"))
        assertFalse("Invalid format without prefix", pakhiIdPattern.matches("583921"))
        assertFalse("Invalid digit count", pakhiIdPattern.matches("PK-12345"))
        assertFalse("Invalid characters", pakhiIdPattern.matches("PK-ABCDEF"))
    }

    @Test
    fun testDeterministicChatId() {
        val userA = "usr_tanvir_1"
        val userB = "usr_sadia_2"

        val sorted1 = listOf(userA, userB).sorted().joinToString("_")
        val sorted2 = listOf(userB, userA).sorted().joinToString("_")

        assertEquals("Chat ID must be deterministic regardless of order", sorted1, sorted2)
        assertEquals("usr_sadia_2_usr_tanvir_1", sorted1)
    }

    @Test
    fun testMessageCharacterLimit() {
        val validMessage = "আসসালামু আলাইকুম, কেমন আছেন?"
        assertTrue("Message within 4000 limit", validMessage.length <= 4000)

        val longMessage = "a".repeat(4001)
        assertFalse("Message exceeds 4000 limit", longMessage.length <= 4000)
    }
}
