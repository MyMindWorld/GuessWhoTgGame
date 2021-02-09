package ru.gromov.guessWhoTgBot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.PropertySource

@EntityScan(basePackages = ["ru.gromov.guessWhoTgBot.db.model"])
@PropertySource("classpath:application.properties")
@SpringBootApplication
open class SpringBot

fun main(args: Array<String>) {
    runApplication<SpringBot>(*args)
}
