package ru.gromov.guessWhoTgBot

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.logging.LogLevel
import com.natpryce.konfig.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import ru.gromov.guessWhoTgBot.service.GameService
import java.util.*

@Component
class MainBotLoop @Autowired constructor(
    private val gameService: GameService,
    @Value("\${bot.token:NONE}") private var botToken: String
) {


    @EventListener(value = [ApplicationStartedEvent::class])
    fun startBot() {

        if (botToken == "NONE") {
            throw IllegalStateException("Setup bot token!")
        }

        val bot = bot {


            token = botToken
            timeout = 30
            logLevel = LogLevel.All()

            dispatch {

                command("start") {
                    gameService.createUserAndShowMarkup(bot, message)
                }

                text("Create Game") {
                    gameService.createGame(bot, message)
                }

                command("createGame") {
                    gameService.createGame(bot, message)
                }

                text("Current Game") {
                    gameService.showCurrentGame(bot, message)
                }

                command("currentGame") {
                    gameService.showCurrentGame(bot, message)
                }


                text("Join Game") {
                    bot.sendMessage(
                        chatId = message.chat.id,
                        text = "Send join command you got from your friend to get started!"
                    )
                }

                command("join") {
                    val joinedArgs = args.joinToString()
                    if (joinedArgs.isBlank()) {
                        bot.sendMessage(
                            chatId = message.chat.id,
                            text = "You should send join code with the command!"
                        )
                        return@command
                    }
                    gameService.joinGame(bot, message, joinedArgs)
                }

                text("Start Game") {
                    gameService.startGame(bot, message)
                }

                command("startGame") {
                    gameService.startGame(bot, message)
                }

                command("riddle") {
                    val joinedArgs = args.joinToString(separator = " ")
                    if (joinedArgs.isBlank()) {
                        bot.sendMessage(
                            chatId = message.chat.id,
                            text = "You should send riddle with the command!"
                        )
                        return@command
                    }
                    if (gameService.verifyUserIsInGameAndItIsStarted(message)) {
                        gameService.handleUserRiddleResponse(bot, message, joinedArgs)
                    }
                }

                text("Leave Game") {
                    gameService.startGame(bot, message)
                }

                command("leaveGame") {
                    gameService.startGame(bot, message)
                }

                text("End Game") {
                    gameService.endGame(bot, message)
                }

                command("endGame") {
                    gameService.endGame(bot, message)
                }

                text("Кто мудаки?") {
                    bot.sendMessage(chatId = message.chat.id, text = "Все мудаки!")
                }

                telegramError {
                    println(error.getErrorMessage())
                }


            }
        }
        bot.startPolling()
    }

}

