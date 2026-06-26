package com.plotmap.backend.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
class EmailAlreadyExistsException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.CONFLICT)
class NameAlreadyExistsException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.CONFLICT)
class ContentFilteredException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class InvalidCredentialsException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.NOT_FOUND)
class ProjectNotFoundException(message: String) : RuntimeException(message)
