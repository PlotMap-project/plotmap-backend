package com.plotmap.backend.dto.request

data class CreateStoryArcRequest(
    val title: String,
    val description: String = "",
    val color: String? = null
)
