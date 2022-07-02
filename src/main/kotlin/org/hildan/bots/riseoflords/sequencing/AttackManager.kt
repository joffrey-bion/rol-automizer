package org.hildan.bots.riseoflords.sequencing

import org.hildan.bots.riseoflords.config.AttackParams
import org.hildan.bots.riseoflords.config.Config
import org.hildan.bots.riseoflords.config.PlayerFilter
import org.hildan.bots.riseoflords.model.Player
import org.hildan.bots.riseoflords.model.AttackResult
import org.hildan.bots.riseoflords.client.RiseOfLordsClient
import org.hildan.bots.riseoflords.util.Format
import org.hildan.bots.riseoflords.util.Sleeper
import org.hildan.bots.riseoflords.util.Sleeper.Speed
import org.slf4j.LoggerFactory

class AttackManager(private val config: Config) {

    private val fakeTime = Sleeper(Speed.INHUMAN)
    private val rol: RiseOfLordsClient = RiseOfLordsClient()

    fun startAttackSession() {
        logger.info("Starting attack session...")
        login(config.account.login, config.account.password)
        cloneSorcerersIfPossible()
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

    private fun cloneSorcerersIfPossible() {
        val nPossibleClones = minOf(
            (rol.currentState.gold / RiseOfLordsClient.SORCERER_CLONE_COST_GOLD).toInt(),
            rol.currentState.mana / RiseOfLordsClient.SORCERER_CLONE_COST_MANA,
        )
        if (nPossibleClones > 0) {
            rol.displaySorceryPage()
            fakeTime.actionInPage()
            val success = rol.cloneSorcerers(nPossibleClones)
            if (success) {
                logger.info("Cloned $nPossibleClones sorcerers")
            } else {
                logger.error("Error when attempting to clone $nPossibleClones sorcerers " +
                    "(Gold: ${rol.currentState.gold}, Mana: ${rol.currentState.mana})")
            }
            fakeTime.changePageLong()
        }
    }

    private fun sendEmailIfMissive() {
        val castle = rol.displayCastlePage()
        if (castle.nMessages > 0) {
            sendEmailAboutMissives(castle.nMessages)
        }
    }

    private fun sendEmailAboutMissives(nMessages: Int) {

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
    private fun attackRichest(filter: PlayerFilter, params: AttackParams): Long {
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
        logger.info("Searching players matching the criteria...")
        val playersToAttack = listMatchingPlayers(filter)
            .sortedByDescending { it.gold } // richest first
            .take(params.maxTurns) // limit to max turns
            .take(rol.currentState.turns) // limit to available turns
            .toList()
        logger.info("{} matching players found", playersToAttack.size)
        return attackAll(playersToAttack, params)
    }

    private fun listMatchingPlayers(filter: PlayerFilter) = listPlayers(startRank = filter.minRank)
        .takeWhile { it.rank <= filter.maxRank }
        .onEach {
            if (it.rank % 70 == 0 || it.rank == filter.maxRank) {
                logger.info("{}/{} players scanned", it.rank - filter.minRank + 1, filter.nbPlayersToScan)
            }
        }
        .filter { p -> p.gold >= filter.goldThreshold }

    private fun listPlayers(startRank: Int): Sequence<Player> = sequence {
        var currentFirstRank = startRank
        while (true) {
            logger.debug("Reading page of players ranked {} to {}...", currentFirstRank, currentFirstRank + 98)
            val page = rol.displayPlayerListPage(startRank = currentFirstRank)
            fakeTime.readPlayerListPage()
            yieldAll(page)
            currentFirstRank = page.last().rank + 1
        }
    }

    /**
     * Attacks all the specified [playersToAttack], following the given [params], and returns the total amount of
     * gold stolen.
     */
    private fun attackAll(playersToAttack: List<Player>, params: AttackParams): Long {
        logger.info("{} players to attack", playersToAttack.size)
        if (rol.currentState.turns == 0) {
            logger.error("No turns available, impossible to attack")
            return 0
        } else if (rol.currentState.turns < playersToAttack.size) {
            logger.error("Not enough turns to attack this many players, attack aborted")
            return 0
        }
        var totalGoldStolen = 0L
        var nbConsideredPlayers = 0
        var nbAttackedPlayers = 0
        for (player in playersToAttack) {
            nbConsideredPlayers++
            // attack player
            val result = attack(player)
            if (result !is AttackResult.Victory) {
                // no gold stolen (error or skipped)
                continue
            }
            totalGoldStolen += result.goldStolen
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
    private fun attack(player: Player): AttackResult? {
        logger.debug("Attacking player {}...", player.name)
        logger.trace("Displaying player page...")
        val playerGold = rol.displayPlayer(player.name)
        if (playerGold != player.gold) {
            logger.warn("Something's wrong: the player does not have the expected gold")
            return null
        }
        fakeTime.actionInPage()
        logger.trace("Attacking...")
        val result = rol.attack(player.name)
        when (result) {
            is AttackResult.Victory -> logger.info(
                "Victory! {} gold stolen from player {}, current gold: {}",
                Format.gold(result.goldStolen),
                player.name,
                Format.gold(rol.currentState.gold)
            )
            AttackResult.StormActive -> logger.warn("Cannot attack: a storm is raging upon your kingdom!")
            AttackResult.Defeat -> logger.warn(
                "Defeat! Ach, player {} was too stronk! Current gold: {}", player.name, rol.currentState.gold
            )
        }
        return result
    }

    private fun storeGoldIntoChest(): Long {
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
        logger.info("{} gold stored in chest, total: {}", Format.gold(amount), Format.gold(rol.currentState.chestGold))
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
