package com.plotmap.backend.dto.response

import java.time.Instant

data class ChapterDto(
    val id: String,
    val chapterOrder: Int,
    val title: String?,
    val createdAt: Instant
)
