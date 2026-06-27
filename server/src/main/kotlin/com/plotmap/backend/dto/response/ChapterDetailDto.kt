package com.plotmap.backend.dto.response

import java.time.Instant

data class ChapterDetailDto(
    val id: String,
    val chapterOrder: Int,
    val title: String?,
    val text: String,
    val createdAt: Instant
)
