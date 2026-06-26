package com.plotmap.backend.repository.jpa

import com.plotmap.backend.model.entity.ProjectChapter
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProjectChapterRepository : JpaRepository<ProjectChapter, UUID> {

    fun findAllByProjectIdOrderByChapterOrderAsc(projectId: UUID): List<ProjectChapter>

    fun countByProjectId(projectId: UUID): Int
}
