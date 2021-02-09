package ru.gromov.guessWhoTgBot.testData

import com.github.kotlintelegrambot.entities.Chat
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.User

fun createMessage(userAndChat: Pair<User, Chat>) = Message(
    1L,
    from = userAndChat.first,
    chat = userAndChat.second,
    date = 1,
    text = "Create Game"
)

fun gameInfoMessage(userAndChat: Pair<User, Chat>) = Message(
    1L,
    from = userAndChat.first,
    chat = userAndChat.second,
    date = 1,
    text = "/currentGame"
)

fun startGameMessage(userAndChat: Pair<User, Chat>) = Message(
    1L,
    from = userAndChat.first,
    chat = userAndChat.second,
    date = 1,
    text = "/startGame"
)

fun joinMessage(userAndChat: Pair<User, Chat>, joinLink: String) = Message(
    1L,
    from = userAndChat.first,
    chat = userAndChat.second,
    date = 1,
    text = "/join $joinLink"
)
