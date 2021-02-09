package ru.gromov.guessWhoTgBot.service

import com.github.kotlintelegrambot.entities.ParseMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.mockito.Mockito.*
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.Rollback
import org.springframework.transaction.annotation.Transactional
import ru.gromov.guessWhoTgBot.db.model.Game
import ru.gromov.guessWhoTgBot.testData.*
import ru.gromov.guessWhoTgBot.utils.Messages
import ru.gromov.guessWhoTgBot.utils.Messages.ResponseMessages.alreadyInGameError


open class GameServiceTest : BaseServiceTest() {

    @Nested
    open class WhenGameIsCreated : BaseServiceTest() {
        private var createdGame: Game = Game()

        @BeforeEach
        fun createGameForAegis() {
            createdGame = gameService.createGame(bot, joinMessage(userAegis, createdGame.joinCode))
        }

        @Test
        @Transactional
        open fun `should reply with error message on create game`() {
            assertThrows<IllegalStateException> {
                gameService.createGame(bot, joinMessage(userAegis, createdGame.joinCode))
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
                gameService.joinGame(bot, joinMessage(userAegis, createdGame.joinCode), createdGame.joinCode)
            }

            verify(bot, atLeastOnce()).sendMessage(
                chatId = userAegis.second.id,
                text = alreadyInGameError(),
                parseMode = ParseMode.MARKDOWN_V2
            )
        }
    }

    @Nested
    @DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
    class WhenGameIsReadyToStart : BaseServiceTest() {
        private var createdGame: Game = Game()

        @BeforeEach
        fun createGameForAegisAndJoinByMarshallAndJockey() {
            createdGame = gameService.createGame(bot, createMessage(userAegis))
            gameService.joinGame(bot, joinMessage(userMarshall, createdGame.joinCode), createdGame.joinCode)
            gameService.joinGame(bot, joinMessage(userJockey, createdGame.joinCode), createdGame.joinCode)
        }

        @Test
        fun `should have correct game info in DB`() {
            val game = gameService.findUsersCurrentGame(gameInfoMessage(userAegis))
            assertThat(game!!.users.size).isEqualTo(3)
        }

        @Test
        fun `should send correct current game info`() {
            val gameInDb = gameRepository.findByJoinCodeEquals(createdGame.joinCode)

            gameService.showCurrentGame(bot, gameInfoMessage(userAegis))

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
            val gameInDb = gameRepository.findByJoinCodeEquals(createdGame.joinCode)

            val createdMap = gameService.makeRiddlersMap(gameInDb)

            assertThat(createdMap.values.size).isEqualTo(3)
            assertThat(createdMap.values).doesNotHaveDuplicates()
            assertThat(createdMap).allSatisfy { key, value -> assertThat(key).isNotEqualTo(value) }
        }

        @Test
        fun `should make big riddler to riddled map`() {
            for (userId in 100L..110L) {
                gameService.joinGame(bot, joinMessage(randomUser(userId), createdGame.joinCode), createdGame.joinCode)
            }
            val gameInDb = gameRepository.findByJoinCodeEquals(createdGame.joinCode)

            val createdMap = gameService.makeRiddlersMap(gameInDb)

            assertThat(createdMap.values.size).isEqualTo(gameInDb.users.size)
            assertThat(createdMap.values).doesNotHaveDuplicates()
            assertThat(createdMap).allSatisfy { key, value -> assertThat(key).isNotEqualTo(value) }
        }

        @Test
        fun `should ask everybody to make a riddle on game start and set makesRiddleFor in DB`() {
            gameService.startGame(bot, startGameMessage(userAegis))

            val gameInDb = gameRepository.findByJoinCodeEquals(createdGame.joinCode)

            gameInDb.users.forEach {
                verify(bot, atLeastOnce()).sendMessage(
                    chatId = it.chatId!!,
                    text = Messages.makeRiddleFor(it.makesRiddleFor!!),
                    parseMode = ParseMode.MARKDOWN_V2
                )
            }


            assertThat(userService.findUser(createMessage(userAegis)).makesRiddleFor).isNotNull
            assertThat(userService.findUser(createMessage(userMarshall)).makesRiddleFor).isNotNull
            assertThat(userService.findUser(createMessage(userJockey)).makesRiddleFor).isNotNull
        }
    }


    @Test
    @Transactional
    open fun `should set current game on create`() {
        val createdGame = gameService.createGame(bot, createMessage(userAegis))

        val userInDb = userRepository.findByIdEquals(1L)

        assertThat(userInDb!!.currentGame!!.id).isEqualTo(createdGame.id)
    }


    @Test
    @Transactional
    open fun `should set current game on join`() {
        val createdGame = gameService.createGame(bot, createMessage(userAegis))
        gameService.joinGame(bot, joinMessage(userMarshall, createdGame.joinCode), createdGame.joinCode)

        val creatorUserInDb = userRepository.findByIdEquals(1L)
        val joinedUserInDb = userRepository.findByIdEquals(2L)


        assertThat(creatorUserInDb!!.currentGame!!.id).isEqualTo(createdGame.id)
        assertThat(joinedUserInDb!!.currentGame!!.id).isEqualTo(createdGame.id)
    }
}