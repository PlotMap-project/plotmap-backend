package com.plotmap.backend.repository.jpa

import com.plotmap.backend.model.entity.StoryArcToEvent
import com.plotmap.backend.model.entity.StoryArcToEventId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface StoryArcToEventRepository : JpaRepository<StoryArcToEvent, StoryArcToEventId> {

    fun findAllByArcId(arcId: UUID): List<StoryArcToEvent>

    fun findAllByEventId(eventId: UUID): List<StoryArcToEvent>

    fun findAllByProjectId(projectId: UUID): List<StoryArcToEvent>

    fun deleteAllByProjectIdAndEventId(projectId: UUID, eventId: UUID)

    fun deleteAllByProjectId(projectId: UUID)
}
