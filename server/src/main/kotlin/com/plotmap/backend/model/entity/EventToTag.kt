package com.plotmap.backend.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.util.UUID

class EventToTagId(
    val idProject: UUID = UUID.randomUUID(),
    val idEvent: UUID = UUID.randomUUID(),
    val idTag: UUID = UUID.randomUUID()
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EventToTagId) return false
        return idProject == other.idProject
                && idEvent == other.idEvent
                && idTag == other.idTag
    }

    override fun hashCode(): Int {
        var result = idProject.hashCode()
        result = 31 * result + idEvent.hashCode()
        result = 31 * result + idTag.hashCode()
        return result
    }
}

@Entity
@Table(name = "event_to_tags")
@IdClass(EventToTagId::class)
class EventToTag(
    @Id
    @Column(name = "id_project")
    val idProject: UUID,

    @Id
    @Column(name = "id_event")
    val idEvent: UUID,

    @Id
    @Column(name = "id_tag")
    val idTag: UUID
)
