package com.plotmap.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "yandex.gpt")
data class YandexGptProperties(
    var apiKey: String = "",
    var folderId: String = "",
    var modelUri: String = "",
    var structuredOutputEnabled: Boolean = false,
    var maxTextLength: Int = 15000
)
