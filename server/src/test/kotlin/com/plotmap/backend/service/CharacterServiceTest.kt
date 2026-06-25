package com.plotmap.backend.service

import com.plotmap.backend.dto.request.CreateCharacterRequest
import com.plotmap.backend.repository.jpa.CharacterRepository
import com.plotmap.backend.repository.jpa.UserToProjectRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class CharacterServiceTest {

    @Mock lateinit var userToProjectRepository: UserToProjectRepository
    @Mock lateinit var characterRepository: CharacterRepository

    @InjectMocks
    lateinit var characterService: CharacterService

    @Test
    fun `createCharacter should throw if name is blank`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        whenever(userToProjectRepository.existsByIdUserAndIdProject(userId, projectId))
            .thenReturn(true)

        val request = CreateCharacterRequest(name = "  ", description = "test")

        assertThrows<IllegalArgumentException> {
            characterService.createCharacter(userId, projectId, request)
        }
    }

    @Test
    fun `createCharacter should throw 400 for invalid color`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        whenever(userToProjectRepository.existsByIdUserAndIdProject(userId, projectId))
            .thenReturn(true)

        val request = CreateCharacterRequest(
            name = "Hero",
            color = "not-a-color"
        )

        assertThrows<ResponseStatusException> {
            characterService.createCharacter(userId, projectId, request)
        }
    }
}
