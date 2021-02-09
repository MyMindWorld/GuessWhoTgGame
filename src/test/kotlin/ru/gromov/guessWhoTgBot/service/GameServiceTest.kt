package ru.gromov.guessWhoTgBot.service

import com.github.kotlintelegrambot.entities.ParseMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.verify
import org.springframework.transaction.annotation.Transactional
import ru.gromov.guessWhoTgBot.db.model.Game
import ru.gromov.guessWhoTgBot.db.model.User
import ru.gromov.guessWhoTgBot.testData.*
import ru.gromov.guessWhoTgBot.utils.Messages
import ru.gromov.guessWhoTgBot.utils.Messages.ResponseMessages.alreadyInGameError


open class GameServiceTest : BaseServiceTest() {


    @Nested
    @Transactional
    open class WhenGameIsCreated : BaseServiceTest() {
        private var createdGame: Game = Game()

        @BeforeEach
        fun createGameForAegis() {
            val aegis = userRepository.save(
                User(
                    id = userAegis.first.id,
                    name = userAegis.first.firstName,
                    chatId = userAegis.second.id
                )
            )
            val marshal = userRepository.save(
                User(
                    id = userMarshall.first.id,
                    name = userMarshall.first.firstName,
                    chatId = userMarshall.second.id
                )
            )
            val jockey = userRepository.save(
                User(
                    id = userJockey.first.id,
                    name = userJockey.first.firstName,
                    chatId = userJockey.second.id
                )
            )
            createdGame = gameRepository.save(
                Game(
                    creator = aegis
                )
            )
        }

        @Test
        @Transactional
        open fun `should reply with error message on create game`() {
            assertThrows<IllegalStateException> {
                gameService.createGame(bot, userAegis.first.id)
            }

            verify(bot, atLeastOnce()).sendMessage(
                chatId = userAegis.second.id,
                text = alreadyInGameError(),
                parseMode = ParseMode.MARKDOWN_V2
            )
        }

        @Test
        @Transactional
        open fun `should reply with error message on join game`() {
            assertThrows<IllegalStateException> {
                gameService.joinGame(
                    bot,
                    userAegis.first.id,
                    createdGame.joinCode
                )
            }

            verify(bot, atLeastOnce()).sendMessage(
                chatId = userAegis.second.id,
                text = alreadyInGameError(),
                parseMode = ParseMode.MARKDOWN_V2
            )
        }
    }

    @Nested
    @Transactional
    open class WhenGameIsJoinedByUsers : BaseServiceTest() {
        private var createdGame: Game = Game()

        @BeforeEach
        open fun createGameForAegisAndJoinByMarshallAndJockey() {
            val aegis = userRepository.save(
                User(
                    id = userAegis.first.id,
                    name = userAegis.first.firstName,
                    chatId = userAegis.second.id
                )
            )
            val marshal = userRepository.save(
                User(
                    id = userMarshall.first.id,
                    name = userMarshall.first.firstName,
                    chatId = userMarshall.second.id
                )
            )
            val jockey = userRepository.save(
                User(
                    id = userJockey.first.id,
                    name = userJockey.first.firstName,
                    chatId = userJockey.second.id
                )
            )
            createdGame = gameRepository.save(
                Game(
                    creator = aegis
                )
            )
            aegis.currentGame = createdGame
            marshal.currentGame = createdGame
            jockey.currentGame = createdGame

        }

        @Test
        fun `should have correct game info in DB`() {
            val game = gameService.findCurrentGameForUser(userAegis.first.id)
            assertThat(game!!.users.size).isEqualTo(3)
        }

        @Test
        fun `should send correct current game info`() {
            val gameInDb = gameRepository.findByJoinCodeEquals(createdGame.joinCode)!!

            gameService.showCurrentGame(bot, userAegis.first.id)

            verify(bot, atLeastOnce()).sendMessage(
                chatId = userAegis.second.id,
                text = Messages.gameInfo(gameInDb),
                parseMode = ParseMode.MARKDOWN_V2
            )

            assertThat(Messages.gameInfo(gameInDb)).contains(gameInDb.joinCode.split("-"))

            assertThat(Messages.gameInfo(gameInDb)).contains(userAegis.second.id.toString())
            assertThat(Messages.gameInfo(gameInDb)).contains(userMarshall.second.id.toString())
            assertThat(Messages.gameInfo(gameInDb)).contains(userJockey.second.id.toString())
        }

        @Test
        fun `should make correct riddler to riddled map`() {
            val gameInDb = gameRepository.findByJoinCodeEquals(createdGame.joinCode)!!

            val createdMap = gameService.makeRiddlersMap(gameInDb)

            assertThat(createdMap.values.size).isEqualTo(3)
            assertThat(createdMap.values).doesNotHaveDuplicates()
            assertThat(createdMap).allSatisfy { key, value -> assertThat(key).isNotEqualTo(value) }
        }

        @Test
        fun `should make big riddler to riddled map`() {
            for (userId in 100L..110L) {
                userService.findUserOrCreateOne(createMessage(randomUser(userId)))
                gameService.joinGame(
                    bot,
                    userId, createdGame.joinCode
                )
            }
            val gameInDb = gameRepository.findByJoinCodeEquals(createdGame.joinCode)!!

            val createdMap = gameService.makeRiddlersMap(gameInDb)

            assertThat(createdMap.values.size).isEqualTo(gameInDb.users.size)
            assertThat(createdMap.values).doesNotHaveDuplicates()
            assertThat(createdMap).allSatisfy { key, value -> assertThat(key).isNotEqualTo(value) }
        }

        @Test
        fun `should ask everybody to make a riddle on game start and set makesRiddleFor in DB`() {
            gameService.startGame(bot, userAegis.first.id)

            val gameInDb = gameRepository.findByJoinCodeEquals(createdGame.joinCode)!!

            gameInDb.users.forEach {
                verify(bot, atLeastOnce()).sendMessage(
                    chatId = it.chatId!!,
                    text = Messages.makeRiddleFor(it.makesRiddleFor!!),
                    parseMode = ParseMode.MARKDOWN_V2
                )
            }


            assertThat(userService.findUser(userAegis.first.id).makesRiddleFor).isNotNull
            assertThat(userService.findUser(userMarshall.first.id).makesRiddleFor).isNotNull
            assertThat(userService.findUser(userJockey.first.id).makesRiddleFor).isNotNull
        }
    }

    @Nested
    @Transactional
    open class WhenGameIsStarted : BaseServiceTest() {
        private var createdGame: Game = Game()

        @BeforeEach
        fun createGameForAegisAndJoinByMarshallAndJockey() {
            val aegis = userRepository.save(
                User(
                    id = userAegis.first.id,
                    name = userAegis.first.firstName,
                    chatId = userAegis.second.id
                )
            )
            val marshal = userRepository.save(
                User(
                    id = userMarshall.first.id,
                    name = userMarshall.first.firstName,
                    chatId = userMarshall.second.id
                )
            )
            val jockey = userRepository.save(
                User(
                    id = userJockey.first.id,
                    name = userJockey.first.firstName,
                    chatId = userJockey.second.id
                )
            )
            createdGame = gameRepository.save(
                Game(
                    creator = aegis,
                    users = arrayListOf(aegis, marshal, jockey),
                    isStarted = true
                )
            )
            aegis.currentGame = createdGame
            marshal.currentGame = createdGame
            jockey.currentGame = createdGame
        }

        @Test
        fun `should have correct game info in DB`() {
            val game = gameService.findCurrentGameForUser(userAegis.first.id)
            assertThat(game!!.users.size).isEqualTo(3)
        }

        @Test
        fun `should send correct current game info`() {
            val gameInDb = gameRepository.findByJoinCodeEquals(createdGame.joinCode)!!

            gameService.showCurrentGame(bot, userAegis.first.id)

            verify(bot, atLeastOnce()).sendMessage(
                chatId = userAegis.second.id,
                text = Messages.gameInfo(gameInDb),
                parseMode = ParseMode.MARKDOWN_V2
            )

            assertThat(Messages.gameInfo(gameInDb)).contains(gameInDb.joinCode.split("-"))

            assertThat(Messages.gameInfo(gameInDb)).contains(userAegis.second.id.toString())
            assertThat(Messages.gameInfo(gameInDb)).contains(userMarshall.second.id.toString())
            assertThat(Messages.gameInfo(gameInDb)).contains(userJockey.second.id.toString())
        }

        @Test
        fun `should make correct riddler to riddled map`() {
            val gameInDb = gameRepository.findByJoinCodeEquals(createdGame.joinCode)!!

            val createdMap = gameService.makeRiddlersMap(gameInDb)

            assertThat(createdMap.values.size).isEqualTo(3)
            assertThat(createdMap.values).doesNotHaveDuplicates()
            assertThat(createdMap).allSatisfy { key, value -> assertThat(key).isNotEqualTo(value) }
        }

        @Test
        fun `should make big riddler to riddled map`() {
            for (userId in 100L..110L) {
                userService.findUserOrCreateOne(createMessage(randomUser(userId)))
                gameService.joinGame(
                    bot,
                    userId,
                    createdGame.joinCode
                )
            }
            val gameInDb = gameRepository.findByJoinCodeEquals(createdGame.joinCode)!!

            val createdMap = gameService.makeRiddlersMap(gameInDb)

            assertThat(createdMap.values.size).isEqualTo(gameInDb.users.size)
            assertThat(createdMap.values).doesNotHaveDuplicates()
            assertThat(createdMap).allSatisfy { key, value -> assertThat(key).isNotEqualTo(value) }
        }

        @Test
        fun `should ask everybody to make a riddle on game start and set makesRiddleFor in DB`() {
            gameService.startGame(bot, userAegis.first.id)

            val gameInDb = gameRepository.findByJoinCodeEquals(createdGame.joinCode)!!

            gameInDb.users.forEach {
                verify(bot, atLeastOnce()).sendMessage(
                    chatId = it.chatId!!,
                    text = Messages.makeRiddleFor(it.makesRiddleFor!!),
                    parseMode = ParseMode.MARKDOWN_V2
                )
            }


            assertThat(userService.findUser(userAegis.first.id).makesRiddleFor).isNotNull
            assertThat(userService.findUser(userMarshall.first.id).makesRiddleFor).isNotNull
            assertThat(userService.findUser(userJockey.first.id).makesRiddleFor).isNotNull
        }

        @Test
        fun `should correctly process riddles for users`() {
            gameService.startGame(bot, userAegis.first.id)

            val gameInDb = gameRepository.findByJoinCodeEquals(createdGame.joinCode)!!

            gameInDb.users.forEach {
                gameService.handleUserRiddleResponse(bot,it.id!!,it.makesRiddleFor!!.name)
            }

            gameInDb.users.forEach {
                assertThat(userService.findUser(it.id!!).riddledPerson).isEqualTo(it.name)
            }
        }
    }


    @Test
    @Transactional
    open fun `should set current game on create`() {
        val aegis = userRepository.save(
            User(
                id = userAegis.first.id,
                name = userAegis.first.firstName,
                chatId = userAegis.second.id
            )
        )
        val createdGame = gameService.createGame(bot, userAegis.first.id)

        val userInDb = userRepository.findByIdEquals(1L)

        assertThat(userInDb!!.currentGame!!.id).isEqualTo(createdGame.id)
    }


    @Test
    @Transactional
    open fun `should set current game on join`() {
        val aegis = userRepository.save(
            User(
                id = userAegis.first.id,
                name = userAegis.first.firstName,
                chatId = userAegis.second.id
            )
        )
        val marshal = userRepository.save(
            User(
                id = userMarshall.first.id,
                name = userMarshall.first.firstName,
                chatId = userMarshall.second.id
            )
        )

        val createdGame = gameService.createGame(bot, userAegis.first.id)
        gameService.joinGame(
            bot,
            userMarshall.first.id,
            createdGame.joinCode
        )

        val creatorUserInDb = userRepository.findByIdEquals(1L)
        val joinedUserInDb = userRepository.findByIdEquals(2L)


        assertThat(creatorUserInDb!!.currentGame!!.id).isEqualTo(createdGame.id)
        assertThat(joinedUserInDb!!.currentGame!!.id).isEqualTo(createdGame.id)
    }
}
