package com.plotmap.backend.controller

import com.plotmap.backend.dto.request.CreateEdgeRequest
import com.plotmap.backend.dto.request.UpdateEdgeRequest
import com.plotmap.backend.dto.response.ConnectionDto
import com.plotmap.backend.exception.InvalidCredentialsException
import com.plotmap.backend.service.EdgeService
import jakarta.servlet.http.HttpServletRequest
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
@RequestMapping("/api/v1/projects/{projectId}/edges")
class EdgeController(
    private val edgeService: EdgeService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createEdge(
        request: HttpServletRequest,
        @PathVariable projectId: String,
        @RequestBody body: CreateEdgeRequest
    ): ConnectionDto {
        val userId = getUserIdFromRequest(request)
        return edgeService.createEdge(userId, UUID.fromString(projectId), body)
    }

    @PatchMapping("/{edgeId}")
    fun updateEdge(
        request: HttpServletRequest,
        @PathVariable projectId: String,
        @PathVariable edgeId: String,
        @RequestBody body: UpdateEdgeRequest
    ): ConnectionDto {
        val userId = getUserIdFromRequest(request)
        return edgeService.updateEdge(
            userId, UUID.fromString(projectId), UUID.fromString(edgeId), body
        )
    }

    @DeleteMapping("/{edgeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteEdge(
        request: HttpServletRequest,
        @PathVariable projectId: String,
        @PathVariable edgeId: String
    ) {
        val userId = getUserIdFromRequest(request)
        edgeService.deleteEdge(userId, UUID.fromString(projectId), UUID.fromString(edgeId))
    }

    private fun getUserIdFromRequest(request: HttpServletRequest): UUID {
        val userId = request.getAttribute("userId") as? String
            ?: throw InvalidCredentialsException("Missing or invalid token")
        return UUID.fromString(userId)
    }
}
