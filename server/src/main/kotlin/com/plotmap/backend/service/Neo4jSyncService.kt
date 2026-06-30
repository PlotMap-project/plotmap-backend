package com.plotmap.backend.service

import com.plotmap.backend.model.enum.ConnectionType
import com.plotmap.backend.repository.jpa.CharacterRepository
import com.plotmap.backend.repository.jpa.EventEdgeRepository
import com.plotmap.backend.repository.jpa.EventRepository
import com.plotmap.backend.repository.jpa.EventToCharacterRepository
import com.plotmap.backend.repository.jpa.StoryArcRepository
import com.plotmap.backend.repository.jpa.StoryArcToEventRepository
import org.neo4j.driver.Driver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class Neo4jSyncService(
    private val driver: Driver,
    private val eventRepository: EventRepository,
    private val eventEdgeRepository: EventEdgeRepository,
    private val characterRepository: CharacterRepository,
    private val eventToCharacterRepository: EventToCharacterRepository,
    private val storyArcRepository: StoryArcRepository,
    private val storyArcToEventRepository: StoryArcToEventRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val allowedRelationshipTypes = ConnectionType.entries.map { it.name }.toSet()

    fun syncProject(projectId: UUID) {
        log.info("Starting Neo4j sync for project {}", projectId)

        val events = eventRepository
            .findAllByProjectIdOrderByLevelAscOrderInLevelAsc(projectId)
        val edges = eventEdgeRepository.findAllByIdProject(projectId)
        val characters = characterRepository.findAllByProjectId(projectId)
        val eventToCharacters = eventToCharacterRepository.findAllByIdProject(projectId)
        val storyArcs = storyArcRepository.findAllByProjectId(projectId)
        val storyArcToEvents = storyArcToEventRepository.findAllByIdProject(projectId)

        driver.session().use { session ->
            session.executeWrite { tx ->
                tx.run(
                    "MATCH (n) WHERE n.projectId = \$projectId DETACH DELETE n",
                    mapOf("projectId" to projectId.toString())
                )
                if (events.isNotEmpty()) {
                    tx.run(
                        """
                        UNWIND ${'$'}rows AS row
                        CREATE (e:Event {
                            id: row.id,
                            projectId: row.projectId,
                            title: row.title,
                            description: row.description,
                            level: row.level,
                            orderInLevel: row.orderInLevel,
                            suggestedSystemRole: row.role,
                            status: row.status,
                            color: row.color
                        })
                        """.trimIndent(),
                        mapOf("rows" to events.map { event ->
                            mapOf(
                                "id" to event.id.toString(),
                                "projectId" to projectId.toString(),
                                "title" to event.title,
                                "description" to event.description,
                                "level" to event.level,
                                "orderInLevel" to event.orderInLevel,
                                "role" to (event.suggestedSystemRole?.name ?: "REGULAR"),
                                "status" to event.status.name,
                                "color" to (event.color ?: "")
                            )
                        })
                    )
                }
                if (characters.isNotEmpty()) {
                    tx.run(
                        """
                        UNWIND ${'$'}rows AS row
                        CREATE (c:Character {
                            id: row.id,
                            projectId: row.projectId,
                            name: row.name,
                            description: row.description,
                            role: row.role
                        })
                        """.trimIndent(),
                        mapOf("rows" to characters.map { char ->
                            mapOf(
                                "id" to char.id.toString(),
                                "projectId" to projectId.toString(),
                                "name" to char.name,
                                "description" to char.description,
                                "role" to char.role.name
                            )
                        })
                    )
                }
                if (storyArcs.isNotEmpty()) {
                    tx.run(
                        """
                        UNWIND ${'$'}rows AS row
                        CREATE (s:StoryArc {
                            id: row.id,
                            projectId: row.projectId,
                            title: row.title,
                            description: row.description
                        })
                        """.trimIndent(),
                        mapOf("rows" to storyArcs.map { arc ->
                            mapOf(
                                "id" to arc.id.toString(),
                                "projectId" to projectId.toString(),
                                "title" to arc.title,
                                "description" to arc.description
                            )
                        })
                    )
                }
                edges.forEach { edge ->
                    val relType = edge.type.name
                    check(relType in allowedRelationshipTypes) {
                        "Unexpected relationship type: $relType"
                    }
                    tx.run(
                        """
                        MATCH (a:Event {id: ${'$'}sourceId}), (b:Event {id: ${'$'}targetId})
                        CREATE (a)-[:$relType {description: ${'$'}desc}]->(b)
                        """.trimIndent(),
                        mapOf(
                            "sourceId" to edge.sourceEventId.toString(),
                            "targetId" to edge.targetEventId.toString(),
                            "desc" to edge.description
                        )
                    )
                }
                if (eventToCharacters.isNotEmpty()) {
                    tx.run(
                        """
                        UNWIND ${'$'}rows AS row
                        MATCH (c:Character {id: row.charId}), (e:Event {id: row.eventId})
                        CREATE (c)-[:PARTICIPATES_IN]->(e)
                        """.trimIndent(),
                        mapOf("rows" to eventToCharacters.map { etc ->
                            mapOf(
                                "charId" to etc.idCharacter.toString(),
                                "eventId" to etc.idEvent.toString()
                            )
                        })
                    )
                }
                if (storyArcToEvents.isNotEmpty()) {
                    tx.run(
                        """
                        UNWIND ${'$'}rows AS row
                        MATCH (s:StoryArc {id: row.arcId}), (e:Event {id: row.eventId})
                        CREATE (s)-[:CONTAINS]->(e)
                        """.trimIndent(),
                        mapOf("rows" to storyArcToEvents.map { sate ->
                            mapOf(
                                "arcId" to sate.idArc.toString(),
                                "eventId" to sate.idEvent.toString()
                            )
                        })
                    )
                }
            }
        }

        log.info(
            "Neo4j sync completed for project {}: {} events, {} characters, {} arcs, {} edges",
            projectId, events.size, characters.size, storyArcs.size, edges.size
        )
    }
}
