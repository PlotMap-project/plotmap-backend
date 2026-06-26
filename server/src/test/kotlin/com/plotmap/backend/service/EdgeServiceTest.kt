package com.plotmap.backend.service

import com.plotmap.backend.dto.request.CreateEdgeRequest
import com.plotmap.backend.model.entity.Event
import com.plotmap.backend.model.entity.Project
import com.plotmap.backend.model.enum.ProjectType
import com.plotmap.backend.repository.jpa.EventEdgeRepository
import com.plotmap.backend.repository.jpa.EventRepository
import com.plotmap.backend.repository.jpa.ProjectRepository
import com.plotmap.backend.repository.jpa.UserToProjectRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.web.server.ResponseStatusException
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class EdgeServiceTest {

    @Mock lateinit var projectRepository: ProjectRepository
    @Mock lateinit var userToProjectRepository: UserToProjectRepository
    @Mock lateinit var eventRepository: EventRepository
    @Mock lateinit var eventEdgeRepository: EventEdgeRepository

    @InjectMocks
    lateinit var edgeService: EdgeService

    @Test
    fun `createEdge should throw 400 if project is AI_GENERATED`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        val aiProject = Project(id = projectId, title = "AI", type = ProjectType.AI_GENERATED)

        whenever(userToProjectRepository.existsByIdUserAndIdProject(userId, projectId))
            .thenReturn(true)
        whenever(projectRepository.findById(projectId))
            .thenReturn(Optional.of(aiProject))

        val request = CreateEdgeRequest(
            sourceEventId = UUID.randomUUID().toString(),
            targetEventId = UUID.randomUUID().toString()
        )

        val ex = assertThrows<ResponseStatusException> {
            edgeService.createEdge(userId, projectId, request)
        }

        assertEquals(400, ex.statusCode.value())
    }

    @Test
    fun `createEdge should throw 400 on self-loop`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        val manualProject = Project(id = projectId, title = "Manual", type = ProjectType.MANUAL)

        whenever(userToProjectRepository.existsByIdUserAndIdProject(userId, projectId))
            .thenReturn(true)
        whenever(projectRepository.findById(projectId))
            .thenReturn(Optional.of(manualProject))

        val request = CreateEdgeRequest(
            sourceEventId = eventId.toString(),
            targetEventId = eventId.toString()
        )

        val ex = assertThrows<ResponseStatusException> {
            edgeService.createEdge(userId, projectId, request)
        }

        assertEquals(400, ex.statusCode.value())
    }
}
