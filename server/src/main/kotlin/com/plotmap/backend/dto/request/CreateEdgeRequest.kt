package com.plotmap.backend.dto.request

data class CreateEdgeRequest(
    val sourceEventId: String,
    val targetEventId: String,
    val type: String? = null,
    val description: String? = null
)
