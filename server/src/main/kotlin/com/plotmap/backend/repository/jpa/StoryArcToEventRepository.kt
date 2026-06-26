package com.plotmap.backend.repository.jpa

import com.plotmap.backend.model.entity.StoryArcToEvent
import com.plotmap.backend.model.entity.StoryArcToEventId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface StoryArcToEventRepository : JpaRepository<StoryArcToEvent, StoryArcToEventId> {

    fun findAllByIdArc(idArc: UUID): List<StoryArcToEvent>

    fun findAllByIdEvent(idEvent: UUID): List<StoryArcToEvent>

    fun findAllByIdProject(idProject: UUID): List<StoryArcToEvent>

    fun deleteAllByIdProjectAndIdEvent(idProject: UUID, idEvent: UUID)

    fun deleteAllByIdProject(idProject: UUID)
}
