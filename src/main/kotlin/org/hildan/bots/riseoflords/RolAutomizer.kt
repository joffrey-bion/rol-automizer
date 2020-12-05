package org.hildan.bots.riseoflords

import org.hildan.bots.riseoflords.config.BadConfigException
import org.hildan.bots.riseoflords.config.Config
import org.hildan.bots.riseoflords.network.LoginException
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.IOException
import java.time.Duration

object RolAutomizer {

    private val logger = LoggerFactory.getLogger(RolAutomizer::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            logger.error(
                """
                No config file provided: you must provide a .rol file to open.
                More info at https://github.com/joffrey-bion/rol-automizer
                """.trimIndent()
            )
            return
        }
        val filename = args[0]
        val config = loadConfig(filename) ?: return
        logger.info("Loaded config:\n{}", config.toString())
        val sequence = org.hildan.bots.riseoflords.sequencing.Sequence(config)
        var i = 0
        while (config.unlimitedAttacks() || i < config.nbOfAttacks) {
            logger.info("Starting attack session {}/{}", i + 1, config.nbOfAttacks)
            try {
                sequence.start()
            } catch (e: LoginException) {
                logger.error("Login failed for user {}", e.username)
            } catch (e: Exception) {
                logger.error("UNCAUGHT EXCEPTION", e)
            }
            if (config.unlimitedAttacks() || i + 1 < config.nbOfAttacks) {
                // more attacks are waiting
                waitForNextAttack(config.timeBetweenAttacks)
            }
            i++
        }
        logger.info("End of attacks")
    }

    private fun loadConfig(filename: String): Config? {
        try {
            return Config.loadFromFile(filename)
        } catch (e: FileNotFoundException) {
            logger.error("Cannot find config file {}", filename)
        } catch (e: BadConfigException) {
            logger.error("Error reading config file", e)
        } catch (e: IOException) {
            logger.error("Error reading config file", e)
        }
        return null
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
}