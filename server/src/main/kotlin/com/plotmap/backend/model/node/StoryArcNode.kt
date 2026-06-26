package com.plotmap.backend.model.node

import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node

@Node("StoryArc")
data class StoryArcNode(
    @Id
    val id: String,
    val projectId: String,
    val title: String,
    val description: String = ""
)
