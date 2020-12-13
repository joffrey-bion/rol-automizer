package org.hildan.bots.riseoflords.sequencing

import org.hildan.bots.riseoflords.config.AttackParams
import org.hildan.bots.riseoflords.config.Config
import org.hildan.bots.riseoflords.config.PlayerFilter
import org.hildan.bots.riseoflords.model.Player
import org.hildan.bots.riseoflords.network.RiseOfLordsClient
import org.hildan.bots.riseoflords.util.Format
import org.hildan.bots.riseoflords.util.Sleeper
import org.hildan.bots.riseoflords.util.Sleeper.Speed
import org.slf4j.LoggerFactory
import java.util.*

class AttackManager(private val config: Config) {

    private val fakeTime = Sleeper(Speed.INHUMAN)
    private val rol: RiseOfLordsClient = RiseOfLordsClient()

    fun startAttackSession() {
        logger.info("Starting attack session...")
        login(config.account.login, config.account.password)
        attackRichest(config.playerFilter, config.attackParams)
        fakeTime.changePageLong()
        logout()
        logger.info("End of session.")
    }

    private fun login(username: String, password: String) {
        logger.debug("Logging in with username {}...", username)
        rol.login(username, password)
        logger.info("Logged in with username: {}", username)
        logger.info("Faking redirection page delay... (this takes a few seconds)")
        fakeTime.waitAfterLogin()
        rol.displayHomePage()
        fakeTime.actionInPage()
    }

    private fun logout() {
        logger.debug("Logging out...")
        val success = rol.logout()
        if (success) {
            logger.info("Logout successful")
        } else {
            logger.error("Logout failure")
        }
    }

    /**
     * Attacks the richest players matching the [filter], respecting the given [params], and returns the total
     */
    private fun attackRichest(filter: PlayerFilter, params: AttackParams): Int {
        val maxTurns = rol.currentState.turns.coerceAtMost(params.maxTurns)
        if (maxTurns <= 0) {
            logger.info("No more turns to spend, aborting attack.")
            return 0
        }
        logger.info(
            "Starting attack on players ranked {} to {} richer than {} gold ({} attacks max)",
            filter.minRank,
            filter.maxRank,
            Format.gold(filter.goldThreshold),
            maxTurns
        )
        logger.info("Searching players matching the config filter...")
        val matchingPlayers: MutableList<Player> = ArrayList()
        var startRank = filter.minRank
        while (startRank < filter.maxRank) {
            logger.debug("Reading page of players ranked {} to {}...", startRank, startRank + 98)
            val filteredPage = rol.listPlayers(startRank).asSequence() // stream players
                .filter { p -> p.gold >= filter.goldThreshold } // above gold threshold
                .filter { p -> p.rank <= filter.maxRank } // below max rank
                .sortedByDescending { it.gold } // richest first
                .take(params.maxTurns) // limit to max turns
                .take(rol.currentState.turns) // limit to available turns
                .toList()
            val pageMaxRank = (startRank + 98).coerceAtMost(filter.maxRank)
            val nbMatchingPlayers = matchingPlayers.size
            matchingPlayers.addAll(filteredPage)
            logger.info(
                "{} matching player{} found so far, ranked {} to {} ({}/{} players scanned)",
                if (nbMatchingPlayers > 0) nbMatchingPlayers else "No",
                if (nbMatchingPlayers > 1) "s" else "",
                filter.minRank,
                pageMaxRank,
                pageMaxRank - filter.minRank + 1,
                filter.nbPlayersToScan
            )
            fakeTime.readPage()
            startRank += 99
        }
        val nbMatchingPlayers = matchingPlayers.size
        return if (nbMatchingPlayers > maxTurns) {
            logger.info(
                "Only {} out of the {} matching players can be attacked, filtering only the richest of them...",
                maxTurns,
                matchingPlayers.size
            )
            // too many players, select only the richest
            val playersToAttack = matchingPlayers.asSequence() // stream players
                .sortedByDescending { it.gold } // richest first
                .take(params.maxTurns) // limit to max turns
                .take(rol.currentState.turns) // limit to available turns
                .toList()
            attackAll(playersToAttack, params)
        } else {
            attackAll(matchingPlayers, params)
        }
    }

    /**
     * Attacks all the specified [playersToAttack], following the given [params], and returns the total amount of
     * gold stolen.
     */
    private fun attackAll(playersToAttack: List<Player>, params: AttackParams): Int {
        logger.info("{} players to attack", playersToAttack.size)
        if (rol.currentState.turns == 0) {
            logger.error("No turns available, impossible to attack")
            return 0
        } else if (rol.currentState.turns < playersToAttack.size) {
            logger.error("Not enough turns to attack this many players, attack aborted")
            return 0
        }
        var totalGoldStolen = 0
        var nbConsideredPlayers = 0
        var nbAttackedPlayers = 0
        for (player in playersToAttack) {
            nbConsideredPlayers++
            // attack player
            val goldStolen = attack(player)
            if (goldStolen < 0) {
                // error, player not attacked
                continue
            }
            totalGoldStolen += goldStolen
            nbAttackedPlayers++
            val isLastPlayer = nbConsideredPlayers == playersToAttack.size
            // repair weapons as specified
            if (nbAttackedPlayers % params.repairPeriod == 0 || isLastPlayer) {
                fakeTime.beforeRepair()
                repairWeapons()
            }
            // store gold as specified
            if (rol.currentState.gold >= params.storageThreshold || isLastPlayer) {
                fakeTime.beforeGoldStorage()
                storeGoldIntoChest()
                fakeTime.afterGoldStorage()
            } else {
                fakeTime.betweenAttacksWhenNoStorage()
            }
        }
        logger.info("{} total gold stolen from {} players.", Format.gold(totalGoldStolen), nbAttackedPlayers)
        logger.info("The chest now contains {} gold.", Format.gold(rol.currentState.chestGold))
        return totalGoldStolen
    }

    /**
     * Attacks the specified player.
     *
     * @param player
     * the player to attack
     *
     * @return the gold stolen from that player
     */
    private fun attack(player: Player): Int {
        logger.debug("Attacking player {}...", player.name)
        logger.trace("Displaying player page...")
        val playerGold = rol.displayPlayer(player.name)
        if (playerGold == RiseOfLordsClient.ERROR_REQUEST) {
            logger.error("Something's wrong: request failed")
            return -1
        } else if (playerGold != player.gold) {
            logger.warn("Something's wrong: the player does not have the expected gold")
            return -1
        }
        fakeTime.actionInPage()
        logger.trace("Attacking...")
        val goldStolen = rol.attack(player.name)
        when {
            goldStolen > 0 -> {
                logger.info(
                    "Victory! {} gold stolen from player {}, current gold: {}",
                    Format.gold(goldStolen),
                    player.name,
                    Format.gold(rol.currentState.gold)
                )
            }
            goldStolen == RiseOfLordsClient.ERROR_STORM_ACTIVE -> {
                logger.warn("Cannot attack: a storm is raging upon your kingdom!")
            }
            goldStolen == RiseOfLordsClient.ERROR_REQUEST -> {
                logger.error("Attack HTTP request failed, something went wrong")
            }
            else -> {
                logger.warn(
                    "Defeat! Ach, player {} was too sronk! Current gold: {}", player.name, rol.currentState.gold
                )
            }
        }
        return goldStolen
    }

    private fun storeGoldIntoChest(): Int {
        logger.trace("Storing gold into the chest...")
        logger.trace("Displaying chest page...")
        val amount = rol.displayChestPage()
        logger.trace("{} gold to store", Format.gold(amount))
        fakeTime.actionInPage()
        logger.trace("Storing everything...")
        val success = rol.storeInChest(amount)
        if (success) {
            logger.trace("The gold is safe!")
        } else {
            logger.trace("Something went wrong!")
        }
        logger.info(
            "{} gold stored in chest, total: {}", Format.gold(amount), Format.gold(rol.currentState.chestGold)
        )
        return amount
    }

    private fun repairWeapons() {
        logger.trace("Repairing weapons...")
        logger.trace("Displaying weapons page...")
        val wornness = rol.displayWeaponsPage()
        logger.trace("Weapons worn at {}%", wornness)
        fakeTime.actionInPage()
        logger.trace("Repair request...")
        val success = rol.repairWeapons()
        if (!success) {
            logger.error("Couldn't repair weapons, is there enough gold?")
        } else {
            logger.info("Weapons repaired")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AttackManager::class.java)
    }
}