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
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateToken(userId: UUID): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(expiryDate)
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
