package ru.gromov.guessWhoTgBot.db.repository

import ru.gromov.guessWhoTgBot.db.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByIdEquals(id: Long): User?
}