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

    fun createGame(bot: Bot, userId: Long): Game {
        val user = userService.findUser(userId)

        userService.checkUserNotInGame(user, bot)

        val game = gameRepository.save(Game(creator = user))

        userService.setCurrentGameForUser(user, game)

        gameRepository.save(game)

        bot.sendMessage(
            chatId = user.chatId!!,
            text = createdGame(game),
            parseMode = ParseMode.MARKDOWN_V2
        )

        return game
    }

    fun findGameCreatedByUser(userId: Long): Game? {
        val user = userService.findUser(userId)

        return gameRepository.findByCreatorEquals(user)
    }

    fun findCurrentGameForUser(userId: Long): Game? {
        val user = userService.findUser(userId)

        return user.currentGame
    }

    fun joinGame(bot: Bot, userId: Long, joinLink: String): Game {
        val game = gameRepository.findByJoinCodeEquals(joinLink)

        if (game == null) {
            bot.sendMessage(
                chatId = userId,
                text = Messages.gameIsNotFoundOrLinkIncorrect(),
                parseMode = ParseMode.MARKDOWN_V2
            )
        }

        val user = userService.findUser(userId)

        userService.checkUserNotInGame(user, bot)

        userService.setCurrentGameForUser(user, game!!)

        bot.sendMessage(
            chatId = user.chatId!!,
            text = Messages.gameInfo(game),
            parseMode = ParseMode.MARKDOWN_V2
        )

        return game
    }


    fun createUserAndShowMarkup(bot: Bot, message: Message) {
        val user = userService.findUserOrCreateOne(message)

        val keyboardMarkup =
            KeyboardReplyMarkup(keyboard = generateUsersButton(), resizeKeyboard = true)

        bot.sendMessage(
            chatId = user.chatId!!,
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

    fun showCurrentGame(bot: Bot, userId: Long) {
        val currentGame = findCurrentGameForUser(userId)
        if (currentGame != null) {
            bot.sendMessage(
                chatId = userId,
                text = Messages.gameInfo(currentGame),
                parseMode = ParseMode.MARKDOWN_V2
            )
        } else {
            bot.sendMessage(
                chatId = userId,
                text = Messages.notInGameError(),
                parseMode = ParseMode.MARKDOWN_V2
            )
        }

    }

    fun handleUserRiddleResponse(bot: Bot, userId: Long, riddle: String) {
        bot.sendMessage(
            chatId = userId,
            text = riddleIsSet(),
            parseMode = ParseMode.MARKDOWN_V2
        )

        userService.setRiddle(userService.findUser(userId).makesRiddleFor!!, riddle)

        if (checkGameIsReadyToBeStarted(userId)) {
            startGameForPlayers(bot, userId)
        } else {
            bot.sendMessage(
                chatId = userId,
                text = Messages.weAreWaitingFor(findCurrentGameForUser(userId)!!),
                parseMode = ParseMode.MARKDOWN_V2
            )
        }


    }

    fun verifyUserIsInGameAndItIsStarted(userId: Long): Boolean {
        return findCurrentGameForUser(userId)!!.isStarted
    }


    fun startGame(bot: Bot, userId: Long) {


        val gameToBeStarted = findGameCreatedByUser(userId)
        if (gameToBeStarted == null) {
            bot.sendMessage(
                chatId = userId,
                text = Messages.notInGameError(),
                parseMode = ParseMode.MARKDOWN_V2
            )
        }


        if (gameToBeStarted!!.users.size < 2) {
            bot.sendMessage(
                chatId = userId,
                text = Messages.notEnoughPlayers()
            )
            return
        } else {
            bot.sendMessage(
                chatId = userId,
                text = Messages.startingGame()
            )
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


    private fun startGameForPlayers(bot: Bot, userId: Long) {
        val game = findCurrentGameForUser(userId)
        game!!.users.forEach { user ->
            bot.sendMessage(
                chatId = user.chatId!!,
                text = Messages.sendRiddledWithoutSelf(game, user),
                parseMode = ParseMode.MARKDOWN_V2,
                disableWebPagePreview = true
            )
        }

    }

    fun leaveGame(bot: Bot, userId: Long) {
        val user = userService.findUser(userId)
        val game = user.currentGame

        game?.let {
            bot.sendMessage(
                chatId = user.chatId!!,
                text = Messages.userLeavesGame(),
                parseMode = ParseMode.MARKDOWN_V2
            )
            endGameAndDestroy(game, bot, user)
        } ?: run {
            bot.sendMessage(
                chatId = user.chatId!!,
                text = Messages.notInGameError(),
                parseMode = ParseMode.MARKDOWN_V2
            )
        }


    }


    private fun checkGameIsReadyToBeStarted(userId: Long) =
        findCurrentGameForUser(userId)!!.isReadyToStart()

    fun endGame(bot: Bot, userId: Long) {
        val user = userService.findUser(userId)
        user.currentGame?.let {
            if (it.creator != user) {
                bot.sendMessage(
                    chatId = user.chatId!!,
                    text = Messages.itsNotYourGameToEnd(it.creator),
                    parseMode = ParseMode.MARKDOWN_V2
                )
            }
            endGameAndDestroy(it, bot, user)
        } ?: run {
            bot.sendMessage(
                chatId = user.chatId!!,
                text = Messages.notInGameError(),
                parseMode = ParseMode.MARKDOWN_V2
            )
        }

    }

    private fun endGameAndDestroy(
        game: Game,
        bot: Bot,
        user: User
    ) {
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
        gameRepository.delete(game)
    }
}
