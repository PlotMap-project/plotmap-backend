package com.plotmap.backend.controller

import com.plotmap.backend.dto.request.CreateProjectRequest
import com.plotmap.backend.dto.request.GenerateProjectRequest
import com.plotmap.backend.dto.request.UpdateProjectRequest
import com.plotmap.backend.dto.response.JobStatusResponse
import com.plotmap.backend.dto.response.ProjectDetailResponse
import com.plotmap.backend.dto.response.ProjectResponse
import com.plotmap.backend.service.ProjectService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/projects")
class ProjectController(
    private val projectService: ProjectService
) {
    @GetMapping
    fun getProjects(): List<ProjectResponse> {
        val userId = getCurrentUserId()
        return projectService.getProjectsByUserId(userId)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createProject(
        @RequestBody body: CreateProjectRequest
    ): ProjectResponse {
        val userId = getCurrentUserId()
        return projectService.createProject(userId, body)
    }

    @GetMapping("/{projectId}")
    fun getProject(
        @PathVariable projectId: String
    ): ProjectDetailResponse {
        val userId = getCurrentUserId()
        return projectService.getProjectById(userId, UUID.fromString(projectId))
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun generateProject(
        @RequestBody body: GenerateProjectRequest
    ): JobStatusResponse {
        val userId = getCurrentUserId()
        return projectService.createProjectWithGeneration(userId, body)
    }

    @PatchMapping("/{projectId}")
    fun updateProject(
        @PathVariable projectId: String,
        @RequestBody body: UpdateProjectRequest
    ): ProjectResponse {
        val userId = getCurrentUserId()
        return projectService.updateProject(userId, UUID.fromString(projectId), body)
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteProject(
        @PathVariable projectId: String
    ) {
        val userId = getCurrentUserId()
        projectService.deleteProject(userId, UUID.fromString(projectId))
    }
}
