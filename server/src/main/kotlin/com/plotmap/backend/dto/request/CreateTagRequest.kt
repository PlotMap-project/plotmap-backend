package com.plotmap.backend.dto.request

data class CreateTagRequest(
    val name: String,
    val color: String? = null
)
