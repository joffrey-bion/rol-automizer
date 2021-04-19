package org.hildan.bots.riseoflords

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import org.hildan.bots.riseoflords.config.Account
import org.hildan.bots.riseoflords.config.AttackParams
import org.hildan.bots.riseoflords.config.Config
import org.hildan.bots.riseoflords.config.PlayerFilter
import org.hildan.bots.riseoflords.client.LoginException
import org.hildan.bots.riseoflords.sequencing.AttackManager
import org.slf4j.LoggerFactory
import java.time.Duration

object RolAutomizer

fun main(args: Array<String>) = RolAutomizerCommand().main(args)

private val logger = LoggerFactory.getLogger(RolAutomizer::class.java)

class RolAutomizerCommand : CliktCommand() {

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    private val username by option(
        "-u",
        "--username",
        help = "your Rise of Lords username",
        envvar = "ROL_USERNAME",
    ).required()

    private val password by option(
        "-p",
        "--password",
        help = "your Rise of Lords password",
        envvar = "ROL_PASSWORD",
    ).prompt("Enter your password:", hideInput = true)

    private val minRank by option(
        "-m",
        "--min-rank",
        help = "the minimum rank of the players to attack",
    ).int().default(400)

    private val maxRank by option(
        "-M",
        "--max-rank",
        help = "the maximum rank of the players to attack",
    ).int().default(2_200)

    private val minGold by option(
        "-g",
        "--min-gold",
        help = "the minimum gold of the enemy player to consider an attack worth it",
    ).int().default(400_000)

    private val maxTurns by option(
        "-t",
        "--max-turns",
        help = "the maximum number of turns to use during an attack session",
    ).int().default(40)

    private val repairPeriod by option(
        "-r",
        "--repair-period",
        help = "the number of attacks between weapon repairs",
    ).int().default(5, "every 5 attacks")

    private val storageThreshold by option(
        "--storage-threshold",
        help = "the threshold above which we need to store the current gold into the chest",
    ).int().default(300_000).check("cannot store less than 300k into the chest") { it >= 300_000 }

    private val nbOfAttacks by option(
        "--attacks-count",
        help = "the number of attack sessions to perform",
    ).int().default(1)

    private val timeBetweenAttacks by option(
        "--rest-time",
        help = "the number of hours to wait between attack sessions",
        metavar = "HOURS",
    ).int().convert { Duration.ofHours(it.toLong()) }.default(Duration.ofHours(12))

    override fun run() {
        val config = Config(
            account = Account(username, password),
            playerFilter = PlayerFilter(minRank, maxRank, minGold),
            attackParams = AttackParams(maxTurns, repairPeriod, storageThreshold),
            nbOfAttacks = nbOfAttacks,
            timeBetweenAttacks = timeBetweenAttacks,
        )
        val attacks = AttackManager(config)
        repeat(nbOfAttacks) {
            logger.info("Starting attack session {}/{}", it + 1, nbOfAttacks)
            try {
                attacks.startAttackSession()
            } catch (e: LoginException) {
                logger.error("Login failed for user {}", e.username)
            } catch (e: Exception) {
                logger.error("UNCAUGHT EXCEPTION", e)
            }
            if (it + 1 < nbOfAttacks) {
                // more attacks are waiting
                waitForNextAttack(timeBetweenAttacks)
            }
        }
        logger.info("End of attacks")
    }
}

private fun waitForNextAttack(duration: Duration) {
    var d = duration
    try {
        val millis = d.toMillis() % 1000
        Thread.sleep(millis)
        d = d.minusMillis(millis)
        while (!d.isZero && !d.isNegative) {
            Thread.sleep(1000)
            d = d.minusSeconds(1)
            printDuration(d)
        }
        print("\r")
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
    }
}

private fun printDuration(d: Duration) {
    val hours = d.toHours()
    val minutes = d.minusHours(hours).toMinutes()
    val seconds = d.minusHours(hours).minusMinutes(minutes).toMillis() / 1000
    print("\r")
    print(
        String.format(
            "   Next attack session in %s%02d:%02d...", if (hours > 0) "$hours:" else "", minutes, seconds
        )
    )
}
