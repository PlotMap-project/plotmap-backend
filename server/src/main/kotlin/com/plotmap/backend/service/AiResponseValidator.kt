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

            event.copy(
                suggestedSystemRole = fixedRole,
                impactLevel = fixedImpact
            )
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
            events = sanitizedEvents,
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
