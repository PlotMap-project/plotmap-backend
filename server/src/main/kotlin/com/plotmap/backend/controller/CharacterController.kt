package com.plotmap.backend.controller

import com.plotmap.backend.dto.request.CreateCharacterRequest
import com.plotmap.backend.dto.request.UpdateCharacterRequest
import com.plotmap.backend.dto.response.CharacterDto
import com.plotmap.backend.exception.InvalidCredentialsException
import com.plotmap.backend.service.CharacterService
import jakarta.servlet.http.HttpServletRequest
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
        request: HttpServletRequest,
        @PathVariable projectId: String,
        @RequestBody body: CreateCharacterRequest
    ): CharacterDto {
        val userId = getUserIdFromRequest(request)
        return characterService.createCharacter(userId, UUID.fromString(projectId), body)
    }

    @PatchMapping("/{characterId}")
    fun updateCharacter(
        request: HttpServletRequest,
        @PathVariable projectId: String,
        @PathVariable characterId: String,
        @RequestBody body: UpdateCharacterRequest
    ): CharacterDto {
        val userId = getUserIdFromRequest(request)
        return characterService.updateCharacter(
            userId, UUID.fromString(projectId), UUID.fromString(characterId), body
        )
    }

    @DeleteMapping("/{characterId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCharacter(
        request: HttpServletRequest,
        @PathVariable projectId: String,
        @PathVariable characterId: String
    ) {
        val userId = getUserIdFromRequest(request)
        characterService.deleteCharacter(
            userId, UUID.fromString(projectId), UUID.fromString(characterId)
        )
    }

    private fun getUserIdFromRequest(request: HttpServletRequest): UUID {
        val userId = request.getAttribute("userId") as? String
            ?: throw InvalidCredentialsException("Missing or invalid token")
        return UUID.fromString(userId)
    }
}
