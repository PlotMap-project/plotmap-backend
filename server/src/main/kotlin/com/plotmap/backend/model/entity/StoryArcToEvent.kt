package com.plotmap.backend.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.util.UUID

data class StoryArcToEventId(
    val idProject: UUID = UUID(0L, 0L),
    val idArc: UUID = UUID(0L, 0L),
    val idEvent: UUID = UUID(0L, 0L)
) : Serializable

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
