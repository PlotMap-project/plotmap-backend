package com.plotmap.backend.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.util.UUID

data class EventToCharacterId(
    val idProject: UUID = UUID(0L, 0L),
    val idEvent: UUID = UUID(0L, 0L),
    val idCharacter: UUID = UUID(0L, 0L)
) : Serializable

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
