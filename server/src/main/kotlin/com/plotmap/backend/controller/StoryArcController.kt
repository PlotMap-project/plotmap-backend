package com.plotmap.backend.controller

import com.plotmap.backend.dto.request.CreateStoryArcRequest
import com.plotmap.backend.dto.request.UpdateStoryArcRequest
import com.plotmap.backend.dto.response.StoryArcDto
import com.plotmap.backend.service.StoryArcService
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
@RequestMapping("/api/v1/projects/{projectId}/story-arcs")
class StoryArcController(
    private val storyArcService: StoryArcService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createStoryArc(
        @PathVariable projectId: String,
        @RequestBody body: CreateStoryArcRequest
    ): StoryArcDto {
        val userId = getCurrentUserId()
        return storyArcService.createStoryArc(userId, UUID.fromString(projectId), body)
    }

    @PatchMapping("/{arcId}")
    fun updateStoryArc(
        @PathVariable projectId: String,
        @PathVariable arcId: String,
        @RequestBody body: UpdateStoryArcRequest
    ): StoryArcDto {
        val userId = getCurrentUserId()
        return storyArcService.updateStoryArc(
            userId, UUID.fromString(projectId), UUID.fromString(arcId), body
        )
    }

    @DeleteMapping("/{arcId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteStoryArc(
        @PathVariable projectId: String,
        @PathVariable arcId: String
    ) {
        val userId = getCurrentUserId()
        storyArcService.deleteStoryArc(
            userId, UUID.fromString(projectId), UUID.fromString(arcId)
        )
    }
}
