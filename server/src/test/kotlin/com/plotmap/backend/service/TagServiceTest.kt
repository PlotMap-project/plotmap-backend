package com.plotmap.backend.service

import com.plotmap.backend.dto.request.CreateTagRequest
import com.plotmap.backend.exception.ProjectNotFoundException
import com.plotmap.backend.repository.jpa.EventRepository
import com.plotmap.backend.repository.jpa.EventToTagRepository
import com.plotmap.backend.repository.jpa.TagRepository
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
class TagServiceTest {

    @Mock lateinit var userToProjectRepository: UserToProjectRepository
    @Mock lateinit var tagRepository: TagRepository
    @Mock lateinit var eventRepository: EventRepository
    @Mock lateinit var eventToTagRepository: EventToTagRepository

    @InjectMocks
    lateinit var tagService: TagService

    @Test
    fun `createTag should throw if user has no access`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        whenever(userToProjectRepository.existsByIdUserAndIdProject(userId, projectId))
            .thenReturn(false)

        val request = CreateTagRequest(name = "test tag")

        assertThrows<ProjectNotFoundException> {
            tagService.createTag(userId, projectId, request)
        }
    }

    @Test
    fun `createTag should throw if name is blank`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        whenever(userToProjectRepository.existsByIdUserAndIdProject(userId, projectId))
            .thenReturn(true)

        val request = CreateTagRequest(name = "   ")

        assertThrows<IllegalArgumentException> {
            tagService.createTag(userId, projectId, request)
        }
    }
}
