package com.plotmap.backend.dto.request

data class LoginRequestWithEmail(
    val email: String,
    val password: String
)
