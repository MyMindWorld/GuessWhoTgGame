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

    private fun generateUsersButton(user: User): KeyboardReplyMarkup {
        val keyboard: List<List<KeyboardButton>>
        if (user.currentGame != null) {
            if (user.currentGame!!.creator == user && !user.currentGame!!.isStarted) {
                keyboard = listOf(
                    listOf(KeyboardButton("Current Game")),
                    listOf(KeyboardButton("Start Game")),
                    listOf(KeyboardButton("Leave Game"))
                )

            } else {
                keyboard = listOf(
                    listOf(KeyboardButton("Current Game")),
                    listOf(KeyboardButton("Leave Game"))
                )
            }

        } else {
            keyboard = listOf(
                listOf(KeyboardButton("Create Game"))
            )
        }

        return KeyboardReplyMarkup(
            keyboard = keyboard,
            resizeKeyboard = true
        )


    }

    // TODO CHECK SAVES ON EACH ENTITY
// todo 1. Сказать что ссылка кликабельна
// todo 3. Фикс баги с ответом кто кто был
// todo 4.  Добавить текст про конец игры
    fun createGame(bot: Bot, userId: Long): Game {
        val user = userService.findUser(userId).orElseThrow {
            bot.sendMessage(
                chatId = userId,
                text = Messages.youNeedToRegister(),
                parseMode = ParseMode.MARKDOWN_V2
            )
            IllegalStateException("User was not found in db!")
        }

        userService.checkUserNotInGame(user, bot)

        val game = gameRepository.save(Game(creator = user))

        userService.setCurrentGameForUser(user, game)

        gameRepository.save(game)

        bot.sendMessage(
            chatId = user.chatId!!,
            text = createdGame(game),
            parseMode = ParseMode.MARKDOWN_V2,
            replyMarkup = generateUsersButton(user)
        )

        return game
    }

    fun joinGame(bot: Bot, userId: Long, joinLink: String): Game {
        val user = userService.findUser(userId).orElseThrow {
            bot.sendMessage(
                chatId = userId,
                text = Messages.youNeedToRegister(),
                parseMode = ParseMode.MARKDOWN_V2
            )
            IllegalStateException("User ${userId} was not found in db!")
        }
        val game = gameRepository.findByJoinCodeEquals(joinLink)

        if (game == null) {
            bot.sendMessage(
                chatId = userId,
                text = Messages.gameIsNotFoundOrLinkIncorrect(),
                parseMode = ParseMode.MARKDOWN_V2
            )
            throw IllegalStateException("Game with joinLink ${joinLink} was not found in DB!")
        }

        if (game.isStarted || game.isFinished) {
            bot.sendMessage(
                chatId = userId,
                text = Messages.gameIsAlreadyStarted(),
                parseMode = ParseMode.MARKDOWN_V2
            )
            throw IllegalStateException("User ${userId} tried to join already started/finished game with joinLink ${joinLink}")
        }

        userService.checkUserNotInGame(user, bot)

        userService.setCurrentGameForUser(user, game)

        bot.sendMessage(
            chatId = user.chatId!!,
            text = Messages.gameInfo(game),
            parseMode = ParseMode.MARKDOWN_V2,
            replyMarkup = generateUsersButton(user)
        )

        return game
    }


    fun createUserAndShowMarkup(bot: Bot, message: Message) {
        val user = userService.findUserOrCreateOne(message)


        bot.sendMessage(
            chatId = user.chatId!!,
            text = Messages.welcomeMessage(user),
            parseMode = ParseMode.MARKDOWN_V2,
            replyMarkup = generateUsersButton(user)
        )
    }

    fun showCurrentGame(bot: Bot, userId: Long) {
        val user = userService.findUser(userId).orElseThrow {
            bot.sendMessage(
                chatId = userId,
                text = Messages.youNeedToRegister(),
                parseMode = ParseMode.MARKDOWN_V2
            )
            IllegalStateException("User was not found in db!")
        }
        val currentGame = user.currentGame
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
        val user = userService.findUser(userId).orElseThrow {
            bot.sendMessage(
                chatId = userId,
                text = Messages.youNeedToRegister(),
                parseMode = ParseMode.MARKDOWN_V2
            )
            IllegalStateException("User was not found in db!")
        }
        if (user.currentGame == null) {
            bot.sendMessage(
                chatId = user.chatId!!,
                text = Messages.notInGameError(),
                parseMode = ParseMode.MARKDOWN_V2
            )
            return
        }
        if (!user.currentGame!!.isStarted) {
            bot.sendMessage(
                chatId = user.chatId!!,
                text = Messages.notStarted(),
                parseMode = ParseMode.MARKDOWN_V2
            )
            return
        }

        if (user.makesRiddleFor == null) {
            bot.sendMessage(
                chatId = user.chatId!!,
                text = Messages.somethingWentWrong(),
                parseMode = ParseMode.MARKDOWN_V2
            )
            return
        }

        bot.sendMessage(
            chatId = userId,
            text = riddleIsSet(),
            parseMode = ParseMode.MARKDOWN_V2
        )

        userService.setRiddle(user.makesRiddleFor!!, riddle)

        if (user.currentGame!!.isReadyToStart()) {
            startGameForPlayers(bot, user)
        } else {
            bot.sendMessage(
                chatId = userId,
                text = Messages.weAreWaitingFor(user.currentGame!!),
                parseMode = ParseMode.MARKDOWN_V2
            )
        }


    }


    fun startGame(bot: Bot, userId: Long) {

        val user = userService.findUser(userId).orElseThrow {
            bot.sendMessage(
                chatId = userId,
                text = Messages.youNeedToRegister(),
                parseMode = ParseMode.MARKDOWN_V2
            )
            IllegalStateException("User was not found in db!")
        }
        var gameToBeStarted = user.currentGame
        if (gameToBeStarted == null) {
            bot.sendMessage(
                chatId = userId,
                text = Messages.notInGameError(),
                parseMode = ParseMode.MARKDOWN_V2
            )
            return
        }

        if (gameToBeStarted.isStarted) {
            bot.sendMessage(
                chatId = userId,
                text = Messages.gameIsAlreadyStarted(),
                parseMode = ParseMode.MARKDOWN_V2
            )
            return
        }


        if (gameToBeStarted.users.size < 2) {
            bot.sendMessage(
                chatId = userId,
                text = Messages.notEnoughPlayers()
            )
            return
        } else {
            bot.sendMessage(
                chatId = userId,
                text = Messages.startingGame(),
                replyMarkup = generateUsersButton(user)
            )
        }

        val userToWhoHeRiddlesFor = makeRiddlersMap(gameToBeStarted)

        gameToBeStarted.isStarted = true
//        gameToBeStarted = gameRepository.save(gameToBeStarted)

//        val gameUsers = gameToBeStarted.users.toMutableList()

        gameToBeStarted.users.forEach {
            val userForRiddle = userToWhoHeRiddlesFor[it]!! // todo npe
            userService.setMakesRiddleFor(it, userForRiddle)
            bot.sendMessage(
                chatId = it.chatId!!,
                text = makeRiddleFor(userForRiddle),
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


    private fun startGameForPlayers(bot: Bot, user: User) {
        val game = user.currentGame!!
        game.users.stream().forEach { player ->
            bot.sendMessage(
                chatId = player.chatId!!,
                text = Messages.sendRiddledWithoutSelf(game, player),
                parseMode = ParseMode.MARKDOWN_V2,
                disableWebPagePreview = true
            )
        }

    }

    fun leaveGame(bot: Bot, userId: Long) {
        val user = userService.findUser(userId).orElseThrow {
            bot.sendMessage(
                chatId = userId,
                text = Messages.youNeedToRegister(),
                parseMode = ParseMode.MARKDOWN_V2
            )
            IllegalStateException("User was not found in db!")
        }
        val game = user.currentGame

        game?.let {
            bot.sendMessage(
                chatId = user.chatId!!,
                text = Messages.userLeavesGame(),
                parseMode = ParseMode.MARKDOWN_V2
            )
            endGameAndDestroy(game, bot)
        } ?: run {
            bot.sendMessage(
                chatId = user.chatId!!,
                text = Messages.notInGameError(),
                parseMode = ParseMode.MARKDOWN_V2
            )
        }


    }


    fun endGame(bot: Bot, userId: Long) {
        val user = userService.findUser(userId).orElseThrow {
            bot.sendMessage(
                chatId = userId,
                text = Messages.youNeedToRegister(),
                parseMode = ParseMode.MARKDOWN_V2
            )
            IllegalStateException("User was not found in db!")
        }
        user.currentGame?.let {
            if (it.creator != user) {
                bot.sendMessage(
                    chatId = user.chatId!!,
                    text = Messages.itsNotYourGameToEnd(it.creator),
                    parseMode = ParseMode.MARKDOWN_V2
                )
            }
            endGameAndDestroy(it, bot)
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
        bot: Bot
    ) {
        game.isFinished = true
        game.users.forEach { player ->
            bot.sendMessage(
                chatId = player.chatId!!,
                text = Messages.gameEnded(player),
                parseMode = ParseMode.MARKDOWN_V2,
                disableWebPagePreview = true,
                replyMarkup = KeyboardReplyMarkup(
                    keyboard = listOf(
                        listOf(KeyboardButton("Create Game"))
                    ),
                    resizeKeyboard = true
                )
            )
            userService.destroyAllInfoAboutGame(player)
        }
    }
}
