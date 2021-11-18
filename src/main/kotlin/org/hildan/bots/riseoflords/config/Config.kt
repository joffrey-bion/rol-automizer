package org.hildan.bots.riseoflords.config

import kotlin.time.Duration

data class Config(
    val account: Account,
    val playerFilter: PlayerFilter,
    val attackParams: AttackParams,
    val nbOfAttacks: Int,
    val timeBetweenAttacks: Duration
) {
    override fun toString(): String = """
           $account
           $playerFilter
           $attackParams
           """.trimIndent()
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
    val goldThreshold: Long,
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
