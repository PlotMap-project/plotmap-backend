package com.plotmap.backend.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class JwtServiceTest {

    private val secret = "a".repeat(64)
    private val expiration = 3600000L
    private val jwtService = JwtService(secret, expiration)

    @Test
    fun `generate and validate token`() {
        val userId = UUID.randomUUID()
        val token = jwtService.generateToken(userId)

        assertTrue(jwtService.isTokenValid(token))
        assertEquals(userId, jwtService.validateTokenAndGetUserId(token))
    }

    @Test
    fun `invalid token returns false`() {
        assertFalse(jwtService.isTokenValid("garbage.token.here"))
    }

    @Test
    fun `expired token is invalid`() {
        val shortLivedService = JwtService(secret, -1000L)
        val userId = UUID.randomUUID()
        val token = shortLivedService.generateToken(userId)

        assertFalse(jwtService.isTokenValid(token))
    }

    @Test
    fun `short secret throws`() {
        assertThrows<IllegalArgumentException> {
            JwtService("short", expiration).generateToken(UUID.randomUUID())
        }
    }
}
