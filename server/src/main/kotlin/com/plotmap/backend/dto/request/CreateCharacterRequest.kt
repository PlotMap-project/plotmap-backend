package com.plotmap.backend.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateCharacterRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255, message = "Name is too long")
    val name: String,

    val description: String = "",
    val role: String = "SUPPORTING",

    @field:Pattern(
        regexp = "^#[0-9A-Fa-f]{6}$",
        message = "Color must be a valid HEX color (e.g. #FF0000)"
    )
    val color: String? = null
)
