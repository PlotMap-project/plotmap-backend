package com.plotmap.backend.dto.request

import jakarta.validation.constraints.NotBlank

data class AddChapterRequest(
    val title: String? = null,

    @field:NotBlank(message = "Chapter text is required")
    val text: String
)
