package com.plotmap.backend.controller

import com.plotmap.backend.exception.InvalidCredentialsException
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

fun getCurrentUserId(): UUID {
    val principal = SecurityContextHolder.getContext()
        .authentication
        ?.principal as? String
        ?: throw InvalidCredentialsException("Missing or invalid token")

    return try {
        UUID.fromString(principal)
    } catch (e: IllegalArgumentException) {
        throw InvalidCredentialsException("Invalid user identifier in token")
    }
}
