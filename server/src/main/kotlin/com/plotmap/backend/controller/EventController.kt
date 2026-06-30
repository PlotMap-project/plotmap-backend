package com.plotmap.backend.controller

import com.plotmap.backend.dto.request.CreateEventRequest
import com.plotmap.backend.dto.request.UpdateEventRequest
import com.plotmap.backend.dto.response.EventDto
import com.plotmap.backend.service.EventService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
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
) : BaseController() {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createEvent(
        request: HttpServletRequest,
        @PathVariable projectId: String,
        @RequestBody @Valid body: CreateEventRequest
    ): EventDto {
        return eventService.createEvent(
            userId = getUserIdFromRequest(request),
            projectId = UUID.fromString(projectId),
            request = body
        )
    }

    @PatchMapping("/{eventId}")
    fun updateEvent(
        request: HttpServletRequest,
        @PathVariable projectId: String,
        @PathVariable eventId: String,
        @RequestBody body: UpdateEventRequest
    ): EventDto {
        return eventService.updateEvent(
            userId = getUserIdFromRequest(request),
            projectId = UUID.fromString(projectId),
            eventId = UUID.fromString(eventId),
            request = body
        )
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteEvent(
        request: HttpServletRequest,
        @PathVariable projectId: String,
        @PathVariable eventId: String
    ) {
        eventService.deleteEvent(
            userId = getUserIdFromRequest(request),
            projectId = UUID.fromString(projectId),
            eventId = UUID.fromString(eventId)
        )
    }
}
