package com.plotmap.backend.controller

import com.plotmap.backend.dto.request.CreateProjectRequest
import com.plotmap.backend.dto.request.GenerateProjectRequest
import com.plotmap.backend.dto.request.UpdateProjectRequest
import com.plotmap.backend.dto.response.JobStatusResponse
import com.plotmap.backend.dto.response.ProjectDetailResponse
import com.plotmap.backend.dto.response.ProjectResponse
import com.plotmap.backend.service.ProjectService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
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
) : BaseController() {

    @GetMapping
    fun getProjects(request: HttpServletRequest): List<ProjectResponse> {
        return projectService.getProjectsByUserId(getUserIdFromRequest(request))
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createProject(
        request: HttpServletRequest,
        @RequestBody @Valid body: CreateProjectRequest
    ): ProjectResponse {
        return projectService.createProject(getUserIdFromRequest(request), body)
    }

    @GetMapping("/{projectId}")
    fun getProject(
        request: HttpServletRequest,
        @PathVariable projectId: String
    ): ProjectDetailResponse {
        return projectService.getProjectById(
            getUserIdFromRequest(request),
            UUID.fromString(projectId)
        )
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun generateProject(
        request: HttpServletRequest,
        @RequestBody body: GenerateProjectRequest
    ): JobStatusResponse {
        return projectService.createProjectWithGeneration(getUserIdFromRequest(request), body)
    }

    @PatchMapping("/{projectId}")
    fun updateProject(
        request: HttpServletRequest,
        @PathVariable projectId: String,
        @RequestBody @Valid body: UpdateProjectRequest
    ): ProjectResponse {
        return projectService.updateProject(
            getUserIdFromRequest(request),
            UUID.fromString(projectId),
            body
        )
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteProject(
        request: HttpServletRequest,
        @PathVariable projectId: String
    ) {
        projectService.deleteProject(
            getUserIdFromRequest(request),
            UUID.fromString(projectId)
        )
    }
}
