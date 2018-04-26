package org.hildan.bots.riseoflords.sequencing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.hildan.bots.riseoflords.config.Account;
import org.hildan.bots.riseoflords.config.AttackParams;
import org.hildan.bots.riseoflords.config.Config;
import org.hildan.bots.riseoflords.config.PlayerFilter;
import org.hildan.bots.riseoflords.model.Player;
import org.hildan.bots.riseoflords.network.RoLAdapter;
import org.hildan.bots.riseoflords.util.Format;
import org.hildan.bots.riseoflords.util.Sleeper;
import org.hildan.bots.riseoflords.util.Sleeper.Speed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sequence {

    private static final Logger logger = LoggerFactory.getLogger(Sequence.class);

    private static final Comparator<Player> richestFirst = Comparator.comparingInt(Player::getGold).reversed();

    private final Sleeper fakeTime = new Sleeper(Speed.INHUMAN);

    private final RoLAdapter rol;

    private final Config config;

    public Sequence(Config config) {
        this.config = config;
        this.rol = new RoLAdapter();
    }

    public void start() throws LoginException {
        logger.info("Starting attack session...");
        final Account account = config.getAccount();
        login(account.getLogin(), account.getPassword());
        attackRichest(config.getPlayerFilter(), config.getAttackParams());
        fakeTime.changePageLong();
        logout();
        logger.info("End of session.");
    }

    /**
     * Logs in with the specified credentials, and wait for standard time.
     *
     * @param username
     *         the login to connect with
     * @param password
     *         the password to connect with
     */
    private void login(String username, String password) throws LoginException {
        logger.debug("Logging in with username {}...", username);
        rol.login(username, password);
        logger.info("Logged in with username: {}", username);
        logger.info("Faking redirection page delay... (this takes a few seconds)");
        fakeTime.waitAfterLogin();
        rol.displayHomePage();
        fakeTime.actionInPage();
    }

    /**
     * Logs out.
     */
    private void logout() {
        logger.debug("Logging out...");
        final boolean success = rol.logout();
        if (success) {
            logger.info("Logout successful");
        } else {
            logger.error("Logout failure");
        }
    }

    /**
     * Attacks the richest players matching the filter.
     *
     * @param filter
     *         the {@link PlayerFilter} to use to choose players
     * @param params
     *         the {@link AttackParams} to use for attacks/actions sequencing
     *
     * @return the total gold stolen
     */
    private int attackRichest(PlayerFilter filter, AttackParams params) {
        final int maxTurns = Math.min(rol.getCurrentState().turns, params.getMaxTurns());
        if (maxTurns <= 0) {
            logger.info("No more turns to spend, aborting attack.");
            return 0;
        }
        logger.info("Starting attack on players ranked {} to {} richer than {} gold ({} attacks max)",
                        filter.getMinRank(), filter.getMaxRank(), Format.gold(filter.getGoldThreshold()), maxTurns);
        logger.info("Searching players matching the config filter...");
        final List<Player> matchingPlayers = new ArrayList<>();
        int startRank = filter.getMinRank();
        while (startRank < filter.getMaxRank()) {
            logger.debug("Reading page of players ranked {} to {}...", startRank, startRank + 98);
            final List<Player> filteredPage = rol.listPlayers(startRank).stream() // stream players
                                                 .filter(p -> p.getGold()
                                                         >= filter.getGoldThreshold()) // above gold threshold
                                                 .filter(p -> p.getRank() <= filter.getMaxRank()) // below max rank
                                                 .sorted(richestFirst) // richest first
                                                 .limit(params.getMaxTurns()) // limit to max turns
                                                 .limit(rol.getCurrentState().turns) // limit to available turns
                                                 .collect(Collectors.toList());
            int pageMaxRank = Math.min(startRank + 98, filter.getMaxRank());
            int nbMatchingPlayers = matchingPlayers.size();
            matchingPlayers.addAll(filteredPage);
            logger.info("{} matching player{} found so far, ranked {} to {} ({}/{} players scanned)",
                            nbMatchingPlayers > 0 ? nbMatchingPlayers : "No", nbMatchingPlayers > 1 ? "s" : "",
                            filter.getMinRank(), pageMaxRank, pageMaxRank - filter.getMinRank() + 1,
                            filter.getNbPlayersToScan());
            fakeTime.readPage();
            startRank += 99;
        }
        final int nbMatchingPlayers = matchingPlayers.size();
        if (nbMatchingPlayers > maxTurns) {
            logger.info(String.format(
                    "Only %d out of the %d matching players can be attacked, filtering only the richest of them...",
                    maxTurns, matchingPlayers.size()));
            // too many players, select only the richest
            final List<Player> playersToAttack = matchingPlayers.stream() // stream players
                                                                .sorted(richestFirst) // richest first
                                                                .limit(params.getMaxTurns()) // limit to max turns
                                                                .limit(rol.getCurrentState().turns) // limit to available turns
                                                                .collect(Collectors.toList());
            return attackAll(playersToAttack, params);
        } else {
            return attackAll(matchingPlayers, params);
        }
    }

    /**
     * Attacks all the specified players, following the given parameters.
     *
     * @param playersToAttack
     *         the filtered list of players to attack. They must verify the thresholds specified by the given {@link
     *         PlayerFilter}.
     * @param params
     *         the parameters to follow. In particular the storing and repair frequencies are used.
     *
     * @return the total gold stolen
     */
    private int attackAll(List<Player> playersToAttack, AttackParams params) {
        logger.info("{} players to attack", playersToAttack.size());
        if (rol.getCurrentState().turns == 0) {
            logger.error("No turns available, impossible to attack");
            return 0;
        } else if (rol.getCurrentState().turns < playersToAttack.size()) {
            logger.error("Not enough turns to attack this many players, attack aborted");
            return 0;
        }
        int totalGoldStolen = 0;
        int nbConsideredPlayers = 0;
        int nbAttackedPlayers = 0;
        for (final Player player : playersToAttack) {
            nbConsideredPlayers++;
            // attack player
            final int goldStolen = attack(player);
            if (goldStolen < 0) {
                // error, player not attacked
                continue;
            }
            totalGoldStolen += goldStolen;
            nbAttackedPlayers++;
            final boolean isLastPlayer = nbConsideredPlayers == playersToAttack.size();
            // repair weapons as specified
            if (nbAttackedPlayers % params.getRepairPeriod() == 0 || isLastPlayer) {
                fakeTime.beforeRepair();
                repairWeapons();
            }
            // store gold as specified
            if (rol.getCurrentState().gold >= params.getStorageThreshold() || isLastPlayer) {
                fakeTime.beforeGoldStorage();
                storeGoldIntoChest();
                fakeTime.afterGoldStorage();
            } else {
                fakeTime.betweenAttacksWhenNoStorage();
            }
        }
        logger.info("{} total gold stolen from {} players.", Format.gold(totalGoldStolen), nbAttackedPlayers);
        logger.info("The chest now contains {} gold.", Format.gold(rol.getCurrentState().chestGold));
        return totalGoldStolen;
    }

    /**
     * Attacks the specified player.
     *
     * @param player
     *         the player to attack
     *
     * @return the gold stolen from that player
     */
    private int attack(Player player) {
        logger.debug("Attacking player {}...", player.getName());
        logger.trace("Displaying player page...");
        final int playerGold = rol.displayPlayer(player.getName());
        if (playerGold == RoLAdapter.ERROR_REQUEST) {
            logger.error("Something's wrong: request failed");
            return -1;
        } else if (playerGold != player.getGold()) {
            logger.warn("Something's wrong: the player does not have the expected gold");
            return -1;
        }

        fakeTime.actionInPage();

        logger.trace("Attacking...");
        final int goldStolen = rol.attack(player.getName());
        if (goldStolen > 0) {
            logger.info("Victory! {} gold stolen from player {}, current gold: {}", Format.gold(goldStolen),
                    player.getName(), Format.gold(rol.getCurrentState().gold));
        } else if (goldStolen == RoLAdapter.ERROR_STORM_ACTIVE) {
            logger.warn("Cannot attack: a storm is raging upon your kingdom!");
        } else if (goldStolen == RoLAdapter.ERROR_REQUEST) {
            logger.error("Attack HTTP request failed, something went wrong");
        } else {
            logger.warn("Defeat! Ach, player {} was too sronk! Current gold: {}", player.getName(),
                    rol.getCurrentState().gold);
        }
        return goldStolen;
    }

    private int storeGoldIntoChest() {
        logger.trace("Storing gold into the chest...");
        logger.trace("Displaying chest page...");
        final int amount = rol.displayChestPage();
        logger.trace("{} gold to store", Format.gold(amount));

        fakeTime.actionInPage();

        logger.trace("Storing everything...");
        final boolean success = rol.storeInChest(amount);
        if (success) {
            logger.trace("The gold is safe!");
        } else {
            logger.trace("Something went wrong!");
        }
        logger.info("{} gold stored in chest, total: {}", Format.gold(amount),
                Format.gold(rol.getCurrentState().chestGold));
        return amount;
    }

    private void repairWeapons() {
        logger.trace("Repairing weapons...");
        logger.trace("Displaying weapons page...");
        final int wornness = rol.displayWeaponsPage();
        logger.trace("Weapons worn at {}%", wornness);

        fakeTime.actionInPage();

        logger.trace("Repair request...");
        final boolean success = rol.repairWeapons();
        if (!success) {
            logger.error("Couldn't repair weapons, is there enough gold?");
        } else {
            logger.info("Weapons repaired");
        }
    }
}
