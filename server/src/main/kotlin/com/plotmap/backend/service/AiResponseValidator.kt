package com.plotmap.backend.service

import com.plotmap.backend.dto.ai.AiEdgeDto
import com.plotmap.backend.dto.ai.AiGraphResponse
import com.plotmap.backend.model.enum.ConnectionType
import com.plotmap.backend.model.enum.SystemEventRole
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AiResponseValidator {

    private val log = LoggerFactory.getLogger(javaClass)

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList(),
        val sanitized: AiGraphResponse? = null
    )

    companion object {
        private val ALLOWED_COLORS = setOf(
            "#FAFAD2", "#FFEFD5", "#FFE4B5", "#FFDAB9", "#EEE8AA"
        )
        private const val DEFAULT_COLOR = "#FAFAD2"
    }

    fun validate(response: AiGraphResponse): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (response.events.isEmpty()) {
            errors.add("AI returned no events")
            return ValidationResult(
                isValid = false,
                errors = errors
            )
        }

        val eventIds = response.events.map { it.id }.toSet()
        if (eventIds.size != response.events.size) {
            errors.add("Duplicate event IDs found in AI response")
        }

        if (errors.isNotEmpty()) {
            return ValidationResult(isValid = false, errors = errors)
        }

        val sanitizedEvents = response.events.map { event ->
            var fixedRole = event.suggestedSystemRole
            try {
                SystemEventRole.valueOf(event.suggestedSystemRole)
            } catch (e: Exception) {
                warnings.add(
                    "Event ${event.id}: unknown role " +
                            "'${event.suggestedSystemRole}', replaced with REGULAR"
                )
                fixedRole = "REGULAR"
            }

            val fixedImpact = event.impactLevel.coerceIn(1, 10).also { fixed ->
                if (fixed != event.impactLevel) {
                    warnings.add(
                        "Event ${event.id}: impactLevel ${event.impactLevel} " +
                                "out of range, clamped to $fixed"
                    )
                }
            }

            val fixedColor = if (event.color in ALLOWED_COLORS) {
                event.color
            } else {
                warnings.add(
                    "Event ${event.id}: invalid color '${event.color}', " +
                            "replaced with $DEFAULT_COLOR"
                )
                DEFAULT_COLOR
            }

            val fixedLevel = event.level.coerceAtLeast(0).also { fixed ->
                if (fixed != event.level) {
                    warnings.add(
                        "Event ${event.id}: level ${event.level} " +
                                "is negative, clamped to $fixed"
                    )
                }
            }

            val fixedOrderInLevel = event.orderInLevel.coerceAtLeast(0).also { fixed ->
                if (fixed != event.orderInLevel) {
                    warnings.add(
                        "Event ${event.id}: orderInLevel ${event.orderInLevel} " +
                                "is negative, clamped to $fixed"
                    )
                }
            }

            event.copy(
                suggestedSystemRole = fixedRole,
                impactLevel = fixedImpact,
                color = fixedColor,
                level = fixedLevel,
                orderInLevel = fixedOrderInLevel
            )
        }

        val byLevel = sanitizedEvents.groupBy { it.level }
        val renumberedEvents = byLevel.values.flatMap { eventsInLevel ->
            eventsInLevel
                .sortedBy { it.orderInLevel }
                .mapIndexed { index, event ->
                    if (event.orderInLevel != index) {
                        warnings.add(
                            "Event ${event.id}: orderInLevel renumbered " +
                                    "from ${event.orderInLevel} to $index within level ${event.level}"
                        )
                    }
                    event.copy(orderInLevel = index)
                }
        }

        val validEdges = mutableListOf<AiEdgeDto>()
        response.edges.forEach { edge ->
            when {
                edge.sourceEventId == edge.targetEventId -> {
                    warnings.add(
                        "Self-loop on event ${edge.sourceEventId}, skipped"
                    )
                }
                edge.sourceEventId !in eventIds -> {
                    warnings.add(
                        "Edge source '${edge.sourceEventId}' not found, skipped"
                    )
                }
                edge.targetEventId !in eventIds -> {
                    warnings.add(
                        "Edge target '${edge.targetEventId}' not found, skipped"
                    )
                }
                else -> {
                    var fixedType = edge.type
                    try {
                        ConnectionType.valueOf(edge.type)
                    } catch (e: Exception) {
                        warnings.add(
                            "Edge ${edge.sourceEventId}->${edge.targetEventId}: " +
                                    "unknown type '${edge.type}', replaced with TEMPORAL"
                        )
                        fixedType = "TEMPORAL"
                    }

                    val fixedStrength = edge.strength.coerceIn(1, 10)

                    validEdges.add(
                        edge.copy(
                            type = fixedType,
                            strength = fixedStrength
                        )
                    )
                }
            }
        }

        val characterIds = response.characters.map { it.id }.toSet()
        val storyArcIds = response.storyArcs.map { it.id }.toSet()

        val cleanedEvents = renumberedEvents.map { event ->
            val validCharacterIds = event.characterIds.filter { charId ->
                if (charId !in characterIds) {
                    warnings.add(
                        "Event ${event.id}: unknown characterId '$charId', removed"
                    )
                    false
                } else {
                    true
                }
            }

            val validStoryArcIds = event.storyArcIds.filter { arcId ->
                if (arcId !in storyArcIds) {
                    warnings.add(
                        "Event ${event.id}: unknown storyArcId '$arcId', removed"
                    )
                    false
                } else {
                    true
                }
            }

            event.copy(
                characterIds = validCharacterIds,
                storyArcIds = validStoryArcIds
            )
        }

        if (response.events.size > 1) {
            val connectedIds = validEdges
                .flatMap { listOf(it.sourceEventId, it.targetEventId) }
                .toSet()
            val orphans = eventIds - connectedIds
            if (orphans.isNotEmpty()) {
                warnings.add("Orphan events (not connected to graph): $orphans")
            }
        }

        if (warnings.isNotEmpty()) {
            log.warn("AI validation warnings: {}", warnings.joinToString(" | "))
        }

        val sanitized = response.copy(
            events = cleanedEvents,
            edges = validEdges
        )

        return ValidationResult(
            isValid = true,
            errors = errors,
            warnings = warnings,
            sanitized = sanitized
        )
    }
}
