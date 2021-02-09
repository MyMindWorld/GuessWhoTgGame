package ru.gromov.guessWhoTgBot.db.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.gromov.guessWhoTgBot.db.model.Game
import ru.gromov.guessWhoTgBot.db.model.User

@Repository
interface GameRepository : JpaRepository<Game, Long> {
    fun findByJoinCodeEquals(joinCode: String): Game?
    fun findByCreatorEquals(creator: User): Game?
}
