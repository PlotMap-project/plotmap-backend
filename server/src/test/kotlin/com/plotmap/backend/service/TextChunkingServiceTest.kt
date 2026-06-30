package com.plotmap.backend.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TextChunkingServiceTest {

    private val service = TextChunkingService()

    @Test
    fun `short text does not need chunking`() {
        val text = "Short story."
        assertFalse(service.needsChunking(text))
        assertEquals(listOf(text), service.chunk(text))
    }

    @Test
    fun `long text is chunked`() {
        val text = "A".repeat(15000)
        assertTrue(service.needsChunking(text))

        val chunks = service.chunk(text)
        assertTrue(chunks.size > 1)
        chunks.forEach { chunk ->
            assertTrue(chunk.length <= TextChunkingService.MAX_CHUNK_SIZE + 100)
        }
    }

    @Test
    fun `chunks prefer paragraph breaks`() {
        val paragraph1 = "A".repeat(4000)
        val paragraph2 = "B".repeat(4000)
        val text = "$paragraph1\n\n$paragraph2"

        val chunks = service.chunk(text)

        assertTrue(chunks.size >= 2)
        assertTrue(chunks[0].startsWith("A"))
        assertTrue(chunks.last().contains("B"))
    }

    @Test
    fun `blank text is not chunked`() {
        val text = "   "
        assertFalse(service.needsChunking(text))
        val chunks = service.chunk(text)
        assertTrue(chunks.size == 1 || chunks.isEmpty())
    }

    @Test
    fun `text exactly at limit is not chunked`() {
        val text = "A".repeat(TextChunkingService.MAX_CHUNK_SIZE)
        assertFalse(service.needsChunking(text))
        assertEquals(1, service.chunk(text).size)
    }
}
