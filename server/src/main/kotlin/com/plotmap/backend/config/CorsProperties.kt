package com.plotmap.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app.cors")
class CorsProperties {
    var allowedOrigins: List<String> = emptyList()
}
