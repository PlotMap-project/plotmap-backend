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
    val title: String,

    @Column(columnDefinition = "text")
    val description: String = "",

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "suggested_system_role")
    val suggestedSystemRole: SystemEventRole? = null,

    @Column(name = "impact_level", nullable = false)
    val impactLevel: Int = 5,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    val status: EventStatus = EventStatus.PLANNED,

    @Column(name = "user_notes", columnDefinition = "text")
    val userNotes: String = "",

    @Column(name = "level")
    val level: Int = 0,

    @Column(name = "order_in_level")
    val orderInLevel: Int = 0,

    @Column(name = "custom_position_x")
    val customPositionX: Double? = null,

    @Column(name = "custom_position_y")
    val customPositionY: Double? = null,

    @Column(name = "color")
    val color: String? = null,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    val source: EventSource = EventSource.USER_CREATED,

    @Column(name = "source_context", nullable = false, columnDefinition = "TEXT")
    var sourceContext: String = "",

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)
