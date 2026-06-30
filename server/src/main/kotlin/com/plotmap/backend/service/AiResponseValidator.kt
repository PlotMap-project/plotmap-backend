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
        private val COLOR_REGEX = Regex("^#[0-9A-Fa-f]{6}$")
        private const val DEFAULT_COLOR = "#FAFAD2"
        private const val MAX_EDGE_DESCRIPTION_LENGTH = 500
        private const val MAX_SOURCE_CONTEXT_LENGTH = 500
    }

    fun validate(response: AiGraphResponse): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (response.events.isEmpty()) {
            errors.add("AI returned no events")
            return ValidationResult(isValid = false, errors = errors)
        }

        val eventIds = response.events.map { it.id }.toSet()
        if (eventIds.size != response.events.size) {
            errors.add("Duplicate event IDs found in AI response")
            return ValidationResult(isValid = false, errors = errors)
        }

        val sanitizedEvents = response.events.map { event ->
            val fixedRole = try {
                SystemEventRole.valueOf(event.suggestedSystemRole)
                event.suggestedSystemRole
            } catch (e: Exception) {
                warnings.add(
                    "Event ${event.id}: unknown role '${event.suggestedSystemRole}', replaced with REGULAR"
                )
                "REGULAR"
            }

            val fixedImpact = event.impactLevel.coerceIn(1, 10).also { fixed ->
                if (fixed != event.impactLevel) {
                    warnings.add(
                        "Event ${event.id}: impactLevel ${event.impactLevel} out of range, clamped to $fixed"
                    )
                }
            }

            val fixedColor = if (!event.color.isNullOrBlank() && COLOR_REGEX.matches(event.color)) {
                event.color
            } else {
                if (!event.color.isNullOrBlank()) {
                    warnings.add(
                        "Event ${event.id}: invalid color '${event.color}', replaced with $DEFAULT_COLOR"
                    )
                }
                DEFAULT_COLOR
            }

            val fixedLevel = event.level.coerceAtLeast(0)
            val fixedOrderInLevel = event.orderInLevel.coerceAtLeast(0)
            val fixedSourceContext = event.sourceContext.trim().take(MAX_SOURCE_CONTEXT_LENGTH)

            event.copy(
                suggestedSystemRole = fixedRole,
                impactLevel = fixedImpact,
                color = fixedColor,
                level = fixedLevel,
                orderInLevel = fixedOrderInLevel,
                sourceContext = fixedSourceContext
            )
        }

        val renumberedEvents = sanitizedEvents
            .groupBy { it.level }
            .values
            .flatMap { eventsInLevel ->
                eventsInLevel
                    .sortedBy { it.orderInLevel }
                    .mapIndexed { index, event -> event.copy(orderInLevel = index) }
            }

        val characterIds = response.characters.map { it.id }.toSet()
        val storyArcIds = response.storyArcs.map { it.id }.toSet()

        val cleanedEvents = renumberedEvents.map { event ->
            val validCharacterIds = event.characterIds.filter { charId ->
                (charId in characterIds).also { valid ->
                    if (!valid) warnings.add("Event ${event.id}: unknown characterId '$charId', removed")
                }
            }
            val validStoryArcIds = event.storyArcIds.filter { arcId ->
                (arcId in storyArcIds).also { valid ->
                    if (!valid) warnings.add("Event ${event.id}: unknown storyArcId '$arcId', removed")
                }
            }
            event.copy(characterIds = validCharacterIds, storyArcIds = validStoryArcIds)
        }

        val renumberedEventIds = renumberedEvents.map { it.id }.toSet()

        val validEdges = mutableListOf<AiEdgeDto>()
        response.edges.forEach { edge ->
            when {
                edge.sourceEventId == edge.targetEventId ->
                    warnings.add("Self-loop on event ${edge.sourceEventId}, skipped")

                edge.sourceEventId !in renumberedEventIds ->
                    warnings.add("Edge source '${edge.sourceEventId}' not found, skipped")

                edge.targetEventId !in renumberedEventIds ->
                    warnings.add("Edge target '${edge.targetEventId}' not found, skipped")

                else -> {
                    val fixedType = try {
                        ConnectionType.valueOf(edge.type)
                        edge.type
                    } catch (e: Exception) {
                        warnings.add(
                            "Edge ${edge.sourceEventId}->${edge.targetEventId}: " +
                                    "unknown type '${edge.type}', replaced with TEMPORAL"
                        )
                        "TEMPORAL"
                    }

                    validEdges.add(
                        edge.copy(
                            sourceEventId = edge.sourceEventId.trim(),
                            targetEventId = edge.targetEventId.trim(),
                            type = fixedType,
                            description = edge.description.trim().take(MAX_EDGE_DESCRIPTION_LENGTH)
                        )
                    )
                }
            }
        }

        if (warnings.isNotEmpty()) {
            log.warn("AI validation warnings ({}): {}", warnings.size, warnings.joinToString(" | "))
        }

        return ValidationResult(
            isValid = true,
            errors = errors,
            warnings = warnings,
            sanitized = response.copy(events = cleanedEvents, edges = validEdges)
        )
    }
}
