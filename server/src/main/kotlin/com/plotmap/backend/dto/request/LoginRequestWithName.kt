package com.plotmap.backend.dto.request

import jakarta.validation.constraints.NotBlank

data class LoginRequestWithName(
    @field:NotBlank(message = "Name is required")
    val name: String,

    @field:NotBlank(message = "Password is required")
    val password: String
)
