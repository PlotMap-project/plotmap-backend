package com.plotmap.backend.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateEventRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 255, message = "Title is too long")
    val title: String,

    val description: String? = null,
    val suggestedSystemRole: String? = null,

    @field:Min(value = 1, message = "Impact level must be between 1 and 10")
    @field:Max(value = 10, message = "Impact level must be between 1 and 10")
    val impactLevel: Int? = null,

    val status: String? = null,
    val userNotes: String? = null,
    val level: Int? = null,
    val orderInLevel: Int? = null,
    val customPositionX: Double? = null,
    val customPositionY: Double? = null,

    @field:Pattern(
        regexp = "^#[0-9A-Fa-f]{6}$",
        message = "Color must be a valid HEX color (e.g. #FF0000)"
    )
    val color: String? = null,

    val characterIds: List<String>? = null,
    val storyArcIds: List<String>? = null,
    val tagIds: List<String>? = null
)
