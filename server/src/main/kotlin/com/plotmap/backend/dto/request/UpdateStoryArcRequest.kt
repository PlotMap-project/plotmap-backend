package com.plotmap.backend.dto.request

data class UpdateStoryArcRequest(
    val title: String? = null,
    val description: String? = null,
    val color: String? = null
)
