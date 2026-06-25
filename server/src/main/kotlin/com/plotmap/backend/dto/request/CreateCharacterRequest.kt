package com.plotmap.backend.dto.request

data class CreateCharacterRequest(
    val name: String,
    val description: String = "",
    val role: String = "SUPPORTING",
    val color: String? = null
)
