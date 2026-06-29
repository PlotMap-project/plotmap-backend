package com.plotmap.backend.dto.response

data class ConnectionDto(
    val id: String,
    val sourceEventId: String,
    val targetEventId: String,
    val type: String,
    val description: String
)
