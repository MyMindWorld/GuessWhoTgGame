package ru.gromov.guessWhoTgBot.testData

import com.github.kotlintelegrambot.entities.Chat
import com.github.kotlintelegrambot.entities.User

val userAegis = User(id = 1L, firstName = "Aegis", lastName = "Person", isBot = false) to Chat(1, type = "private")
val userMarshall =
    User(id = 2L, firstName = "Marshall", lastName = "Broadson", isBot = false) to Chat(2, type = "private")
val userJockey = User(id = 3L, firstName = "Jockey", lastName = "Spider", isBot = false) to Chat(3, type = "private")
fun randomUser(id: Long) =
    User(id = id, firstName = "rand$id", lastName = "rand$id", isBot = false) to Chat(id, type = "private")

val botUser = User(id = 4L, firstName = "Bot", lastName = "", isBot = true) to Chat(4, type = "private")