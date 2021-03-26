package org.indie.isabella.permahub

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PermaHubApplication

fun main(args: Array<String>) {
	runApplication<PermaHubApplication>(*args)
}
