package com.plotmap.backend.service

import com.plotmap.backend.dto.request.LoginRequestWithEmail
import com.plotmap.backend.dto.request.RegisterRequest
import com.plotmap.backend.exception.EmailAlreadyExistsException
import com.plotmap.backend.exception.InvalidCredentialsException
import com.plotmap.backend.exception.NameAlreadyExistsException
import com.plotmap.backend.model.entity.User
import com.plotmap.backend.repository.jpa.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var passwordEncoder: PasswordEncoder
    @Mock lateinit var jwtService: JwtService

    @InjectMocks
    lateinit var authService: AuthService

    @Test
    fun `register should succeed with valid data`() {
        val request = RegisterRequest(
            email = "test@example.com",
            password = "password123",
            name = "TestUser"
        )

        whenever(userRepository.existsByEmail(request.email)).thenReturn(false)
        whenever(userRepository.existsByName(request.name)).thenReturn(false)
        whenever(passwordEncoder.encode(request.password)).thenReturn("hashed_password")
        whenever(userRepository.save(any<User>())).thenAnswer { invocation ->
            invocation.getArgument<User>(0)
        }
        whenever(jwtService.generateToken(any<UUID>())).thenReturn("fake-jwt-token")

        val result = authService.register(request)

        assertEquals("test@example.com", result.email)
        assertEquals("TestUser", result.name)
        assertEquals("fake-jwt-token", result.token)
        assertNotNull(result.userId)
    }

    @Test
    fun `register should throw if email already exists`() {
        val request = RegisterRequest(
            email = "taken@example.com",
            password = "password123",
            name = "NewUser"
        )

        whenever(userRepository.existsByEmail(request.email)).thenReturn(true)

        assertThrows<EmailAlreadyExistsException> {
            authService.register(request)
        }
    }

    @Test
    fun `register should throw if name already exists`() {
        val request = RegisterRequest(
            email = "new@example.com",
            password = "password123",
            name = "TakenName"
        )

        whenever(userRepository.existsByEmail(request.email)).thenReturn(false)
        whenever(userRepository.existsByName(request.name)).thenReturn(true)

        assertThrows<NameAlreadyExistsException> {
            authService.register(request)
        }
    }

    @Test
    fun `loginWithEmail should succeed with correct credentials`() {
        val user = User(
            email = "test@example.com",
            passwordHash = "hashed",
            name = "TestUser"
        )

        val request = LoginRequestWithEmail(
            email = "test@example.com",
            password = "password123"
        )

        whenever(userRepository.findByEmail(request.email)).thenReturn(user)
        whenever(passwordEncoder.matches(request.password, user.passwordHash)).thenReturn(true)
        whenever(jwtService.generateToken(user.id)).thenReturn("fake-jwt-token")

        val result = authService.loginWithEmail(request)

        assertEquals("test@example.com", result.email)
        assertEquals("fake-jwt-token", result.token)
    }

    @Test
    fun `loginWithEmail should throw on wrong password`() {
        val user = User(
            email = "test@example.com",
            passwordHash = "hashed",
            name = "TestUser"
        )

        val request = LoginRequestWithEmail(
            email = "test@example.com",
            password = "wrong_password"
        )

        whenever(userRepository.findByEmail(request.email)).thenReturn(user)
        whenever(passwordEncoder.matches(request.password, user.passwordHash)).thenReturn(false)

        assertThrows<InvalidCredentialsException> {
            authService.loginWithEmail(request)
        }
    }

    @Test
    fun `loginWithEmail should throw on non-existent email`() {
        val request = LoginRequestWithEmail(
            email = "ghost@example.com",
            password = "password123"
        )

        whenever(userRepository.findByEmail(request.email)).thenReturn(null)

        assertThrows<InvalidCredentialsException> {
            authService.loginWithEmail(request)
        }
    }
}
