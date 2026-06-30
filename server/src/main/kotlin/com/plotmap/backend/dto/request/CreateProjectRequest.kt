package com.plotmap.backend.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateProjectRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 255, message = "Title is too long")
    val title: String,

    val description: String = ""
)
