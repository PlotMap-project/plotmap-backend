package com.plotmap.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories

@SpringBootApplication
@EnableJpaRepositories(basePackages = ["com.plotmap.backend.repository.jpa"])
@EnableNeo4jRepositories(basePackages = ["com.plotmap.backend.repository.neo4j"])
class BackendApplication

fun main(args: Array<String>) {
	runApplication<BackendApplication>(*args)
}
