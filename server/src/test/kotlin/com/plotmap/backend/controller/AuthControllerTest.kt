package com.plotmap.backend.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.plotmap.backend.dto.request.LoginRequestWithEmail
import com.plotmap.backend.dto.request.RegisterRequest
import com.plotmap.backend.dto.response.AuthResponse
import com.plotmap.backend.exception.EmailAlreadyExistsException
import com.plotmap.backend.exception.GlobalExceptionHandler
import com.plotmap.backend.exception.InvalidCredentialsException
import com.plotmap.backend.service.AuthService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class AuthControllerTest {

    private lateinit var authService: AuthService
    private lateinit var mockMvc: MockMvc
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        authService = mock()
        mockMvc = MockMvcBuilders
            .standaloneSetup(AuthController(authService))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `POST register should return 201 on success`() {
        val response = AuthResponse(
            userId = "test-id",
            email = "test@example.com",
            name = "TestUser",
            token = "fake-token"
        )

        whenever(authService.register(any())).thenReturn(response)

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        RegisterRequest(
                            email = "test@example.com",
                            password = "password123",
                            name = "TestUser"
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.token").value("fake-token"))
    }

    @Test
    fun `POST register should return 409 on duplicate email`() {
        whenever(authService.register(any()))
            .thenThrow(EmailAlreadyExistsException("Email already exists"))

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        RegisterRequest(
                            email = "taken@example.com",
                            password = "password123",
                            name = "NewUser"
                        )
                    )
                )
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_EXISTS"))
    }

    @Test
    fun `POST login email should return 200 on success`() {
        val response = AuthResponse(
            userId = "test-id",
            email = "test@example.com",
            name = "TestUser",
            token = "fake-token"
        )

        whenever(authService.loginWithEmail(any())).thenReturn(response)

        mockMvc.perform(
            post("/api/v1/auth/login/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        LoginRequestWithEmail(
                            email = "test@example.com",
                            password = "password123"
                        )
                    )
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").value("fake-token"))
    }

    @Test
    fun `POST login email should return 401 on wrong credentials`() {
        whenever(authService.loginWithEmail(any()))
            .thenThrow(InvalidCredentialsException("Invalid email or password"))

        mockMvc.perform(
            post("/api/v1/auth/login/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        LoginRequestWithEmail(
                            email = "wrong@example.com",
                            password = "wrong"
                        )
                    )
                )
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
    }

    @Test
    fun `POST login should return 200 on success`() {
        val response = AuthResponse(
            userId = "test-id",
            email = "test@example.com",
            name = "TestUser",
            token = "fake-token"
        )

        whenever(authService.loginWithEmail(any())).thenReturn(response)

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        LoginRequestWithEmail(
                            email = "test@example.com",
                            password = "password123"
                        )
                    )
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").value("fake-token"))
    }

    @Test
    fun `POST login should return 401 on wrong credentials`() {
        whenever(authService.loginWithEmail(any()))
            .thenThrow(InvalidCredentialsException("Invalid email or password"))

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        LoginRequestWithEmail(
                            email = "wrong@example.com",
                            password = "wrong"
                        )
                    )
                )
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
    }
}
