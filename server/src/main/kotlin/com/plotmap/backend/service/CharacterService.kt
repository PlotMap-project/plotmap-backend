package com.plotmap.backend.service

import com.plotmap.backend.dto.request.CreateCharacterRequest
import com.plotmap.backend.dto.request.UpdateCharacterRequest
import com.plotmap.backend.dto.response.CharacterDto
import com.plotmap.backend.exception.ProjectNotFoundException
import com.plotmap.backend.model.entity.Character
import com.plotmap.backend.model.enum.CharacterRole
import com.plotmap.backend.repository.jpa.CharacterRepository
import com.plotmap.backend.repository.jpa.UserToProjectRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class CharacterService(
    private val userToProjectRepository: UserToProjectRepository,
    private val characterRepository: CharacterRepository
) {

    @Transactional
    fun createCharacter(
        userId: UUID,
        projectId: UUID,
        request: CreateCharacterRequest
    ): CharacterDto {
        ensureUserHasAccessToProject(userId, projectId)
        require(request.name.isNotBlank()) { "Name must not be blank" }

        val character = characterRepository.save(
            Character(
                projectId = projectId,
                name = request.name.trim(),
                description = request.description.trim(),
                role = parseCharacterRole(request.role),
                color = normalizeColor(request.color)
            )
        )

        return character.toDto()
    }

    @Transactional
    fun updateCharacter(
        userId: UUID,
        projectId: UUID,
        characterId: UUID,
        request: UpdateCharacterRequest
    ): CharacterDto {
        ensureUserHasAccessToProject(userId, projectId)

        val character = characterRepository.findByIdAndProjectId(characterId, projectId)
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND, "Character $characterId not found"
            )

        request.name?.let {
            require(it.isNotBlank()) { "Name must not be blank" }
            character.name = it.trim()
        }

        request.description?.let {
            character.description = it.trim()
        }

        request.role?.let {
            character.role = parseCharacterRole(it)
        }

        request.color?.let {
            character.color = normalizeColor(it)
        }

        character.updatedAt = Instant.now()
        val saved = characterRepository.save(character)
        return saved.toDto()
    }

    @Transactional
    fun deleteCharacter(userId: UUID, projectId: UUID, characterId: UUID) {
        ensureUserHasAccessToProject(userId, projectId)

        val character = characterRepository.findByIdAndProjectId(characterId, projectId)
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND, "Character $characterId not found"
            )

        characterRepository.delete(character)
    }

    private fun ensureUserHasAccessToProject(userId: UUID, projectId: UUID) {
        if (!userToProjectRepository.existsByIdUserAndIdProject(userId, projectId)) {
            throw ProjectNotFoundException("Project $projectId not found")
        }
    }

    private fun parseCharacterRole(value: String): CharacterRole {
        return try {
            CharacterRole.valueOf(value.trim())
        } catch (_: IllegalArgumentException) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Unknown character role: $value"
            )
        }
    }

    private fun normalizeColor(value: String?): String? {
        if (value.isNullOrBlank()) return null
        val trimmed = value.trim()
        if (!Regex("^#[0-9A-Fa-f]{6}$").matches(trimmed)) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invalid color format: $value"
            )
        }
        return trimmed
    }

    private fun Character.toDto(): CharacterDto {
        return CharacterDto(
            id = this.id.toString(),
            name = this.name,
            description = this.description,
            role = this.role.name,
            color = this.color
        )
    }
}
