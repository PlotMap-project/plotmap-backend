package com.plotmap.backend.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}")
    private val secret: String,

    @Value("\${jwt.expiration}")
    private val expiration: Long
) {
    private val signingKey: SecretKey by lazy {
        val keyBytes = secret.toByteArray()
        require(keyBytes.size >= 32) {
            "jwt.secret must be at least 32 characters long"
        }
        Keys.hmacShaKeyFor(keyBytes)
    }

    fun generateToken(userId: UUID): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(Date(now.time + expiration))
            .signWith(signingKey)
            .compact()
    }

    fun validateTokenAndGetUserId(token: String): UUID {
        val claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload

        return UUID.fromString(claims.subject)
    }

    fun isTokenValid(token: String): Boolean {
        return try {
            validateTokenAndGetUserId(token)
            true
        } catch (e: Exception) {
            false
        }
    }
}
