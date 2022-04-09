package ru.gromov.guessWhoTgBot.utils

import ru.gromov.guessWhoTgBot.db.model.Game
import ru.gromov.guessWhoTgBot.db.model.User
import toValidInlineLink
import toValidTgMessage
import java.util.stream.Collectors

class Messages {
    companion object ResponseMessages {
        fun startingGame() = "Starting game, please wait!"

        fun notStarted() = "Not started yet! Please wait."

        fun somethingWentWrong() = "Something went terribly wrong( Try recreating a game"

        fun notEnoughPlayers() = "Sadly you cannot play alone("

        fun welcomeMessage(user: User) = """
            Welcome, ${user.name}
            """.toValidTgMessage()

        fun youNeedToRegister() = """
            To use this bot, you need to register via /start command
            """.toValidTgMessage()

        fun createdGame(game: Game) = """
            Game is created!
            Tell other players to send me this link:
            `/join ${game.joinCode}`
            
            Simply click on command to copy it and share with your friends to get started!
            """.toValidTgMessage()

        fun gameInfo(game: Game) = """
            Join link : `/join ${game.joinCode}`
            Currently in game :""".toValidTgMessage() +
                "\n" + getUsersFromGame(game) // todo fix 1 person link (PavelGromov)

        fun gameIsNotFoundOrLinkIncorrect() = """
            Game not found
            Ask creator for a correct link""".toValidTgMessage()

        fun gameIsAlreadyStarted() = """
            Game is already started or finished!
            Create new one""".toValidTgMessage()


        fun weAreWaitingFor(game: Game) = """
            Game is about to be started
            Currently we are waiting for :""".toValidTgMessage() +
                "\n" + getNotReadyUsersFromGame(game)

        fun notInGameError() = """
            Currently you are not in game!
            Create one with /createGame or join one with link from your friend!
            """.toValidTgMessage()

        fun riddleIsSet() = """
            Got it, nice one!
            """.toValidTgMessage()

        fun alreadyInGameError() = """
            You already in game! 
            You can verify players with /currentGame
            Leave it with /leaveGame or ask creator to end it
            with /endGame command
            """.toValidTgMessage()


        fun sendRiddledWithoutSelf(game: Game, excludedUser: User): String =
            """
            Game is finally starting!
            Here is links between persons and riddles:""".toValidTgMessage() +
                    "\n" + getRiddleToRiddledPersonWithoutUser(game, excludedUser) +
                    "\n" + """If you having trouble with info on riddle, just click it! """.toValidTgMessage()

        fun gameEnded(user: User): String =
            """
            Game is finished, we hope you had fun!
            After all you were - """.toValidTgMessage() +
                    getRiddledPersonGoogleLink(user)

        fun itsNotYourGameToEnd(creator: User): String =
            """
            Hey, its not your game to end!
            Ask ${creator.getMentionLink().toValidInlineLink()} to do it"""

        fun userLeavesGame(): String =
            """
            Sorry that you leaving( We have to end game for everyone"""

        fun makeRiddleFor(makesRiddleFor: User) =
            """You are making riddle for ${makesRiddleFor.getMentionLink()}""".toValidInlineLink() +
                    "\n" +
                    """When ready, send riddle as `/riddle` PERSON""".trimMargin()
                        .toValidTgMessage()


        private fun getUsersFromGame(game: Game) = game.users
            .stream()
            .map { it.getMentionLink() }
            .collect(Collectors.toList())
            .joinToString(
                prefix = "",
                separator = ",\n",
                postfix = "",
                limit = 99,
                truncated = "...",
                transform = { it.toString() }).toValidInlineLink()

        private fun getNotReadyUsersFromGame(game: Game) =
            game.getNotReadyUsers()
                .stream()
                .map { it.getMentionLink() }
                .collect(Collectors.toList())
                .joinToString(
                    prefix = "",
                    separator = ",\n",
                    postfix = "",
                    limit = 99,
                    truncated = "...",
                    transform = { it.toString() }).toValidInlineLink()

        private fun getRiddleToRiddledPersonWithoutUser(game: Game, excludedUser: User) =
            game.users
                .stream().filter { it.id != excludedUser.id }
                .map { it.getMentionLink() + " is " + it.getRiddledPersonGoogleLink() }
                .collect(Collectors.toList())
                .joinToString(
                    prefix = "",
                    separator = ",\n",
                    postfix = "",
                    limit = 99,
                    truncated = "...",
                    transform = { it.toString() }).toValidInlineLink()

        private fun getRiddledPersonGoogleLink(user: User) =
            user.getRiddledPersonGoogleLink().toValidInlineLink()
    }


}
