package com.plotmap.backend.dto.request

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String
)
