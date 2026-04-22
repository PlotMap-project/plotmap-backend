package com.plotmap.backend.dto.request

data class CreateProjectRequest(
    val title: String,
    val description: String = ""
)
