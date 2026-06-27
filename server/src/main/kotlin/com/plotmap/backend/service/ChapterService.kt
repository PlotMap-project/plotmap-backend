package com.plotmap.backend.service

import com.plotmap.backend.dto.request.AddChapterRequest
import com.plotmap.backend.dto.response.AddChapterResponse
import com.plotmap.backend.dto.response.ChapterDetailDto
import com.plotmap.backend.dto.response.ChapterDto
import com.plotmap.backend.dto.response.JobStatusResponse
import com.plotmap.backend.exception.ProjectNotFoundException
import com.plotmap.backend.model.entity.GenerationJob
import com.plotmap.backend.model.entity.ProjectChapter
import com.plotmap.backend.model.enum.GenerationMode
import com.plotmap.backend.model.enum.JobStatus
import com.plotmap.backend.model.enum.ProjectType
import com.plotmap.backend.repository.jpa.GenerationJobRepository
import com.plotmap.backend.repository.jpa.ProjectChapterRepository
import com.plotmap.backend.repository.jpa.ProjectRepository
import com.plotmap.backend.repository.jpa.UserToProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.UUID

@Service
class ChapterService(
    private val projectRepository: ProjectRepository,
    private val userToProjectRepository: UserToProjectRepository,
    private val projectChapterRepository: ProjectChapterRepository,
    private val generationJobRepository: GenerationJobRepository,
    private val generationJobProcessor: GenerationJobProcessor
) {

    fun getChapters(userId: UUID, projectId: UUID): List<ChapterDto> {
        ensureUserHasAccessToProject(userId, projectId)

        return projectChapterRepository
            .findAllByProjectIdOrderByChapterOrderAsc(projectId)
            .map { it.toDto() }
    }

    fun getChapterById(userId: UUID, projectId: UUID, chapterId: UUID): ChapterDetailDto {
        ensureUserHasAccessToProject(userId, projectId)

        val chapter = projectChapterRepository.findById(chapterId)
            .orElseThrow { ProjectNotFoundException("Chapter $chapterId not found") }

        if (chapter.projectId != projectId) {
            throw ProjectNotFoundException("Chapter $chapterId not found in project $projectId")
        }

        return chapter.toDetailDto()
    }

    @Transactional
    fun addChapter(userId: UUID, projectId: UUID, request: AddChapterRequest): AddChapterResponse {
        ensureUserHasAccessToProject(userId, projectId)

        require(request.text.isNotBlank()) { "Chapter text must not be empty" }

        val project = projectRepository.findById(projectId)
            .orElseThrow { ProjectNotFoundException("Project $projectId not found") }

        require(project.type == ProjectType.AI_GENERATED) {
            "Cannot add chapters to a MANUAL project"
        }

        val nextOrder = projectChapterRepository.countByProjectId(projectId) + 1

        val chapter = projectChapterRepository.save(
            ProjectChapter(
                projectId = projectId,
                chapterOrder = nextOrder,
                title = request.title?.trim(),
                text = request.text
            )
        )

        val job = generationJobRepository.save(
            GenerationJob(
                projectId = projectId,
                chapterId = chapter.id,
                mode = GenerationMode.APPEND_CHAPTER,
                status = JobStatus.PENDING
            )
        )

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    generationJobProcessor.process(job.id)
                }
            }
        )

        return AddChapterResponse(
            chapter = chapter.toDto(),
            job = JobStatusResponse(
                jobId = job.id.toString(),
                projectId = job.projectId.toString(),
                status = job.status.name,
                errorMessage = job.errorMessage,
                result = null,
                createdAt = job.createdAt,
                updatedAt = job.updatedAt
            )
        )
    }

    private fun ensureUserHasAccessToProject(userId: UUID, projectId: UUID) {
        val exists = userToProjectRepository.existsByIdUserAndIdProject(userId, projectId)
        if (!exists) {
            throw ProjectNotFoundException("Project not found")
        }
    }

    private fun ProjectChapter.toDto(): ChapterDto {
        return ChapterDto(
            id = this.id.toString(),
            chapterOrder = this.chapterOrder,
            title = this.title,
            createdAt = this.createdAt
        )
    }

    private fun ProjectChapter.toDetailDto(): ChapterDetailDto {
        return ChapterDetailDto(
            id = this.id.toString(),
            chapterOrder = this.chapterOrder,
            title = this.title,
            text = this.text,
            createdAt = this.createdAt
        )
    }
}
