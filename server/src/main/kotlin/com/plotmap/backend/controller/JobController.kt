package com.plotmap.backend.controller

import com.plotmap.backend.dto.response.JobStatusResponse
import com.plotmap.backend.service.JobService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/jobs")
class JobController(
    private val jobService: JobService
) {
    @GetMapping("/{jobId}")
    fun getJobStatus(
        @PathVariable jobId: String
    ): JobStatusResponse {
        val userId = getCurrentUserId()
        return jobService.getJobStatus(userId, UUID.fromString(jobId))
    }
}
