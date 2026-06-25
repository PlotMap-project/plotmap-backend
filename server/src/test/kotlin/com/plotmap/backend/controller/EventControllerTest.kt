package com.plotmap.backend.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.plotmap.backend.dto.request.CreateEventRequest
import com.plotmap.backend.dto.response.EventDto
import com.plotmap.backend.exception.GlobalExceptionHandler
import com.plotmap.backend.service.EventService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

class EventControllerTest {

    private lateinit var eventService: EventService
    private lateinit var mockMvc: MockMvc
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        eventService = mock()
        mockMvc = MockMvcBuilders
            .standaloneSetup(EventController(eventService))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `POST event should return 201 on success`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        val eventDto = EventDto(
            id = UUID.randomUUID().toString(),
            title = "Test Event",
            description = "",
            suggestedSystemRole = "REGULAR",
            impactLevel = 5,
            status = "PLANNED",
            userNotes = "",
            level = 0,
            orderInLevel = 0,
            customPositionX = null,
            customPositionY = null,
            color = null,
            source = "USER_CREATED",
            sourceContext = "",
            characterIds = emptyList(),
            storyArcIds = emptyList(),
            tagIds = emptyList(),
            createdAt = Instant.now()
        )

        whenever(eventService.createEvent(eq(userId), eq(projectId), any<CreateEventRequest>()))
            .thenReturn(eventDto)

        mockMvc.perform(
            post("/api/v1/projects/$projectId/events")
                .requestAttr("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreateEventRequest(title = "Test Event")
                    )
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("Test Event"))
    }

    @Test
    fun `POST event in AI project should return 400`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        whenever(eventService.createEvent(eq(userId), eq(projectId), any<CreateEventRequest>()))
            .thenThrow(
                ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Events can be created manually only in MANUAL projects"
                )
            )

        mockMvc.perform(
            post("/api/v1/projects/$projectId/events")
                .requestAttr("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreateEventRequest(title = "Test")
                    )
                )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `DELETE event without request userId should return 401`() {
        val projectId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        mockMvc.perform(
            delete("/api/v1/projects/$projectId/events/$eventId")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
    }
}
