package com.plotmap.backend.controller

import com.plotmap.backend.dto.request.LoginRequestWithEmail
import com.plotmap.backend.dto.request.LoginRequestWithName
import com.plotmap.backend.dto.request.RegisterRequest
import com.plotmap.backend.dto.response.AuthResponse
import com.plotmap.backend.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody @Valid request: RegisterRequest): AuthResponse {
        return authService.register(request)
    }

    @PostMapping("/login/email")
    fun loginWithEmail(@RequestBody @Valid request: LoginRequestWithEmail): AuthResponse {
        return authService.loginWithEmail(request)
    }

    @PostMapping("/login/name")
    fun loginWithName(@RequestBody @Valid request: LoginRequestWithName): AuthResponse {
        return authService.loginWithName(request)
    }
}
