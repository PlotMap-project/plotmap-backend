package com.plotmap.backend.service

import com.plotmap.backend.dto.request.LoginRequestWithEmail
import com.plotmap.backend.dto.request.LoginRequestWithName
import com.plotmap.backend.dto.request.RegisterRequest
import com.plotmap.backend.dto.response.AuthResponse
import com.plotmap.backend.exception.EmailAlreadyExistsException
import com.plotmap.backend.exception.InvalidCredentialsException
import com.plotmap.backend.exception.NameAlreadyExistsException
import com.plotmap.backend.model.entity.User
import com.plotmap.backend.repository.jpa.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        val normalizedEmail = request.email.lowercase().trim()

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw EmailAlreadyExistsException("Email is already registered")
        }

        if (userRepository.existsByName(request.name)) {
            throw NameAlreadyExistsException("Name is already taken")
        }

        val user = User(
            email = normalizedEmail,
            passwordHash = passwordEncoder.encode(request.password),
            name = request.name
        )

        val saved = userRepository.save(user)

        return saved.toAuthResponse()
    }

    fun loginWithEmail(request: LoginRequestWithEmail): AuthResponse {
        val normalizedEmail = request.email.lowercase().trim()

        val user = userRepository.findByEmail(normalizedEmail)
            ?: throw InvalidCredentialsException("Invalid email or password")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException("Invalid email or password")
        }

        return user.toAuthResponse()
    }

    fun loginWithName(request: LoginRequestWithName): AuthResponse {
        val user = userRepository.findByName(request.name)
            ?: throw InvalidCredentialsException("Invalid name or password")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException("Invalid name or password")
        }

        return user.toAuthResponse()
    }

    private fun User.toAuthResponse() = AuthResponse(
        userId = id.toString(),
        email = email,
        name = name,
        token = jwtService.generateToken(id)
    )
}
