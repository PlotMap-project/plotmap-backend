package com.plotmap.backend.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateStoryArcRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 255, message = "Title is too long")
    val title: String,

    val description: String = "",

    @field:Pattern(
        regexp = "^#[0-9A-Fa-f]{6}$",
        message = "Color must be a valid HEX color (e.g. #FF0000)"
    )
    val color: String? = null
)
