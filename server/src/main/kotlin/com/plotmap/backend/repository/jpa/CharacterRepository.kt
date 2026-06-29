package com.plotmap.backend.repository.jpa

import com.plotmap.backend.model.entity.Character
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CharacterRepository : JpaRepository<Character, UUID> {

    fun findAllByProjectId(projectId: UUID): List<Character>

    fun findByIdAndProjectId(id: UUID, projectId: UUID): Character?

    fun existsByIdAndProjectId(id: UUID, projectId: UUID): Boolean

    fun deleteAllByProjectId(projectId: UUID)
}
