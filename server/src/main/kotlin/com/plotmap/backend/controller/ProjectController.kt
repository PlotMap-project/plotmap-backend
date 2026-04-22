package com.plotmap.backend.controller

import com.plotmap.backend.dto.request.CreateProjectRequest
import com.plotmap.backend.dto.request.GenerateProjectRequest
import com.plotmap.backend.dto.response.ProjectDetailResponse
import com.plotmap.backend.dto.response.ProjectResponse
import com.plotmap.backend.service.ProjectService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID


@RestController
@RequestMapping("/api/v1/projects")
class ProjectController(
    private val projectService: ProjectService
) {
    //GET /api/v1/projects
    @GetMapping
    fun getProjects(
        @RequestHeader("X-User-Id") userId: String
    ): List<ProjectResponse> {
        return projectService.getProjectsByUserId(UUID.fromString(userId))
    }

    //GET /api/v1/projects/{projectId}
    @GetMapping("/{projectId}")
    fun getProject(
        @RequestHeader("X-User-Id") userId: String,
        @PathVariable projectId: String
    ): ProjectDetailResponse {
        return projectService.getProjectById(
            userId = UUID.fromString(userId),
            projectId = UUID.fromString(projectId)
        )
    }

    //POST /api/v1/projects
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createProject(
        @RequestHeader("X-User-Id") userId: String,
        @RequestBody request: CreateProjectRequest
    ): ProjectResponse {
        return projectService.createProject(
            userId = UUID.fromString(userId),
            request = request
        )
    }

    //POST /api/v1/projects/generate
    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    fun createProjectWithGeneration(
        @RequestHeader("X-User-Id") userId: String,
        @RequestBody request: GenerateProjectRequest
    ): ProjectDetailResponse {
        return projectService.createProjectWithGeneration(
            userId = UUID.fromString(userId),
            request = request
        )
    }
}
