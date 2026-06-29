package com.plotmap.backend.repository.jpa

import com.plotmap.backend.model.entity.Project
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ProjectRepository : JpaRepository<Project, UUID> {
    @Query("""
        SELECT p FROM Project p
        JOIN UserToProject utp ON p.id = utp.projectId
        WHERE utp.userId = :userId
        ORDER BY p.createdAt DESC
    """)
    fun findAllByUserId(userId: UUID): List<Project>
}
