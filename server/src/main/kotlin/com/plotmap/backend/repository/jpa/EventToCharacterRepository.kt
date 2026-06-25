package com.plotmap.backend.repository.jpa

import com.plotmap.backend.model.entity.EventToCharacter
import com.plotmap.backend.model.entity.EventToCharacterId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EventToCharacterRepository : JpaRepository<EventToCharacter, EventToCharacterId> {

    fun findAllByIdEvent(idEvent: UUID): List<EventToCharacter>

    fun findAllByIdProject(idProject: UUID): List<EventToCharacter>

    fun deleteAllByIdProject(idProject: UUID)
}
