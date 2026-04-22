package com.plotmap.backend.service

import com.plotmap.backend.dto.request.LoginRequestWithEmail
import com.plotmap.backend.dto.request.LoginRequestWithName
import com.plotmap.backend.dto.request.RegisterRequest
import com.plotmap.backend.dto.response.AuthResponse
import com.plotmap.backend.entity.User
import com.plotmap.backend.exception.EmailAlreadyExistsException
import com.plotmap.backend.exception.InvalidCredentialsException
import com.plotmap.backend.exception.NameAlreadyExistsException
import com.plotmap.backend.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {
    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException(
                "Email ${request.email} is already registered"
            )
        }

        if (userRepository.existsByName(request.name)) {
            throw NameAlreadyExistsException(
                "Name ${request.name} is already taken"
            )
        }

        val hashedPassword = requireNotNull(passwordEncoder.encode(request.password)) {
            "Password encoder returned null"
        }

        val user = User(
            email = request.email,
            passwordHash = hashedPassword,
            name = request.name
        )

        val saved = userRepository.save(user)

        return AuthResponse(
            userId = saved.id.toString(),
            email = saved.email,
            name = saved.name,
            token = jwtService.generateToken(saved.id)
        )
    }

    fun loginWithEmail(request: LoginRequestWithEmail): AuthResponse {
        val user = userRepository.findByEmail(request.email)
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
        userId = this.id.toString(),
        email = this.email,
        name = this.name,
        token = jwtService.generateToken(this.id)
    )
}
