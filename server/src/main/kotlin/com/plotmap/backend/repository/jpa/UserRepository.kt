package com.plotmap.backend.repository.jpa

import com.plotmap.backend.model.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
    fun findByName(name: String): User?
    fun existsByEmail(email: String): Boolean
    fun existsByName(name: String): Boolean
}
