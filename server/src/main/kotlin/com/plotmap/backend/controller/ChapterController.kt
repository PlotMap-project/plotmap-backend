package com.plotmap.backend.controller

import com.plotmap.backend.dto.request.AddChapterRequest
import com.plotmap.backend.dto.request.UpdateChapterRequest
import com.plotmap.backend.dto.response.AddChapterResponse
import com.plotmap.backend.dto.response.ChapterDetailDto
import com.plotmap.backend.dto.response.ChapterDto
import com.plotmap.backend.exception.InvalidCredentialsException
import com.plotmap.backend.service.ChapterService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/projects/{projectId}/chapters")
class ChapterController(
    private val chapterService: ChapterService
) {

    @GetMapping
    fun getChapters(
        request: HttpServletRequest,
        @PathVariable projectId: String
    ): List<ChapterDto> {
        val userId = getUserIdFromRequest(request)
        return chapterService.getChapters(userId, UUID.fromString(projectId))
    }

    @GetMapping("/{chapterId}")
    fun getChapterById(
        request: HttpServletRequest,
        @PathVariable projectId: String,
        @PathVariable chapterId: String
    ): ChapterDetailDto {
        val userId = getUserIdFromRequest(request)
        return chapterService.getChapterById(
            userId,
            UUID.fromString(projectId),
            UUID.fromString(chapterId)
        )
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun addChapter(
        request: HttpServletRequest,
        @PathVariable projectId: String,
        @RequestBody body: AddChapterRequest
    ): AddChapterResponse {
        val userId = getUserIdFromRequest(request)
        return chapterService.addChapter(userId, UUID.fromString(projectId), body)
    }

    @PutMapping("/{chapterId}")
    fun updateChapter(
        request: HttpServletRequest,
        @PathVariable projectId: String,
        @PathVariable chapterId: String,
        @RequestBody body: UpdateChapterRequest
    ): AddChapterResponse {
        val userId = getUserIdFromRequest(request)
        return chapterService.updateChapter(
            userId,
            UUID.fromString(projectId),
            UUID.fromString(chapterId),
            body
        )
    }

    private fun getUserIdFromRequest(request: HttpServletRequest): UUID {
        val userId = request.getAttribute("userId") as? String
            ?: throw InvalidCredentialsException("Missing or invalid token")
        return UUID.fromString(userId)
    }
}
