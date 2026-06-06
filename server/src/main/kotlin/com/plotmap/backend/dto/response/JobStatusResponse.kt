package com.plotmap.backend.dto.response

import com.plotmap.backend.dto.ai.AiGraphResponse
import java.time.Instant

data class JobStatusResponse(
    val jobId: String,
    val projectId: String,
    val status: String,
    val errorMessage: String? = null,
    val result: AiGraphResponse? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)
