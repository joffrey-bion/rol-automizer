package org.hildan.bots.riseoflords

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.output.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.*
import org.hildan.bots.riseoflords.client.*
import org.hildan.bots.riseoflords.config.*
import org.hildan.bots.riseoflords.sequencing.*
import org.slf4j.*
import kotlin.time.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

object RolAutomizer

fun main(args: Array<String>) = RolAutomizerCommand().main(args)

private val logger = LoggerFactory.getLogger(RolAutomizer::class.java)

class RolAutomizerCommand : CliktCommand() {

    init {
        context {
            helpFormatter = { MordantHelpFormatter(it, showDefaultValues = true) }
        }
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
    ).long().default(400_000L)

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
    ).int().convert { it.toLong().hours }.default(12.hours)

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
    var remaining = duration
    try {
        printDuration(remaining)
        repeat(duration.inWholeSeconds.toInt()) {
            Thread.sleep(1000)
            remaining -= 1.seconds
            printDuration(remaining)
        }
        print("\r")
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
    }
}

private fun printDuration(d: Duration) {
    d.toComponents { h, m, s, _ ->
        print("\r")
        print(String.format("   Next attack session in %s%02d:%02d...", if (h > 0) "$h:" else "", m, s))
    }
}
