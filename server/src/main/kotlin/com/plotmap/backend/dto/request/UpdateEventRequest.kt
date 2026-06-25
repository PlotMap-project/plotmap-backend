package com.plotmap.backend.dto.request

data class UpdateEventRequest(
    val title: String? = null,
    val description: String? = null,
    val suggestedSystemRole: String? = null,
    val impactLevel: Int? = null,
    val status: String? = null,
    val userNotes: String? = null,
    val level: Int? = null,
    val orderInLevel: Int? = null,
    val customPositionX: Double? = null,
    val customPositionY: Double? = null,
    val color: String? = null,
    val characterIds: List<String>? = null,
    val storyArcIds: List<String>? = null,
    val tagIds: List<String>? = null
)
