package com.plotmap.backend.dto.response

data class AddChapterResponse(
    val chapter: ChapterDto,
    val job: JobStatusResponse
)
