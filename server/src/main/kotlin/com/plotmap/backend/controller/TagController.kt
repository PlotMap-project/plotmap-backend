package com.plotmap.backend.controller

import com.plotmap.backend.dto.request.CreateTagRequest
import com.plotmap.backend.dto.response.TagDto
import com.plotmap.backend.exception.InvalidCredentialsException
import com.plotmap.backend.service.TagService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/projects/{projectId}")
class TagController(
    private val tagService: TagService
) {

    @PostMapping("/tags")
    @ResponseStatus(HttpStatus.CREATED)
    fun createTag(
        request: HttpServletRequest,
        @PathVariable projectId: String,
        @RequestBody body: CreateTagRequest
    ): TagDto {
        val userId = getUserIdFromRequest(request)
        return tagService.createTag(userId, UUID.fromString(projectId), body)
    }

    @DeleteMapping("/tags/{tagId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTag(
        request: HttpServletRequest,
        @PathVariable projectId: String,
        @PathVariable tagId: String
    ) {
        val userId = getUserIdFromRequest(request)
        tagService.deleteTag(userId, UUID.fromString(projectId), UUID.fromString(tagId))
    }

    @PostMapping("/events/{eventId}/tags/{tagId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun assignTagToEvent(
        request: HttpServletRequest,
        @PathVariable projectId: String,
        @PathVariable eventId: String,
        @PathVariable tagId: String
    ) {
        val userId = getUserIdFromRequest(request)
        tagService.assignTagToEvent(
            userId,
            UUID.fromString(projectId),
            UUID.fromString(eventId),
            UUID.fromString(tagId)
        )
    }

    @DeleteMapping("/events/{eventId}/tags/{tagId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unassignTagFromEvent(
        request: HttpServletRequest,
        @PathVariable projectId: String,
        @PathVariable eventId: String,
        @PathVariable tagId: String
    ) {
        val userId = getUserIdFromRequest(request)
        tagService.unassignTagFromEvent(
            userId,
            UUID.fromString(projectId),
            UUID.fromString(eventId),
            UUID.fromString(tagId)
        )
    }

    private fun getUserIdFromRequest(request: HttpServletRequest): UUID {
        val userId = request.getAttribute("userId") as? String
            ?: throw InvalidCredentialsException("Missing or invalid token")
        return UUID.fromString(userId)
    }
}
