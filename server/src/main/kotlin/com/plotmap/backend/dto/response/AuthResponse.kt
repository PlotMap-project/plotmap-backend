package com.plotmap.backend.dto.response

data class AuthResponse(
    val userId: String,
    val email: String,
    val name: String,
    val token: String
)
