package com.plotmap.backend.service

import com.plotmap.backend.dto.response.JobStatusResponse
import com.plotmap.backend.exception.InvalidCredentialsException
import com.plotmap.backend.repository.jpa.GenerationJobRepository
import com.plotmap.backend.repository.jpa.UserToProjectRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class JobService(
    private val generationJobRepository: GenerationJobRepository,
    private val userToProjectRepository: UserToProjectRepository
) {
    fun getJobStatus(userId: UUID, jobId: UUID): JobStatusResponse {
        val job = generationJobRepository.findById(jobId)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Job $jobId not found")
            }

        val hasAccess = userToProjectRepository.existsByUserIdAndProjectId(userId, job.projectId)
        if (!hasAccess) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
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
}
