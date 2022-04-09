package ru.gromov.guessWhoTgBot.userStoryTests

import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.transaction.annotation.Transactional
import ru.gromov.guessWhoTgBot.db.model.User
import ru.gromov.guessWhoTgBot.service.BaseServiceTest
import ru.gromov.guessWhoTgBot.utils.Messages
import kotlin.random.Random

@Transactional
open class UserStoryTests : BaseServiceTest() {

    fun generateAndAddUsers(count: Int): HashMap<Int, User> {
        val usersMap: HashMap<Int, User> = hashMapOf()
        for (userId in 0..count) {
            val user = userRepository.save(
                User(
                    id = userId.toLong(),
                    name = "Name$userId",
                    chatId = userId.toLong()
                )
            )
            usersMap[userId] = user
        }
        return usersMap
    }

    fun getUsersToRiddledPerson(count: Int): HashMap<Int, User> {
        val usersMap: HashMap<Int, User> = hashMapOf()
        for (userId in 0..count) {
            val userWasRiddled = userRepository.findByIdEquals(userId.toLong())!!.riddledPerson
            val newUser = User(riddledPerson = userWasRiddled)
            usersMap[userId] = newUser
        }
        return usersMap
    }

    @Test
    fun `positive scenario`() {
        val usersCount = Random.nextInt(from = 10, until = 40)
        val usersMap: HashMap<Int, User> =
            generateAndAddUsers(usersCount) // users just clicked start

        var aegis = userRepository.save(
            User(
                id = 41,
                name = "Aegis",
                chatId = 55
            )
        )
        gameService.createGame(bot, aegis.id!!)

        var createdGame = gameRepository.findByCreatorEquals(aegis)

        assertThat(createdGame!!.creator).isEqualTo(aegis)
        assertThat(aegis.currentGame).isEqualTo(createdGame)


        Mockito.verify(bot, Mockito.times(1)).sendMessage(
            chatId = aegis.chatId!!,
            text = Messages.createdGame(createdGame),
            parseMode = ParseMode.MARKDOWN_V2,
            replyMarkup = KeyboardReplyMarkup(
                keyboard = listOf(
                    listOf(KeyboardButton("Current Game")),
                    listOf(KeyboardButton("Start Game")),
                    listOf(KeyboardButton("Leave Game")),
                ),
                resizeKeyboard = true
            )
        )

        // users clicking join with correct link
        for (userId in 0..usersCount) {
            val user = usersMap[userId]
            gameService.joinGame(bot, user!!.id!!, createdGame.joinCode)
            Mockito.verify(bot, Mockito.times(1)).sendMessage(
                chatId = userId.toLong(),
                text = Messages.gameInfo(createdGame),
                parseMode = ParseMode.MARKDOWN_V2,
                replyMarkup = KeyboardReplyMarkup(
                    keyboard = listOf(
                        listOf(KeyboardButton("Current Game")),
                        listOf(KeyboardButton("Leave Game")),
                    ),
                    resizeKeyboard = true
                )
            )
            assertThat(user.currentGame).isEqualTo(createdGame)
        }

        gameService.startGame(bot, aegis.id!!)

        // aegis got confirmation message
        bot.sendMessage(
            chatId = aegis.chatId!!,
            text = Messages.startingGame(),
            replyMarkup = KeyboardReplyMarkup(
                keyboard = listOf(
                    listOf(KeyboardButton("Current Game")),
                    listOf(KeyboardButton("Leave Game")),
                ),
                resizeKeyboard = true
            )
        )

        // all users should get message with person
        for (userId in 0..usersCount) {
            val user = usersMap[userId]
            val makesRiddleFor = user!!.makesRiddleFor!!
            Mockito.verify(bot, Mockito.times(1)).sendMessage(
                chatId = userId.toLong(),
                text = Messages.makeRiddleFor(makesRiddleFor),
                parseMode = ParseMode.MARKDOWN_V2
            )
        }

        // as well as creator
        Mockito.verify(bot, Mockito.times(1)).sendMessage(
            chatId = aegis.chatId!!.toLong(),
            text = Messages.makeRiddleFor(aegis.makesRiddleFor!!),
            parseMode = ParseMode.MARKDOWN_V2
        )

        // users sending their answers
        for (userId in 0..usersCount) {
            val user = usersMap[userId]
            gameService.handleUserRiddleResponse(bot, user!!.id!!, user.makesRiddleFor!!.name)
            Mockito.verify(bot, Mockito.times(1)).sendMessage(
                chatId = userId.toLong(),
                text = Messages.riddleIsSet(),
                parseMode = ParseMode.MARKDOWN_V2
            )
            Mockito.verify(bot, Mockito.times(1)).sendMessage(
                chatId = userId.toLong(),
                text = Messages.weAreWaitingFor(createdGame),
                parseMode = ParseMode.MARKDOWN_V2
            )
        }

        //last user send answer, game should start
        gameService.handleUserRiddleResponse(bot, aegis.id!!, aegis.makesRiddleFor!!.name)

        // all users get message with who is who
        for (userId in 0..usersCount) {
            val user = usersMap[userId]!!
            Mockito.verify(bot, Mockito.times(1)).sendMessage(
                chatId = userId.toLong(),
                text = Messages.sendRiddledWithoutSelf(createdGame, user),
                parseMode = ParseMode.MARKDOWN_V2,
                disableWebPagePreview = true
            )
        }
        // as well as creator
        Mockito.verify(bot, Mockito.times(1)).sendMessage(
            chatId = aegis.chatId!!.toLong(),
            text = Messages.sendRiddledWithoutSelf(createdGame, aegis),
            parseMode = ParseMode.MARKDOWN_V2,
            disableWebPagePreview = true
        )

        // saving all users info before it is destroyed
        val riddledUsersMap = getUsersToRiddledPerson(usersCount)
        val aegisWasRidded = aegis.riddledPerson

        // random user leaves game after mental breakdown
        gameService.leaveGame(
            bot,
            usersMap[Random.nextInt(from = 0, until = usersCount)]!!.id!!
        )
        // all users get message with who they were
        for (userId in 0..usersCount) {
            val user = riddledUsersMap[userId]!!
            Mockito.verify(bot, Mockito.times(1)).sendMessage(
                chatId = userId.toLong(),
                text = Messages.gameEnded(user),
                parseMode = ParseMode.MARKDOWN_V2,
                disableWebPagePreview = true,
                replyMarkup = KeyboardReplyMarkup(
                    keyboard = listOf(
                        listOf(KeyboardButton("Create Game"))
                    ),
                    resizeKeyboard = true
                )
            )
        }
        // as well as creator
        aegis.riddledPerson = aegisWasRidded
        Mockito.verify(bot, Mockito.times(1)).sendMessage(
            chatId = aegis.chatId!!.toLong(),
            text = Messages.gameEnded(aegis),
            parseMode = ParseMode.MARKDOWN_V2,
            disableWebPagePreview = true,
            replyMarkup = KeyboardReplyMarkup(
                keyboard = listOf(
                    listOf(KeyboardButton("Create Game"))
                ),
                resizeKeyboard = true
            )
        )


    }


}
