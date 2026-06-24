package com.plotmap.backend.model.domain

import com.plotmap.backend.model.enum.GenerationMode
import com.plotmap.backend.model.enum.JobStatus
import java.time.Instant
import java.util.UUID

data class GenerationJob(
    val id: UUID = UUID.randomUUID(),
    val projectId: UUID,
    val userId: UUID,
    val mode: GenerationMode = GenerationMode.FULL,
    val status: JobStatus = JobStatus.PENDING,
    val sourceText: String,
    val errorMessage: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
