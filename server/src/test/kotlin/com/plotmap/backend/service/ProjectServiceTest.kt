package com.plotmap.backend.service

import com.plotmap.backend.config.YandexGptProperties
import com.plotmap.backend.dto.request.CreateProjectRequest
import com.plotmap.backend.dto.request.UpdateProjectRequest
import com.plotmap.backend.exception.ProjectNotFoundException
import com.plotmap.backend.model.entity.Project
import com.plotmap.backend.model.entity.UserToProject
import com.plotmap.backend.model.enum.ProjectType
import com.plotmap.backend.repository.jpa.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ProjectServiceTest {

    @Mock lateinit var projectRepository: ProjectRepository
    @Mock lateinit var userToProjectRepository: UserToProjectRepository
    @Mock lateinit var projectChapterRepository: ProjectChapterRepository
    @Mock lateinit var generationJobRepository: GenerationJobRepository
    @Mock lateinit var eventRepository: EventRepository
    @Mock lateinit var eventEdgeRepository: EventEdgeRepository
    @Mock lateinit var characterRepository: CharacterRepository
    @Mock lateinit var eventToCharacterRepository: EventToCharacterRepository
    @Mock lateinit var storyArcRepository: StoryArcRepository
    @Mock lateinit var storyArcToEventRepository: StoryArcToEventRepository
    @Mock lateinit var tagRepository: TagRepository
    @Mock lateinit var eventToTagRepository: EventToTagRepository
    @Mock lateinit var generationJobProcessor: GenerationJobProcessor
    @Mock lateinit var yandexGptProperties: YandexGptProperties

    @InjectMocks
    lateinit var projectService: ProjectService

    @Test
    fun `createProject should create manual project successfully`() {
        val userId = UUID.randomUUID()
        val request = CreateProjectRequest(title = "My Story", description = "A great story")

        whenever(projectRepository.save(any<Project>())).thenAnswer { invocation ->
            invocation.getArgument<Project>(0)
        }
        whenever(userToProjectRepository.save(any<UserToProject>())).thenAnswer { invocation ->
            invocation.getArgument<UserToProject>(0)
        }

        val result = projectService.createProject(userId, request)

        assertEquals("My Story", result.title)
        assertEquals("MANUAL", result.type)
        assertEquals("A great story", result.description)
    }

    @Test
    fun `createProject should throw if title is blank`() {
        val userId = UUID.randomUUID()
        val request = CreateProjectRequest(title = "   ", description = "desc")

        assertThrows<IllegalArgumentException> {
            projectService.createProject(userId, request)
        }
    }

    @Test
    fun `getProjectsByUserId should return list of projects`() {
        val userId = UUID.randomUUID()
        val projects = listOf(
            Project(title = "Project 1", type = ProjectType.MANUAL),
            Project(title = "Project 2", type = ProjectType.AI_GENERATED)
        )

        whenever(projectRepository.findAllByUserId(userId)).thenReturn(projects)

        val result = projectService.getProjectsByUserId(userId)

        assertEquals(2, result.size)
        assertEquals("Project 1", result[0].title)
        assertEquals("Project 2", result[1].title)
    }

    @Test
    fun `updateProject should throw if user has no access`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        whenever(userToProjectRepository.existsByIdUserAndIdProject(userId, projectId))
            .thenReturn(false)

        assertThrows<ProjectNotFoundException> {
            projectService.updateProject(
                userId, projectId,
                UpdateProjectRequest(title = "New Title")
            )
        }
    }

    @Test
    fun `updateProject should update title and description`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val project = Project(id = projectId, title = "Old", type = ProjectType.MANUAL)

        whenever(userToProjectRepository.existsByIdUserAndIdProject(userId, projectId))
            .thenReturn(true)
        whenever(projectRepository.findById(projectId))
            .thenReturn(Optional.of(project))
        whenever(projectRepository.save(any<Project>())).thenAnswer { invocation ->
            invocation.getArgument<Project>(0)
        }

        val result = projectService.updateProject(
            userId, projectId,
            UpdateProjectRequest(title = "New Title", description = "New Desc")
        )

        assertEquals("New Title", result.title)
        assertEquals("New Desc", result.description)
    }

    @Test
    fun `deleteProject should throw if user has no access`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        whenever(userToProjectRepository.existsByIdUserAndIdProject(userId, projectId))
            .thenReturn(false)

        assertThrows<ProjectNotFoundException> {
            projectService.deleteProject(userId, projectId)
        }
    }
}
