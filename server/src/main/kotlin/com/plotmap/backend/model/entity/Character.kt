package com.plotmap.backend.model.entity

import java.time.Instant
import java.util.UUID


data class Character(
    val id: UUID = UUID.randomUUID(),
    val projectId: UUID,
    val name: String,
    val description: String = "",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
