package com.plotmap.backend.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.util.UUID

class EventToCharacterId(
    val idProject: UUID = UUID.randomUUID(),
    val idEvent: UUID = UUID.randomUUID(),
    val idCharacter: UUID = UUID.randomUUID()
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EventToCharacterId) return false
        return idProject == other.idProject
                && idEvent == other.idEvent
                && idCharacter == other.idCharacter
    }

    override fun hashCode(): Int {
        var result = idProject.hashCode()
        result = 31 * result + idEvent.hashCode()
        result = 31 * result + idCharacter.hashCode()
        return result
    }
}

@Entity
@Table(name = "event_to_characters")
@IdClass(EventToCharacterId::class)
class EventToCharacter(
    @Id
    @Column(name = "id_project")
    val idProject: UUID,

    @Id
    @Column(name = "id_event")
    val idEvent: UUID,

    @Id
    @Column(name = "id_character")
    val idCharacter: UUID
)
