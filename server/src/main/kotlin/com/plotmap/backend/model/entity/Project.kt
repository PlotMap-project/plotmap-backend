package com.plotmap.backend.model.entity

import java.time.Instant
import java.util.UUID

data class Project(
    val id: UUID = UUID.randomUUID(),
    val ownerId: UUID,
    val title: String,
    val description: String = "",
    val sourceText: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
