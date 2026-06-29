package com.plotmap.backend.config

import com.plotmap.backend.service.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7).trim()
            try {
                val userId = jwtService.validateTokenAndGetUserId(token)

                val authentication = UsernamePasswordAuthenticationToken(
                    userId.toString(),
                    null,
                    emptyList()
                )
                SecurityContextHolder.getContext().authentication = authentication
            } catch (e: Exception) {
                log.debug("JWT validation failed: {}", e.message)
            }
        }

        filterChain.doFilter(request, response)
    }
}
