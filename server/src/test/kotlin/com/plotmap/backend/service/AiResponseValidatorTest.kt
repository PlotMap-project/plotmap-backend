package com.plotmap.backend.service

import com.plotmap.backend.dto.ai.AiCharacterDto
import com.plotmap.backend.dto.ai.AiEdgeDto
import com.plotmap.backend.dto.ai.AiEventDto
import com.plotmap.backend.dto.ai.AiGraphResponse
import com.plotmap.backend.dto.ai.AiStoryArcDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AiResponseValidatorTest {

    private val validator = AiResponseValidator()

    private fun event(
        id: String = "e1",
        title: String = "Test",
        role: String = "REGULAR",
        impact: Int = 5,
        color: String = "#FAFAD2",
        level: Int = 0,
        order: Int = 0,
        charIds: List<String> = emptyList(),
        arcIds: List<String> = emptyList()
    ) = AiEventDto(
        id = id, title = title, suggestedSystemRole = role,
        impactLevel = impact, color = color, level = level,
        orderInLevel = order, characterIds = charIds, storyArcIds = arcIds
    )

    @Test
    fun `valid response passes`() {
        val response = AiGraphResponse(
            events = listOf(event()),
            edges = emptyList()
        )
        val result = validator.validate(response)
        assertTrue(result.isValid)
        assertNotNull(result.sanitized)
    }

    @Test
    fun `empty events fails`() {
        val response = AiGraphResponse(events = emptyList())
        val result = validator.validate(response)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { "no events" in it.lowercase() })
    }

    @Test
    fun `duplicate event IDs fails`() {
        val response = AiGraphResponse(
            events = listOf(event(id = "e1"), event(id = "e1"))
        )
        val result = validator.validate(response)
        assertFalse(result.isValid)
    }

    @Test
    fun `self-loop edge is removed`() {
        val response = AiGraphResponse(
            events = listOf(event(id = "e1")),
            edges = listOf(AiEdgeDto("e1", "e1", "CAUSAL"))
        )
        val result = validator.validate(response)
        assertTrue(result.isValid)
        assertEquals(0, result.sanitized!!.edges.size)
        assertTrue(result.warnings.any { "self-loop" in it.lowercase() })
    }

    @Test
    fun `edge with missing target is removed`() {
        val response = AiGraphResponse(
            events = listOf(event(id = "e1")),
            edges = listOf(AiEdgeDto("e1", "e_missing", "CAUSAL"))
        )
        val result = validator.validate(response)
        assertTrue(result.isValid)
        assertEquals(0, result.sanitized!!.edges.size)
    }

    @Test
    fun `impact level is clamped`() {
        val response = AiGraphResponse(
            events = listOf(event(impact = 99))
        )
        val result = validator.validate(response)
        assertTrue(result.isValid)
        assertEquals(10, result.sanitized!!.events[0].impactLevel)
    }

    @Test
    fun `invalid color is replaced with default`() {
        val response = AiGraphResponse(
            events = listOf(event(color = "red"))
        )
        val result = validator.validate(response)
        assertTrue(result.isValid)
        assertEquals("#FAFAD2", result.sanitized!!.events[0].color)
    }

    @Test
    fun `unknown role is replaced with REGULAR`() {
        val response = AiGraphResponse(
            events = listOf(event(role = "NONSENSE"))
        )
        val result = validator.validate(response)
        assertTrue(result.isValid)
        assertEquals("REGULAR", result.sanitized!!.events[0].suggestedSystemRole)
    }

    @Test
    fun `unknown characterId in event is removed`() {
        val response = AiGraphResponse(
            events = listOf(event(charIds = listOf("c_unknown"))),
            characters = emptyList()
        )
        val result = validator.validate(response)
        assertTrue(result.isValid)
        assertTrue(result.sanitized!!.events[0].characterIds.isEmpty())
    }

    @Test
    fun `valid edge passes through`() {
        val response = AiGraphResponse(
            events = listOf(event(id = "e1"), event(id = "e2", order = 1)),
            edges = listOf(AiEdgeDto("e1", "e2", "CAUSAL", "causes"))
        )
        val result = validator.validate(response)
        assertTrue(result.isValid)
        assertEquals(1, result.sanitized!!.edges.size)
        assertEquals("CAUSAL", result.sanitized!!.edges[0].type)
    }
}
