package com.plotmap.backend.repository.jpa

import com.plotmap.backend.model.entity.StoryArc
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface StoryArcRepository : JpaRepository<StoryArc, UUID> {

    fun findAllByProjectId(projectId: UUID): List<StoryArc>

    fun findByIdAndProjectId(id: UUID, projectId: UUID): StoryArc?

    fun existsByIdAndProjectId(id: UUID, projectId: UUID): Boolean
}
