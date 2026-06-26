package com.plotmap.backend.model.node

import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node

@Node("Character")
data class CharacterNode(
    @Id
    val id: String,
    val projectId: String,
    val name: String,
    val description: String = "",
    val role: String = "SUPPORTING"
)
