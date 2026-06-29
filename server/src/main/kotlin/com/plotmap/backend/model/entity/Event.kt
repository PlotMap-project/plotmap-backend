package com.plotmap.backend.model.entity

import com.plotmap.backend.model.enum.EventSource
import com.plotmap.backend.model.enum.EventStatus
import com.plotmap.backend.model.enum.SystemEventRole
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "events")
class Event(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "project_id", nullable = false)
    val projectId: UUID,

    @Column(nullable = false)
    var title: String,

    @Column(columnDefinition = "text")
    var description: String = "",

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "suggested_system_role")
    var suggestedSystemRole: SystemEventRole? = null,

    @Column(name = "impact_level", nullable = false)
    var impactLevel: Int = 5,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    var status: EventStatus = EventStatus.PLANNED,

    @Column(name = "user_notes", columnDefinition = "text")
    var userNotes: String = "",

    @Column(name = "level")
    var level: Int = 0,

    @Column(name = "order_in_level")
    var orderInLevel: Int = 0,

    @Column(name = "custom_position_x")
    var customPositionX: Double? = null,

    @Column(name = "custom_position_y")
    var customPositionY: Double? = null,

    @Column(name = "color")
    var color: String? = null,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    var source: EventSource = EventSource.USER_CREATED,

    @Column(name = "source_context", nullable = false, columnDefinition = "TEXT")
    var sourceContext: String = "",

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
