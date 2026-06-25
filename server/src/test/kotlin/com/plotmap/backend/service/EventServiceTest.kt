package com.plotmap.backend.service

import com.plotmap.backend.dto.request.CreateEventRequest
import com.plotmap.backend.model.entity.Project
import com.plotmap.backend.model.enum.ProjectType
import com.plotmap.backend.repository.jpa.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.Optional
import com.plotmap.backend.dto.request.UpdateEventRequest
import com.plotmap.backend.model.entity.Event
import com.plotmap.backend.model.enum.EventSource
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class EventServiceTest {

    @Mock lateinit var projectRepository: ProjectRepository
    @Mock lateinit var userToProjectRepository: UserToProjectRepository
    @Mock lateinit var eventRepository: EventRepository
    @Mock lateinit var characterRepository: CharacterRepository
    @Mock lateinit var storyArcRepository: StoryArcRepository
    @Mock lateinit var tagRepository: TagRepository
    @Mock lateinit var eventToCharacterRepository: EventToCharacterRepository
    @Mock lateinit var storyArcToEventRepository: StoryArcToEventRepository
    @Mock lateinit var eventToTagRepository: EventToTagRepository

    @InjectMocks
    lateinit var eventService: EventService

    @Test
    fun `createEvent should throw 400 BAD REQUEST if project is AI_GENERATED`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val request = CreateEventRequest(title = "New Event")

        val aiProject = Project(
            id = projectId,
            title = "Test",
            type = ProjectType.AI_GENERATED
        )

        whenever(userToProjectRepository.existsByIdUserAndIdProject(userId, projectId)).thenReturn(true)
        whenever(projectRepository.findById(projectId)).thenReturn(Optional.of(aiProject))

        val exception = assertThrows(ResponseStatusException::class.java) {
            eventService.createEvent(userId, projectId, request)
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        assertEquals("Events can be created manually only in MANUAL projects", exception.reason)
    }

    @Test
    fun `updateEvent should allow position change in AI project`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        val aiProject = Project(id = projectId, title = "AI", type = ProjectType.AI_GENERATED)
        val event = Event(
            id = eventId,
            projectId = projectId,
            title = "AI Event",
            source = EventSource.AI_GENERATED
        )

        whenever(userToProjectRepository.existsByIdUserAndIdProject(userId, projectId))
            .thenReturn(true)
        whenever(projectRepository.findById(projectId))
            .thenReturn(Optional.of(aiProject))
        whenever(eventRepository.findByIdAndProjectId(eventId, projectId))
            .thenReturn(event)
        whenever(eventRepository.save(any<Event>())).thenAnswer { it.getArgument<Event>(0) }
        whenever(eventToCharacterRepository.findAllByIdEvent(eventId)).thenReturn(emptyList())
        whenever(storyArcToEventRepository.findAllByIdEvent(eventId)).thenReturn(emptyList())
        whenever(eventToTagRepository.findAllByIdEvent(eventId)).thenReturn(emptyList())

        val request = UpdateEventRequest(
            customPositionX = 150.0,
            customPositionY = 250.0
        )

        val result = eventService.updateEvent(userId, projectId, eventId, request)

        assertEquals(150.0, result.customPositionX)
        assertEquals(250.0, result.customPositionY)
    }
}
