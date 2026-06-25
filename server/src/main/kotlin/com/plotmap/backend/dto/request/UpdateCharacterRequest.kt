package com.plotmap.backend.dto.request

data class UpdateCharacterRequest(
    val name: String? = null,
    val description: String? = null,
    val role: String? = null,
    val color: String? = null
)
