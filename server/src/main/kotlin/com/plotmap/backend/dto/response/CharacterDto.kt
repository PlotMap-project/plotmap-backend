package com.plotmap.backend.dto.response

data class CharacterDto(
    val id: String,
    val name: String,
    val description: String,
    val role: String,
    val color: String?
)
