package com.plotmap.backend.dto.response

import java.time.Instant

data class JobStatusResponse(
    val jobId: String,
    val projectId: String,
    val status: String,
    val errorMessage: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)
