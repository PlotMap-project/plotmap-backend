package com.plotmap.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "yandex.gpt")
class YandexGptProperties {
    lateinit var apiKey: String
    lateinit var folderId: String
    lateinit var modelUri: String
    lateinit var agentId: String
    lateinit var organization: String
}
