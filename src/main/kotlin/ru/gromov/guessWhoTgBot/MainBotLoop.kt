package ru.gromov.guessWhoTgBot

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.telegramError
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.logging.LogLevel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import ru.gromov.guessWhoTgBot.service.GameService

@Component
class MainBotLoop @Autowired constructor(
    private val gameService: GameService,
    @Value("\${bot.token:not_set}") private var botToken: String
) {


    @EventListener(value = [ApplicationStartedEvent::class])
    fun startBot() {

        if (botToken.isBlank() || botToken == "not_set") {
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
                    gameService.createGame(bot, message.from!!.id)
                }

                command("createGame") {
                    gameService.createGame(bot, message.from!!.id)
                }

                text("Current Game") {
                    gameService.showCurrentGame(bot, message.from!!.id)
                }

                command("currentGame") {
                    gameService.showCurrentGame(bot, message.from!!.id)
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
                    gameService.joinGame(bot, message.from!!.id, joinedArgs)
                }

                text("Start Game") {
                    gameService.startGame(bot, message.from!!.id)
                }

                command("startGame") {
                    gameService.startGame(bot, message.from!!.id)
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
                    gameService.handleUserRiddleResponse(bot, message.from!!.id, joinedArgs)
                }

                text("Leave Game") {
                    gameService.leaveGame(bot, message.from!!.id)
                }

                command("leaveGame") {
                    gameService.leaveGame(bot, message.from!!.id)
                }

                text("End Game") {
                    gameService.endGame(bot, message.from!!.id)
                }

                command("endGame") {
                    gameService.endGame(bot, message.from!!.id)
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

