package com.plotmap.backend.controller

import com.plotmap.backend.exception.InvalidCredentialsException
import jakarta.servlet.http.HttpServletRequest
import java.util.UUID

abstract class BaseController {

    protected fun getUserIdFromRequest(request: HttpServletRequest): UUID {
        val userId = request.getAttribute("userId") as? String
            ?: throw InvalidCredentialsException("Missing or invalid token")
        return UUID.fromString(userId)
    }
}
