package com.plotmap.backend.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.plotmap.backend.dto.request.CreateProjectRequest
import com.plotmap.backend.dto.response.ProjectResponse
import com.plotmap.backend.exception.GlobalExceptionHandler
import com.plotmap.backend.service.ProjectService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import java.util.UUID

class ProjectControllerTest {

    private lateinit var projectService: ProjectService
    private lateinit var mockMvc: MockMvc
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        projectService = mock()
        mockMvc = MockMvcBuilders
            .standaloneSetup(ProjectController(projectService))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `GET projects should return 200 with request userId`() {
        val userId = UUID.randomUUID()

        whenever(projectService.getProjectsByUserId(eq(userId)))
            .thenReturn(emptyList())

        mockMvc.perform(
            get("/api/v1/projects")
                .requestAttr("userId", userId.toString())
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `GET projects should return 401 if request has no userId`() {
        mockMvc.perform(get("/api/v1/projects"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
    }

    @Test
    fun `POST projects should return 201 with valid data`() {
        val userId = UUID.randomUUID()

        val response = ProjectResponse(
            id = UUID.randomUUID().toString(),
            title = "New Project",
            type = "MANUAL",
            description = "Desc",
            createdAt = Instant.now()
        )

        whenever(projectService.createProject(eq(userId), any<CreateProjectRequest>()))
            .thenReturn(response)

        mockMvc.perform(
            post("/api/v1/projects")
                .requestAttr("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreateProjectRequest(
                            title = "New Project",
                            description = "Desc"
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("New Project"))
            .andExpect(jsonPath("$.type").value("MANUAL"))
    }
}
