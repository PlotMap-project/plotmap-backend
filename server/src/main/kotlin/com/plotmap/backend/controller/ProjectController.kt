package com.plotmap.backend.controller

import com.plotmap.backend.dto.request.CreateProjectRequest
import com.plotmap.backend.dto.request.GenerateProjectRequest
import com.plotmap.backend.dto.response.JobStatusResponse
import com.plotmap.backend.dto.response.ProjectDetailResponse
import com.plotmap.backend.dto.response.ProjectResponse
import com.plotmap.backend.exception.InvalidCredentialsException
import com.plotmap.backend.service.ProjectService
import com.plotmap.backend.dto.request.UpdateProjectRequest
import jakarta.servlet.http.HttpServletRequest
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
    //GET /api/v1/projects
    @GetMapping
    fun getProjects(request: HttpServletRequest): List<ProjectResponse> {
        val userId = getUserIdFromRequest(request)
        return projectService.getProjectsByUserId(userId)
    }

    //POST /api/v1/projects
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createProject(
        request: HttpServletRequest,
        @RequestBody body: CreateProjectRequest
    ): ProjectResponse {
        val userId = getUserIdFromRequest(request)
        return projectService.createProject(userId, body)
    }

    //GET /api/v1/projects/{projectId}
    @GetMapping("/{projectId}")
    fun getProject(
        request: HttpServletRequest,
        @PathVariable projectId: String
    ): ProjectDetailResponse {
        val userId = getUserIdFromRequest(request)
        return projectService.getProjectById(userId, UUID.fromString(projectId))
    }

    //POST /api/v1/projects/generate
    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun generateProject(
        request: HttpServletRequest,
        @RequestBody body: GenerateProjectRequest
    ): JobStatusResponse {
        val userId = getUserIdFromRequest(request)
        return projectService.createProjectWithGeneration(userId, body)
    }

    // PATCH /api/v1/projects/{projectId}
    @PatchMapping("/{projectId}")
    fun updateProject(
        request: HttpServletRequest,
        @PathVariable projectId: String,
        @RequestBody body: UpdateProjectRequest
    ): ProjectResponse {
        val userId = getUserIdFromRequest(request)
        return projectService.updateProject(userId, UUID.fromString(projectId), body)
    }

    // DELETE /api/v1/projects/{projectId}
    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteProject(
        request: HttpServletRequest,
        @PathVariable projectId: String
    ) {
        val userId = getUserIdFromRequest(request)
        projectService.deleteProject(userId, UUID.fromString(projectId))
    }

    private fun getUserIdFromRequest(request: HttpServletRequest): UUID {
        val userId = request.getAttribute("userId") as? String
            ?: throw InvalidCredentialsException("Missing or invalid token")
        return UUID.fromString(userId)
    }
}
