package com.plotmap.backend.service

import com.plotmap.backend.dto.request.AddChapterRequest
import com.plotmap.backend.dto.response.ChapterDto
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

    @Transactional
    fun addChapter(userId: UUID, projectId: UUID, request: AddChapterRequest): ChapterDto {
        ensureUserHasAccessToProject(userId, projectId)

        require(request.text.isNotBlank()) { "Chapter text must not be empty" }

        val project = projectRepository.findById(projectId)
            .orElseThrow { ProjectNotFoundException("Project $projectId not found") }

        val nextOrder = projectChapterRepository.countByProjectId(projectId) + 1

        val chapter = projectChapterRepository.save(
            ProjectChapter(
                projectId = projectId,
                chapterOrder = nextOrder,
                title = request.title?.trim(),
                text = request.text
            )
        )

        if (project.type == ProjectType.AI_GENERATED) {
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
        }

        return chapter.toDto()
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
}
