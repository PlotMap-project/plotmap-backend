package com.plotmap.backend.repository.jpa

import com.plotmap.backend.model.entity.UserToProject
import com.plotmap.backend.model.entity.UserToProjectId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserToProjectRepository : JpaRepository<UserToProject, UserToProjectId> {
    fun existsByUserIdAndProjectId(userId: UUID, projectId: UUID): Boolean
}
