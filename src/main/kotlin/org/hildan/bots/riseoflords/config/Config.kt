package org.hildan.bots.riseoflords.config

import java.io.FileInputStream
import java.time.Duration
import java.util.*

data class Config(
    val account: Account,
    val playerFilter: PlayerFilter,
    val attackParams: AttackParams,
    val nbOfAttacks: Int,
    val timeBetweenAttacks: Duration
) {
    fun unlimitedAttacks(): Boolean = nbOfAttacks == 0

    override fun toString(): String = """
           $account
           $playerFilter
           $attackParams
           """.trimIndent()

    companion object {

        fun loadFromFile(filename: String): Config = with(Properties().apply { load(FileInputStream(filename)) }) {
            Config(
                account = Account(
                    login = getMandatoryProperty("account.login"),
                    password = getMandatoryProperty("account.password"),
                ),
                playerFilter = PlayerFilter(
                    minRank = getIntProperty("filter.minRank", 500),
                    maxRank = getIntProperty("filter.maxRank", 4_000),
                    goldThreshold = getIntProperty("filter.minGold", 400_000),
                ),
                attackParams = AttackParams(
                    maxTurns = getIntProperty("attack.maxTurns", 20),
                    repairPeriod = getIntProperty("attack.repairPeriod", 5),
                    storageThreshold = getIntProperty("attack.storageThreshold", 500_000),
                ),
                nbOfAttacks = getIntProperty("sequence.nbOfAttacks", 1),
                timeBetweenAttacks = Duration.ofHours(getIntProperty("sequence.hoursBetweenAttacks", 1).toLong()),
            )
        }
    }
}

data class Account(
    val login: String,
    val password: String,
) {
    override fun toString(): String = """
        Account:
            username: $login
            password: ${"*".repeat(8)}
    """.trimIndent()
}

/** Parameters to choose players. */
class PlayerFilter(
    /** The minimum rank to attack.  */
    val minRank: Int,
    /** The maximum rank to attack.  */
    val maxRank: Int,
    /** Only players with at least this much gold will be attacked.  */
    val goldThreshold: Int
) {
    val nbPlayersToScan: Int
        get() = maxRank - minRank + 1

    override fun toString(): String = "Player filter:\n   ranks: $minRank-$maxRank\n   min gold: $goldThreshold"
}

data class AttackParams(
    /** The maximum number of turns to use for the attack. */
    val maxTurns: Int,
    /** The number of attacks between each weapon reparation. */
    val repairPeriod: Int,
    /** The amount of gold that should trigger storage in the chest. */
    val storageThreshold: Int
) {
    override fun toString(): String = """
        Attack params:
            maxTurns: $maxTurns
            repair period: $repairPeriod
            gold storage threshold: $storageThreshold
    """.trimIndent()
}

class BadConfigException(message: String?) : Exception(message)

private fun Properties.getMandatoryProperty(key: String): String =
    getProperty(key)?.ifEmpty { null } ?: throw BadConfigException("No value for '$key', can't continue")

private fun Properties.getIntProperty(key: String, defaultValue: Int): Int = try {
    getProperty(key)?.toInt() ?: defaultValue
} catch (e: NumberFormatException) {
    throw BadConfigException("The value for key '$key' must be an integer")
}
