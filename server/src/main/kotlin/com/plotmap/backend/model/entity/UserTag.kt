package com.plotmap.backend.model.entity

import java.time.Instant
import java.util.UUID

data class UserTag(
    val id: UUID = UUID.randomUUID(),
    val projectId: UUID,
    val name: String,
    val createdAt: Instant = Instant.now()
)
