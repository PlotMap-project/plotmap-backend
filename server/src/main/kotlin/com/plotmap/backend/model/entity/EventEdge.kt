package com.plotmap.backend.model.entity

import com.plotmap.backend.model.enum.ConnectionType
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
@Table(name = "event_edges")
class EventEdge(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "id_project", nullable = false)
    val projectId: UUID,

    @Column(name = "source_event_id", nullable = false)
    val sourceEventId: UUID,

    @Column(name = "target_event_id", nullable = false)
    val targetEventId: UUID,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    var type: ConnectionType = ConnectionType.CAUSAL,

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String = "",

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
