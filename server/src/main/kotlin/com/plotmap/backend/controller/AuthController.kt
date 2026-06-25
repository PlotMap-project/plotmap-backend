package com.plotmap.backend.controller

import com.plotmap.backend.dto.request.LoginRequestWithEmail
import com.plotmap.backend.dto.request.LoginRequestWithName
import com.plotmap.backend.dto.request.RegisterRequest
import com.plotmap.backend.dto.response.AuthResponse
import com.plotmap.backend.service.AuthService
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
    //POST /api/v1/auth/register
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody request: RegisterRequest): AuthResponse {
        return authService.register(request)
    }
    //POST /api/v1/auth/login/email
    @PostMapping("/login/email")
    fun loginWithEmail(@RequestBody request: LoginRequestWithEmail): AuthResponse {
        return authService.loginWithEmail(request)
    }

    //POST /api/v1/auth/login/name
    @PostMapping("/login/name")
    fun loginWithName(@RequestBody request: LoginRequestWithName): AuthResponse {
        return authService.loginWithName(request)
    }

    // POST /api/v1/auth/login
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequestWithEmail): AuthResponse {
        return authService.loginWithEmail(request)
    }
}
