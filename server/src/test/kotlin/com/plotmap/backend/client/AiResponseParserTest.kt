package com.plotmap.backend.client

import com.plotmap.backend.dto.ai.AiEventDto
import com.plotmap.backend.dto.ai.AiGraphResponse
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.ObjectMapper

class AiResponseParserTest {

    private val objectMapper = mockk<ObjectMapper>()
    private val parser = AiResponseParser(objectMapper)

    @Test
    fun `parses clean JSON`() {
        val json = """
        {
            "events": [{"id":"e1","title":"Event 1"}],
            "edges": [],
            "characters": [],
            "storyArcs": []
        }
        """.trimIndent()

        every {
            objectMapper.readValue(any<String>(), AiGraphResponse::class.java)
        } returns AiGraphResponse(
            events = listOf(AiEventDto(id = "e1", title = "Event 1"))
        )

        val result = parser.parse(json)

        assertEquals(1, result.events.size)
        assertEquals("Event 1", result.events[0].title)
    }

    @Test
    fun `strips markdown code block`() {
        val json = """
        ```json
        {
            "events": [{"id":"e1","title":"Event 1"}],
            "edges": [],
            "characters": [],
            "storyArcs": []
        }
        ```
        """.trimIndent()

        every {
            objectMapper.readValue(any<String>(), AiGraphResponse::class.java)
        } answers {
            val cleaned = firstArg<String>()
            assertEquals(
                """
                {
                    "events": [{"id":"e1","title":"Event 1"}],
                    "edges": [],
                    "characters": [],
                    "storyArcs": []
                }
                """.trimIndent(),
                cleaned
            )
            AiGraphResponse(events = listOf(AiEventDto(id = "e1", title = "Event 1")))
        }

        val result = parser.parse(json)
        assertEquals(1, result.events.size)
    }

    @Test
    fun `extracts JSON from surrounding text`() {
        val json = """
        Here is the result:
        {"events":[{"id":"e1","title":"Test"}],"edges":[],"characters":[],"storyArcs":[]}
        Hope this helps!
        """.trimIndent()

        every {
            objectMapper.readValue(any<String>(), AiGraphResponse::class.java)
        } answers {
            val cleaned = firstArg<String>()
            assertEquals(
                """{"events":[{"id":"e1","title":"Test"}],"edges":[],"characters":[],"storyArcs":[]}""",
                cleaned
            )
            AiGraphResponse(events = listOf(AiEventDto(id = "e1", title = "Test")))
        }

        val result = parser.parse(json)
        assertEquals("Test", result.events[0].title)
    }

    @Test
    fun `throws on no JSON`() {
        assertThrows<IllegalStateException> {
            parser.parse("This is just plain text without JSON")
        }
    }

    @Test
    fun `throws on invalid JSON`() {
        val json = """
        {"events":[{"id":"e1","title":"Test"}],"edges":[],"characters":[],"storyArcs":[]}
        """.trimIndent()

        every {
            objectMapper.readValue(any<String>(), AiGraphResponse::class.java)
        } throws RuntimeException("broken json")

        assertThrows<IllegalStateException> {
            parser.parse(json)
        }
    }
}
