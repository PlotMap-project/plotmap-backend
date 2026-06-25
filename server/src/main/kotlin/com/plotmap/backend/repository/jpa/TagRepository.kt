package com.plotmap.backend.repository.jpa

import com.plotmap.backend.model.entity.Tag
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TagRepository : JpaRepository<Tag, UUID> {

    fun findAllByProjectId(projectId: UUID): List<Tag>

    fun existsByIdAndProjectId(id: UUID, projectId: UUID): Boolean
}
