package com.plotmap.backend.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.util.UUID

class StoryArcToEventId(
    val idProject: UUID = UUID(0, 0),
    val idArc: UUID = UUID(0, 0),
    val idEvent: UUID = UUID(0, 0)
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StoryArcToEventId) return false
        return idProject == other.idProject
                && idArc == other.idArc
                && idEvent == other.idEvent
    }

    override fun hashCode(): Int {
        var result = idProject.hashCode()
        result = 31 * result + idArc.hashCode()
        result = 31 * result + idEvent.hashCode()
        return result
    }
}

@Entity
@Table(name = "story_arc_to_events")
@IdClass(StoryArcToEventId::class)
class StoryArcToEvent(
    @Id
    @Column(name = "id_project")
    val idProject: UUID,

    @Id
    @Column(name = "id_arc")
    val idArc: UUID,

    @Id
    @Column(name = "id_event")
    val idEvent: UUID,

    @Column(name = "order_in_arc", nullable = false)
    var orderInArc: Int = 0
)
