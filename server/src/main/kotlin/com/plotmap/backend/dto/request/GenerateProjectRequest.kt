package com.plotmap.backend.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class GenerateProjectRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255, message = "Name is too long")
    val name: String,

    val description: String = "",

    @field:NotBlank(message = "Text is required")
    val text: String
)
