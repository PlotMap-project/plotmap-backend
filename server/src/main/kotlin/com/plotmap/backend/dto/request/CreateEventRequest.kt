package com.plotmap.backend.dto.request

data class CreateEventRequest(
    val title: String,
    val description: String = "",
    val suggestedSystemRole: String = "REGULAR",
    val impactLevel: Int = 5,
    val status: String = "PLANNED",
    val userNotes: String = "",
    val level: Int = 0,
    val orderInLevel: Int = 0,
    val customPositionX: Double? = null,
    val customPositionY: Double? = null,
    val color: String? = null,
    val characterIds: List<String> = emptyList(),
    val storyArcIds: List<String> = emptyList(),
    val tagIds: List<String> = emptyList()
)
