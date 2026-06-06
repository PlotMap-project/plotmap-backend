package com.plotmap.backend.controller

import com.plotmap.backend.dto.response.JobStatusResponse
import com.plotmap.backend.exception.InvalidCredentialsException
import com.plotmap.backend.service.JobStore
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
    private val jobStore: JobStore
) {
    // GET /api/v1/jobs/{jobId}
    @GetMapping("/{jobId}")
    fun getJobStatus(
        request: HttpServletRequest,
        @PathVariable jobId: String
    ): JobStatusResponse {
        val userId = getUserIdFromRequest(request)
        val parsedJobId = UUID.fromString(jobId)

        val job = jobStore.get(parsedJobId)
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Job $jobId not found"
            )

        if (job.userId != userId) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Access denied"
            )
        }

        val result = jobStore.getResult(parsedJobId)

        return JobStatusResponse(
            jobId = job.id.toString(),
            projectId = job.projectId.toString(),
            status = job.status.name,
            errorMessage = job.errorMessage,
            result = result,
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
