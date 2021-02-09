package ru.gromov.guessWhoTgBot.service

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import ru.gromov.guessWhoTgBot.db.model.Game
import ru.gromov.guessWhoTgBot.db.model.User
import ru.gromov.guessWhoTgBot.db.repository.GameRepository
import ru.gromov.guessWhoTgBot.utils.Messages
import ru.gromov.guessWhoTgBot.utils.Messages.ResponseMessages.createdGame
import ru.gromov.guessWhoTgBot.utils.Messages.ResponseMessages.makeRiddleFor
import ru.gromov.guessWhoTgBot.utils.Messages.ResponseMessages.riddleIsSet

@Service
class GameService @Autowired constructor(
    private val userService: UserService,
    private val gameRepository: GameRepository
) {

    fun createGame(bot: Bot, message: Message): Game {
        val user = userService.findUserOrCreateOne(message)

        userService.checkUserNotInGame(user, bot, message)

        val game = gameRepository.save(Game(creator = user))

        userService.setCurrentGameForUser(user, game)

        gameRepository.save(game)

        bot.sendMessage(
            chatId = message.chat.id,
            text = createdGame(game),
            parseMode = ParseMode.MARKDOWN_V2
        )

        return game
    }

    fun findGameFromUser(message: Message): Game {
        val user = userService.findUser(message)

        return gameRepository.findByCreatorEquals(user)
    }

    fun findUsersCurrentGame(message: Message): Game? {
        val user = userService.findUser(message)

        return user.currentGame
    }

    fun joinGame(bot: Bot, message: Message, joinLink: String): Game {
        val game = gameRepository.findByJoinCodeEquals(joinLink)

        val user = userService.findUserOrCreateOne(message)

        userService.checkUserNotInGame(user, bot, message)

        userService.setCurrentGameForUser(user, game)

        bot.sendMessage(
            chatId = message.chat.id,
            text = Messages.gameInfo(game),
            parseMode = ParseMode.MARKDOWN_V2
        )

        return game
    }


    fun createUserAndShowMarkup(bot: Bot, message: Message) {
        val user = userService.findUserOrCreateOne(message)

        val keyboardMarkup = KeyboardReplyMarkup(keyboard = generateUsersButton(), resizeKeyboard = true)

        bot.sendMessage(
            chatId = message.chat.id,
            text = Messages.welcomeMessage(user),
            parseMode = ParseMode.MARKDOWN_V2,
            replyMarkup = keyboardMarkup
        )
    }

    fun generateUsersButton(): List<List<KeyboardButton>> {
        return listOf(
            listOf(KeyboardButton("Create Game")),
            listOf(KeyboardButton("Current Game")),
            listOf(KeyboardButton("Start Game")),
            listOf(KeyboardButton("Leave Game"))
        )
    }

    fun showCurrentGame(bot: Bot, message: Message) {
        val currentGame = findUsersCurrentGame(message)
        if (currentGame != null) {
            bot.sendMessage(
                chatId = message.chat.id,
                text = Messages.gameInfo(currentGame),
                parseMode = ParseMode.MARKDOWN_V2
            )
        } else {
            bot.sendMessage(
                chatId = message.chat.id,
                text = Messages.notInGameError(),
                parseMode = ParseMode.MARKDOWN_V2
            )
        }

    }

    fun handleUserRiddleResponse(bot: Bot, message: Message, riddle: String) {
        bot.sendMessage(chatId = message.chat.id, text = riddleIsSet(), parseMode = ParseMode.MARKDOWN_V2)

        userService.setRiddle(userService.findUser(message).makesRiddleFor!!, riddle)

        if (checkGameIsReadyToBeStarted(bot, message)) {
            startGameForPlayers(bot, message)
        } else {
            bot.sendMessage(
                chatId = message.chat.id,
                text = Messages.weAreWaitingFor(findUsersCurrentGame(message)!!),
                parseMode = ParseMode.MARKDOWN_V2
            )
        }


    }

    fun verifyUserIsInGameAndItIsStarted(message: Message): Boolean {
        return findUsersCurrentGame(message)!!.isStarted
    }


    fun startGame(bot: Bot, message: Message) {
        bot.sendMessage(
            chatId = message.chat.id,
            text = Messages.startingGame()
        )

        val gameToBeStarted = findGameFromUser(message)

        if (gameToBeStarted.users.size < 2) {
            bot.sendMessage(
                chatId = message.chat.id,
                text = Messages.notEnoughPlayers()
            )
            return
        }

        val riddlerToRiddled = makeRiddlersMap(gameToBeStarted)

        gameToBeStarted.isStarted = true
        gameRepository.save(gameToBeStarted)

        gameToBeStarted.users.forEach {
            userService.setMakesRiddleFor(it, riddlerToRiddled[it]!!)
            bot.sendMessage(
                chatId = it.chatId!!,
                text = makeRiddleFor(riddlerToRiddled[it]!!),
                parseMode = ParseMode.MARKDOWN_V2
            )
        }
    }

    fun makeRiddlersMap(game: Game): HashMap<User, User> {
        val riddlerToRiddled = hashMapOf<User, User>()

        val usersList = game.users.toList().shuffled()

        usersList.forEach { user ->
            var index = usersList.indexOf(user) + 1
            if (index >= usersList.size) {
                index = 0
            }
            riddlerToRiddled[user] = usersList[index]

        }
        return riddlerToRiddled
    }


    private fun startGameForPlayers(bot: Bot, message: Message) {
        val game = findUsersCurrentGame(message)
        game!!.users.forEach { user ->
            bot.sendMessage(
                chatId = user.chatId!!,
                text = Messages.sendRiddledWithoutSelf(game, user),
                parseMode = ParseMode.MARKDOWN_V2,
                disableWebPagePreview = true
            )
        }

    }

    private fun leaveGame(bot: Bot, message: Message) {
        TODO()
    }


    private fun checkGameIsReadyToBeStarted(bot: Bot, message: Message) =
        findUsersCurrentGame(message)!!.isReadyToStart()

    fun endGame(bot: Bot, message: Message) {
        val user = userService.findUser(message)
        val game = findUsersCurrentGame(message)
        if (game!!.creator != user) {
            bot.sendMessage(
                chatId = user.chatId!!,
                text = Messages.itsNotYourGameToEnd(game.creator),
                parseMode = ParseMode.MARKDOWN_V2
            )
        }
        game.isFinished = true
        game.users.forEach {
            bot.sendMessage(
                chatId = it.chatId!!,
                text = Messages.gameEnded(user),
                parseMode = ParseMode.MARKDOWN_V2,
                disableWebPagePreview = true
            )
            userService.destroyAllInfoAboutGame(it)
        }
    }
}