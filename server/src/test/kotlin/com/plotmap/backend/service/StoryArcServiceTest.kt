package com.plotmap.backend.service

import com.plotmap.backend.dto.request.CreateStoryArcRequest
import com.plotmap.backend.repository.jpa.StoryArcRepository
import com.plotmap.backend.repository.jpa.UserToProjectRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class StoryArcServiceTest {

    @Mock lateinit var userToProjectRepository: UserToProjectRepository
    @Mock lateinit var storyArcRepository: StoryArcRepository

    @InjectMocks
    lateinit var storyArcService: StoryArcService

    @Test
    fun `createStoryArc should throw if title is blank`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        whenever(userToProjectRepository.existsByIdUserAndIdProject(userId, projectId))
            .thenReturn(true)

        val request = CreateStoryArcRequest(title = "   ")

        assertThrows<IllegalArgumentException> {
            storyArcService.createStoryArc(userId, projectId, request)
        }
    }
}
