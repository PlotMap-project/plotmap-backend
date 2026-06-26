package com.plotmap.backend.model.node

import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node

@Node("Event")
data class EventNode(
    @Id
    val id: String,
    val projectId: String,
    val title: String,
    val description: String = "",
    val level: Int = 0,
    val orderInLevel: Int = 0,
    val suggestedSystemRole: String? = null,
    val status: String = "PLANNED",
    val color: String? = null
)
