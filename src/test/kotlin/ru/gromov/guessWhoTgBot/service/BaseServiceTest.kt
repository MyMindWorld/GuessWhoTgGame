package ru.gromov.guessWhoTgBot.service

import com.github.kotlintelegrambot.Bot
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.gromov.guessWhoTgBot.MainBotLoop
import ru.gromov.guessWhoTgBot.db.repository.GameRepository
import ru.gromov.guessWhoTgBot.db.repository.UserRepository


@SpringBootTest
@ActiveProfiles("dbtest")
@ExtendWith(SpringExtension::class)
@PropertySource("classpath:application.properties")
open class BaseServiceTest {
    @Mock
    var bot = Mockito.mock(Bot::class.java)

    @MockBean
    lateinit var mainBotLoop: MainBotLoop

    @Autowired
    lateinit var gameService: GameService

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var gameRepository: GameRepository

    @Autowired
    lateinit var userRepository: UserRepository
}
