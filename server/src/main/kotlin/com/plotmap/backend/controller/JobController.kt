package com.plotmap.backend.controller

import com.plotmap.backend.dto.response.JobStatusResponse
import com.plotmap.backend.exception.InvalidCredentialsException
import com.plotmap.backend.repository.jpa.GenerationJobRepository
import com.plotmap.backend.repository.jpa.UserToProjectRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/api/v1/jobs")
class JobController(
    private val generationJobRepository: GenerationJobRepository,
    private val userToProjectRepository: UserToProjectRepository
) {
    @GetMapping("/{jobId}")
    fun getJobStatus(
        request: HttpServletRequest,
        @PathVariable jobId: String
    ): JobStatusResponse {
        val userId = getUserIdFromRequest(request)
        val parsedJobId = UUID.fromString(jobId)

        val job = generationJobRepository.findById(parsedJobId)
            .orElseThrow {
                ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Job $jobId not found"
                )
            }

        val hasAccess = userToProjectRepository.existsByIdUserAndIdProject(userId, job.projectId)
        if (!hasAccess) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Access denied"
            )
        }

        return JobStatusResponse(
            jobId = job.id.toString(),
            projectId = job.projectId.toString(),
            status = job.status.name,
            errorMessage = job.errorMessage,
            result = null,
            createdAt = job.createdAt,
            updatedAt = job.updatedAt
        )
    }

    private fun getUserIdFromRequest(request: HttpServletRequest): UUID {
        val userId = request.getAttribute("userId") as? String
            ?: throw InvalidCredentialsException("Missing or invalid token")
        return UUID.fromString(userId)
    }
}
