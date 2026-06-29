package com.plotmap.backend.repository.jpa

import com.plotmap.backend.model.entity.EventToCharacter
import com.plotmap.backend.model.entity.EventToCharacterId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EventToCharacterRepository : JpaRepository<EventToCharacter, EventToCharacterId> {

    fun findAllByEventId(eventId: UUID): List<EventToCharacter>

    fun findAllByProjectId(projectId: UUID): List<EventToCharacter>

    fun deleteAllByProjectId(projectId: UUID)

    fun deleteAllByProjectIdAndEventId(projectId: UUID, eventId: UUID)
}
