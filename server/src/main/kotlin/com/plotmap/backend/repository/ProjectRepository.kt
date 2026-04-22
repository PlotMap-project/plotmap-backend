package com.plotmap.backend.repository

import com.plotmap.backend.entity.Project
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ProjectRepository : JpaRepository<Project, UUID> {
    @Query("""
        SELECT p FROM Project p
        JOIN UserToProject utp ON p.id = utp.idProject
        WHERE utp.idUser = :userId
        ORDER BY p.createdAt DESC
    """)
    fun findAllByUserId(userId: UUID): List<Project>
}
