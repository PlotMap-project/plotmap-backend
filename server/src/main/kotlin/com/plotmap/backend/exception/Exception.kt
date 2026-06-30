package com.plotmap.backend.exception

class EmailAlreadyExistsException(message: String) : RuntimeException(message)

class NameAlreadyExistsException(message: String) : RuntimeException(message)

class ContentFilteredException(message: String) : RuntimeException(message)

class InvalidCredentialsException(message: String) : RuntimeException(message)

class ProjectNotFoundException(message: String) : RuntimeException(message)
