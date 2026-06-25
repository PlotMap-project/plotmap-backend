package com.plotmap.backend.repository.jpa

import com.plotmap.backend.model.entity.GenerationJob
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GenerationJobRepository : JpaRepository<GenerationJob, UUID> {

    fun findAllByProjectId(projectId: UUID): List<GenerationJob>
}
