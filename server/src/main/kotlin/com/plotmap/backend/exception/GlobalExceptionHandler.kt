package com.plotmap.backend.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

data class ErrorResponse(
    val error: String,
    val message: String
)

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(EmailAlreadyExistsException::class)
    fun handleEmailExists(e: EmailAlreadyExistsException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse("EMAIL_ALREADY_EXISTS", e.message ?: "Email already exists"))
    }

    @ExceptionHandler(NameAlreadyExistsException::class)
    fun handleNameExists(e: NameAlreadyExistsException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse("NAME_ALREADY_EXISTS", e.message ?: "Name already exists"))
    }

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(e: InvalidCredentialsException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse("INVALID_CREDENTIALS", e.message ?: "Invalid credentials"))
    }

    @ExceptionHandler(ProjectNotFoundException::class)
    fun handleProjectNotFound(e: ProjectNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse("PROJECT_NOT_FOUND", e.message ?: "Project not found"))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("BAD_REQUEST", e.message ?: "Invalid request"))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(e: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception happened", e)

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    "INTERNAL_ERROR",
                    e.message ?: "Internal error, god left us"
                )
            )
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(e: ResponseStatusException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(e.statusCode)
            .body(
                ErrorResponse(
                    e.statusCode.toString(),
                    e.reason ?: "Request failed"
                )
            )
    }
}
