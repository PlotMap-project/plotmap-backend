package com.plotmap.backend.repository

import com.plotmap.backend.entity.UserToProject
import com.plotmap.backend.entity.UserToProjectId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserToProjectRepository : JpaRepository<UserToProject, UserToProjectId> {
    fun existsByIdUserAndIdProject(idUser: UUID, idProject: UUID): Boolean
}
