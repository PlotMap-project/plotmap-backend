package com.plotmap.backend.controller

import com.plotmap.backend.dto.request.CreateEventRequest
import com.plotmap.backend.dto.request.UpdateEventRequest
import com.plotmap.backend.dto.response.EventDto
import com.plotmap.backend.service.EventService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/projects/{projectId}/events")
class EventController(
    private val eventService: EventService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createEvent(
        @PathVariable projectId: String,
        @RequestBody body: CreateEventRequest
    ): EventDto {
        val userId = getCurrentUserId()
        return eventService.createEvent(
            userId = userId,
            projectId = UUID.fromString(projectId),
            request = body
        )
    }

    @PatchMapping("/{eventId}")
    fun updateEvent(
        @PathVariable projectId: String,
        @PathVariable eventId: String,
        @RequestBody body: UpdateEventRequest
    ): EventDto {
        val userId = getCurrentUserId()
        return eventService.updateEvent(
            userId = userId,
            projectId = UUID.fromString(projectId),
            eventId = UUID.fromString(eventId),
            request = body
        )
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteEvent(
        @PathVariable projectId: String,
        @PathVariable eventId: String
    ) {
        val userId = getCurrentUserId()
        eventService.deleteEvent(
            userId = userId,
            projectId = UUID.fromString(projectId),
            eventId = UUID.fromString(eventId)
        )
    }
}
