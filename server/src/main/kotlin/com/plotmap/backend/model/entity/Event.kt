package com.plotmap.backend.model.entity

import com.plotmap.backend.model.enum.EventSource
import com.plotmap.backend.model.enum.EventStatus
import com.plotmap.backend.model.enum.SystemEventRole
import java.time.Instant
import java.util.UUID


data class Event(
    val id: UUID = UUID.randomUUID(),
    val projectId: UUID,

    // AI CAN CHANGE
    val title: String,
    val description: String = "",
    val suggestedSystemRole: SystemEventRole? = null,
    val impactLevel: Int = 5,

    // AI WON`T CHANGE
    val status: EventStatus = EventStatus.PLANNED,
    val justification: String = "",
    val userNotes: String = "",
    val userTagIds: List<UUID> = emptyList(),
    val characterIds: List<UUID> = emptyList(),
    val storyArcIds: List<UUID> = emptyList(),

    // Other
    val level: Int = 0,
    val orderInLevel: Int = 0,
    val customPositionX: Double? = null,
    val customPositionY: Double? = null,
    val isManuallyPositioned: Boolean = false,

    // For AI matching
    val source: EventSource = EventSource.AI_GENERATED,
    val textHash: String? = null,

    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
