package ru.gromov.guessWhoTgBot.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import ru.gromov.guessWhoTgBot.db.model.Game
import ru.gromov.guessWhoTgBot.db.model.User
import ru.gromov.guessWhoTgBot.service.BaseServiceTest

open class GameRepositoryTest : BaseServiceTest() {


    @Test
    @Transactional
    open fun `should correctly set game for user`() {
        var firstUser = userRepository.save(User(id = 51, name = "first", chatId = 51))
        var game = gameRepository.save(Game(creator = firstUser))

        game.addUser(firstUser)
        game = gameRepository.save(game)

        assertThat(gameRepository.findByCreatorEquals(firstUser)!!.users.size).isEqualTo(1)
        assertThat(userRepository.findByIdEquals(firstUser.id!!)!!.currentGame).isEqualTo(game)
    }
}
