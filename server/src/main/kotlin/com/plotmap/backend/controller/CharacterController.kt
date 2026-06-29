package com.plotmap.backend.controller

import com.plotmap.backend.dto.request.CreateCharacterRequest
import com.plotmap.backend.dto.request.UpdateCharacterRequest
import com.plotmap.backend.dto.response.CharacterDto
import com.plotmap.backend.service.CharacterService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/projects/{projectId}/characters")
class CharacterController(
    private val characterService: CharacterService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCharacter(
        @PathVariable projectId: String,
        @RequestBody body: CreateCharacterRequest
    ): CharacterDto {
        val userId = getCurrentUserId()
        return characterService.createCharacter(userId, UUID.fromString(projectId), body)
    }

    @PatchMapping("/{characterId}")
    fun updateCharacter(
        @PathVariable projectId: String,
        @PathVariable characterId: String,
        @RequestBody body: UpdateCharacterRequest
    ): CharacterDto {
        val userId = getCurrentUserId()
        return characterService.updateCharacter(
            userId, UUID.fromString(projectId), UUID.fromString(characterId), body
        )
    }

    @DeleteMapping("/{characterId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCharacter(
        @PathVariable projectId: String,
        @PathVariable characterId: String
    ) {
        val userId = getCurrentUserId()
        characterService.deleteCharacter(
            userId, UUID.fromString(projectId), UUID.fromString(characterId)
        )
    }
}
