package com.plotmap.backend.service

import org.springframework.stereotype.Service
import kotlin.math.max

@Service
class TextChunkingService {

    companion object {
        const val MAX_CHUNK_SIZE = 6000
        const val OVERLAP_SIZE = 300
    }

    fun needsChunking(text: String): Boolean {
        return text.length > MAX_CHUNK_SIZE
    }

    fun chunk(text: String): List<String> {
        if (!needsChunking(text)) {
            return listOf(text)
        }

        val chunks = mutableListOf<String>()
        var start = 0

        while (start < text.length) {
            val hardEnd = (start + MAX_CHUNK_SIZE).coerceAtMost(text.length)
            var end = hardEnd

            if (end < text.length) {
                val lastParagraph = text.lastIndexOf("\n\n", end)
                val lastSentence = text.lastIndexOf(". ", end)
                val lastNewline = text.lastIndexOf("\n", end)

                end = when {
                    lastParagraph > start + MAX_CHUNK_SIZE / 2 -> lastParagraph + 2
                    lastSentence > start + MAX_CHUNK_SIZE / 2 -> lastSentence + 2
                    lastNewline > start + MAX_CHUNK_SIZE / 2 -> lastNewline + 1
                    else -> hardEnd
                }
            }

            val chunk = text.substring(start, end).trim()
            if (chunk.isNotBlank()) {
                chunks.add(chunk)
            }

            if (end >= text.length) {
                break
            }

            start = max(end - OVERLAP_SIZE, start + 1)
        }

        return chunks
    }
}
