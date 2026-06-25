package com.plotmap.backend.repository.jpa

import com.plotmap.backend.model.entity.EventToTag
import com.plotmap.backend.model.entity.EventToTagId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EventToTagRepository : JpaRepository<EventToTag, EventToTagId> {

    fun findAllByIdEvent(idEvent: UUID): List<EventToTag>

    fun findAllByIdProject(idProject: UUID): List<EventToTag>

    fun deleteAllByIdProject(idProject: UUID)
}
