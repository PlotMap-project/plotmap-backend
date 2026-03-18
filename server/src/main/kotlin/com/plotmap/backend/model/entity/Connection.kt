package com.plotmap.backend.model.entity

import com.plotmap.backend.model.enum.ConnectionType
import java.time.Instant
import java.util.UUID

data class Connection(
    val id: UUID = UUID.randomUUID(),
    val projectId: UUID,
    val sourceEventId: UUID,
    val targetEventId: UUID,
    val type: ConnectionType = ConnectionType.CAUSAL,
    val description: String = "",
    val strength: Int = 5,
    val createdAt: Instant = Instant.now()
)
