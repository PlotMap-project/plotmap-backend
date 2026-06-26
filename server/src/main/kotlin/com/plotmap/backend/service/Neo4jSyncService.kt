package com.plotmap.backend.service

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

    fun syncProject(projectId: UUID) {
        log.info("Starting Neo4j sync for project {}", projectId)

        val events = eventRepository.findAllByProjectIdOrderByLevelAscOrderInLevelAsc(projectId)
        val edges = eventEdgeRepository.findAllByIdProject(projectId)
        val characters = characterRepository.findAllByProjectId(projectId)
        val eventToCharacters = eventToCharacterRepository.findAllByIdProject(projectId)
        val storyArcs = storyArcRepository.findAllByProjectId(projectId)
        val storyArcToEvents = storyArcToEventRepository.findAllByIdProject(projectId)

        driver.session().use { session ->
            session.executeWrite { tx ->
                tx.run(
                    """
                    MATCH (n)
                    WHERE n.projectId = ${'$'}projectId
                    DETACH DELETE n
                    """.trimIndent(),
                    mapOf("projectId" to projectId.toString())
                )
                events.forEach { event ->
                    tx.run(
                        """
                        CREATE (e:Event {
                            id: ${'$'}id,
                            projectId: ${'$'}projectId,
                            title: ${'$'}title,
                            description: ${'$'}description,
                            level: ${'$'}level,
                            orderInLevel: ${'$'}orderInLevel,
                            suggestedSystemRole: ${'$'}role,
                            status: ${'$'}status,
                            color: ${'$'}color
                        })
                        """.trimIndent(),
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
                    )
                }
                characters.forEach { char ->
                    tx.run(
                        """
                        CREATE (c:Character {
                            id: ${'$'}id,
                            projectId: ${'$'}projectId,
                            name: ${'$'}name,
                            description: ${'$'}description,
                            role: ${'$'}role
                        })
                        """.trimIndent(),
                        mapOf(
                            "id" to char.id.toString(),
                            "projectId" to projectId.toString(),
                            "name" to char.name,
                            "description" to char.description,
                            "role" to char.role.name
                        )
                    )
                }
                storyArcs.forEach { arc ->
                    tx.run(
                        """
                        CREATE (s:StoryArc {
                            id: ${'$'}id,
                            projectId: ${'$'}projectId,
                            title: ${'$'}title,
                            description: ${'$'}description
                        })
                        """.trimIndent(),
                        mapOf(
                            "id" to arc.id.toString(),
                            "projectId" to projectId.toString(),
                            "title" to arc.title,
                            "description" to arc.description
                        )
                    )
                }
                edges.forEach { edge ->
                    val relType = edge.type.name
                    tx.run(
                        """
                        MATCH (a:Event {id: ${'$'}sourceId}), (b:Event {id: ${'$'}targetId})
                        CREATE (a)-[:${relType} {description: ${'$'}desc}]->(b)
                        """.trimIndent(),
                        mapOf(
                            "sourceId" to edge.sourceEventId.toString(),
                            "targetId" to edge.targetEventId.toString(),
                            "desc" to edge.description
                        )
                    )
                }
                eventToCharacters.forEach { etc ->
                    tx.run(
                        """
                        MATCH (c:Character {id: ${'$'}charId}), (e:Event {id: ${'$'}eventId})
                        CREATE (c)-[:PARTICIPATES_IN]->(e)
                        """.trimIndent(),
                        mapOf(
                            "charId" to etc.idCharacter.toString(),
                            "eventId" to etc.idEvent.toString()
                        )
                    )
                }
                storyArcToEvents.forEach { sate ->
                    tx.run(
                        """
                        MATCH (s:StoryArc {id: ${'$'}arcId}), (e:Event {id: ${'$'}eventId})
                        CREATE (s)-[:CONTAINS]->(e)
                        """.trimIndent(),
                        mapOf(
                            "arcId" to sate.idArc.toString(),
                            "eventId" to sate.idEvent.toString()
                        )
                    )
                }
            }
        }

        log.info("Neo4j sync completed for project {}", projectId)
    }
}
