package com.plotmap.backend.service

import com.plotmap.backend.dto.request.AddChapterRequest
import com.plotmap.backend.exception.ProjectNotFoundException
import com.plotmap.backend.model.entity.GenerationJob
import com.plotmap.backend.model.entity.Project
import com.plotmap.backend.model.entity.ProjectChapter
import com.plotmap.backend.model.enum.GenerationMode
import com.plotmap.backend.model.enum.ProjectType
import com.plotmap.backend.repository.jpa.GenerationJobRepository
import com.plotmap.backend.repository.jpa.ProjectChapterRepository
import com.plotmap.backend.repository.jpa.ProjectRepository
import com.plotmap.backend.repository.jpa.UserToProjectRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ChapterServiceTest {

    @Mock lateinit var projectRepository: ProjectRepository
    @Mock lateinit var userToProjectRepository: UserToProjectRepository
    @Mock lateinit var projectChapterRepository: ProjectChapterRepository
    @Mock lateinit var generationJobRepository: GenerationJobRepository
    @Mock lateinit var generationJobProcessor: GenerationJobProcessor

    @InjectMocks
    lateinit var chapterService: ChapterService

    @Test
    fun `getChapters should throw if user has no access`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        whenever(userToProjectRepository.existsByIdUserAndIdProject(userId, projectId))
            .thenReturn(false)

        assertThrows<ProjectNotFoundException> {
            chapterService.getChapters(userId, projectId)
        }
    }

    @Test
    fun `getChapters should return chapters ordered by chapterOrder`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        val chapters = listOf(
            ProjectChapter(projectId = projectId, chapterOrder = 1, title = "Ch 1", text = "text1"),
            ProjectChapter(projectId = projectId, chapterOrder = 2, title = "Ch 2", text = "text2")
        )

        whenever(userToProjectRepository.existsByIdUserAndIdProject(userId, projectId))
            .thenReturn(true)
        whenever(projectChapterRepository.findAllByProjectIdOrderByChapterOrderAsc(projectId))
            .thenReturn(chapters)

        val result = chapterService.getChapters(userId, projectId)

        assertEquals(2, result.size)
        assertEquals(1, result[0].chapterOrder)
        assertEquals(2, result[1].chapterOrder)
    }

    @Test
    fun `addChapter should throw if text is blank`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        whenever(userToProjectRepository.existsByIdUserAndIdProject(userId, projectId))
            .thenReturn(true)

        assertThrows<IllegalArgumentException> {
            chapterService.addChapter(userId, projectId, AddChapterRequest(text = "   "))
        }
    }

    @Test
    fun `addChapter should save chapter with correct order`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        val manualProject = Project(
            id = projectId, title = "Test", type = ProjectType.MANUAL
        )

        whenever(userToProjectRepository.existsByIdUserAndIdProject(userId, projectId))
            .thenReturn(true)
        whenever(projectRepository.findById(projectId))
            .thenReturn(Optional.of(manualProject))
        whenever(projectChapterRepository.countByProjectId(projectId))
            .thenReturn(3)
        whenever(projectChapterRepository.save(any<ProjectChapter>())).thenAnswer { invocation ->
            invocation.getArgument<ProjectChapter>(0)
        }

        val result = chapterService.addChapter(
            userId, projectId,
            AddChapterRequest(title = "Ch 4", text = "Some text")
        )

        assertEquals(4, result.chapterOrder)
        assertEquals("Ch 4", result.title)
    }

    @Test
    fun `addChapter should create generation job for AI project`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        val aiProject = Project(
            id = projectId,
            title = "AI",
            type = ProjectType.AI_GENERATED
        )

        whenever(userToProjectRepository.existsByIdUserAndIdProject(userId, projectId))
            .thenReturn(true)
        whenever(projectRepository.findById(projectId))
            .thenReturn(Optional.of(aiProject))
        whenever(projectChapterRepository.countByProjectId(projectId))
            .thenReturn(1)
        whenever(projectChapterRepository.save(any<ProjectChapter>())).thenAnswer { invocation ->
            invocation.getArgument<ProjectChapter>(0)
        }
        whenever(generationJobRepository.save(any<GenerationJob>())).thenAnswer { invocation ->
            invocation.getArgument<GenerationJob>(0)
        }

        TransactionSynchronizationManager.initSynchronization()
        try {
            val result = chapterService.addChapter(
                userId,
                projectId,
                AddChapterRequest(title = "Ch 2", text = "New chapter text")
            )

            assertEquals(2, result.chapterOrder)
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }
}
