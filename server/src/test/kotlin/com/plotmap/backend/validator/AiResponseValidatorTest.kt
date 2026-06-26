package com.plotmap.backend.validator

import com.plotmap.backend.dto.ai.AiEdgeDto
import com.plotmap.backend.dto.ai.AiEventDto
import com.plotmap.backend.dto.ai.AiGraphResponse
import com.plotmap.backend.service.AiResponseValidator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AiResponseValidatorTest {

    private val validator = AiResponseValidator()

    @Test
    fun `should return false if AI returns no events`() {
        val response = AiGraphResponse(events = emptyList())
        val result = validator.validate(response)
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("AI returned no events"))
    }

    @Test
    fun `should replace invalid color with default color`() {
        val badEvent = AiEventDto(
            id = "event-1",
            title = "Test",
            color = "#WRONG_COLOR"
        )
        val response = AiGraphResponse(events = listOf(badEvent))
        val result = validator.validate(response)
        assertTrue(result.isValid)
        val sanitizedEvent = result.sanitized!!.events.first()
        assertEquals("#FAFAD2", sanitizedEvent.color)
        assertTrue(result.warnings.any { it.contains("invalid color") })
    }

    @Test
    fun `should remove edges with non-existent target or source`() {
        val event = AiEventDto(id = "event-1", title = "Test")
        val badEdge = AiEdgeDto(
            sourceEventId = "event-1",
            targetEventId = "ghost-event-2",
            type = "CAUSAL",
            description = "Test"
        )
        val response = AiGraphResponse(events = listOf(event), edges = listOf(badEdge))
        val result = validator.validate(response)
        assertTrue(result.isValid)
        assertTrue(result.sanitized!!.edges.isEmpty())
    }
}
