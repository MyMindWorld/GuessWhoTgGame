package ru.gromov.guessWhoTgBot.service

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import ru.gromov.guessWhoTgBot.db.model.Game
import ru.gromov.guessWhoTgBot.db.model.User
import ru.gromov.guessWhoTgBot.db.repository.UserRepository
import ru.gromov.guessWhoTgBot.utils.Messages

@Service
class UserService @Autowired constructor(
    private val userRepository: UserRepository
) {

    fun findUserOrCreateOne(message: Message): User =
        userRepository.findByIdEquals(message.from!!.id) ?: createUser(message)


    fun findUser(userId: Long, bot: Bot): User =
        userRepository.findById(userId)
            .orElseThrow {
                bot.sendMessage(
                    chatId = userId,
                    text = Messages.youNeedToRegister(),
                    parseMode = ParseMode.MARKDOWN_V2
                )
                IllegalStateException("User was not found in db!")
            }

    private fun createUser(message: Message): User {
        return userRepository.save(
            User(
                message.from!!.id,
                createUserName(message),
                message.chat.id,
                message.from!!.username ?: "",
                null
            )
        )
    }

    fun setCurrentGameForUser(user: User, game: Game) {
        user.currentGame = game
        userRepository.save(user)
    }

    fun setMakesRiddleFor(user: User, userForRiddle: User) {
        user.makesRiddleFor = userForRiddle
        userRepository.save(user)
    }

    fun setRiddle(user: User, riddle: String) {
        user.riddledPerson = riddle
        userRepository.save(user)
    }

    fun destroyAllInfoAboutGame(user: User) {
        user.currentGame = null
        user.makesRiddleFor = null
        user.riddledPerson = ""
        userRepository.save(user)
    }

    fun checkUserNotInGame(user: User, bot: Bot) {
        if (user.currentGame != null) {
            bot.sendMessage(
                chatId = user.chatId!!,
                text = Messages.alreadyInGameError(),
                parseMode = ParseMode.MARKDOWN_V2
            )
            throw IllegalStateException("User was already in game")
        }
    }


    private fun createUserName(message: Message): String =
        "${message.from!!.firstName} ${message.from?.lastName ?: ""}"

}
