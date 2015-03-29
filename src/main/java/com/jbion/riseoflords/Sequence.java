package com.jbion.riseoflords;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.jbion.riseoflords.config.Account;
import com.jbion.riseoflords.config.AttackParams;
import com.jbion.riseoflords.config.Config;
import com.jbion.riseoflords.config.PlayerFilter;
import com.jbion.riseoflords.model.AccountState;
import com.jbion.riseoflords.model.Player;
import com.jbion.riseoflords.network.RoLAdapter;
import com.jbion.riseoflords.util.Format;
import com.jbion.riseoflords.util.Log;
import com.jbion.riseoflords.util.Log.Mode;
import com.jbion.riseoflords.util.Sleeper;
import com.jbion.riseoflords.util.Sleeper.Speed;

public class Sequence {

    private static final String TAG = Sequence.class.getSimpleName();

    private static final Comparator<Player> richestFirst = Comparator.comparingInt(Player::getGold).reversed();

    private final Log log = Log.get();
    private final Sleeper fakeTime = new Sleeper(Speed.FAST);

    private final RoLAdapter rol;

    private final Config config;

    public Sequence(Config config) {
        this.config = config;
        this.rol = new RoLAdapter();
    }

    public AccountState getCurrentState() {
        return rol.getCurrentState();
    }

    public void start() {
        log.i(TAG, "Starting attack session...");
        final Account account = config.getAccount();
        login(account.getLogin(), account.getPassword());
        attackRichest(config.getPlayerFilter(), config.getAttackParams());
        fakeTime.changePageLong();
        log.i(TAG, "");
        logout();
        log.i(TAG, "End of session.");
    }

    /**
     * Logs in with the specified credentials, and wait for standard time.
     *
     * @param username
     *            the login to connect with
     * @param password
     *            the password to connect with
     */
    private void login(String username, String password) {
        log.d(TAG, "Logging in with username ", username, "...");
        final boolean success = rol.login(username, password);
        if (success) {
            log.i(TAG, "Logged in with username: ", username);
            log.i(TAG, "");
            log.i(TAG, "Faking redirection page delay... (this takes a few seconds)");
            log.i(TAG, "");
        } else {
            throw new RuntimeException("Login failure.");
        }
        fakeTime.waitAfterLogin();
        rol.homePage();
        fakeTime.actionInPage();
    }

    /**
     * Logs out.
     */
    private void logout() {
        log.d(TAG, "Logging out...");
        final boolean success = rol.logout();
        if (success) {
            log.i(TAG, "Logout successful");
        } else {
            log.e(TAG, "Logout failure");
        }
    }

    /**
     * Attacks the richest players matching the filter.
     *
     * @param filter
     *            the {@link PlayerFilter} to use to choose players
     * @param params
     *            the {@link AttackParams} to use for attacks/actions sequencing
     * @return the total gold stolen
     */
    private int attackRichest(PlayerFilter filter, AttackParams params) {
        final int maxTurns = Math.min(rol.getCurrentState().turns, params.getMaxTurns());
        if (maxTurns <= 0) {
            log.i(TAG, "No more turns to spend, aborting attack.");
            return 0;
        }
        log.i(TAG, String
              .format("Starting massive attack on players ranked %d to %d richer than %s gold (%d attacks max)",
                      filter.getMinRank(), filter.getMaxRank(), Format.gold(filter.getGoldThreshold()), maxTurns));
        log.i(TAG, "Searching players matching the config filter...");
        log.indent();
        final List<Player> matchingPlayers = new ArrayList<>();
        int startRank = filter.getMinRank();
        while (startRank < filter.getMaxRank()) {
            log.d(TAG, "Reading page of players ranked ", startRank, " to ", startRank + 98, "...");
            final List<Player> filteredPage = rol.listPlayers(startRank).stream() // stream players
                    .filter(p -> p.getGold() >= filter.getGoldThreshold()) // above gold threshold
                    .filter(p -> p.getRank() <= filter.getMaxRank()) // below max rank
                    .sorted(richestFirst) // richest first
                    .limit(params.getMaxTurns()) // limit to max turns
                    .limit(rol.getCurrentState().turns) // limit to available turns
                    .collect(Collectors.toList());
            int pageMaxRank = Math.min(startRank + 98, filter.getMaxRank());
            log.i(Mode.FILE, TAG, String.format("  %2d matching player%s ranked %d to %d (%d/%d players scanned)",
                                                matchingPlayers.size(), matchingPlayers.size() > 1 ? "s" : "",
                                                filter.getMinRank(), pageMaxRank,
                                                pageMaxRank - filter.getMinRank() + 1, filter.getNbPlayersToScan()));
            matchingPlayers.addAll(filteredPage);
            System.out.print("\r");
            System.out.print(String.format("  %2d matching player%s ranked %d to %d (%d/%d players scanned)",
                                           matchingPlayers.size(), matchingPlayers.size() > 1 ? "s" : "",
                                           filter.getMinRank(), pageMaxRank, pageMaxRank - filter.getMinRank() + 1,
                                           filter.getNbPlayersToScan()));
            fakeTime.readPage();
            startRank += 99;
        }
        System.out.println();
        System.out.println();
        log.deindent(1);
        final int nbMatchingPlayers = matchingPlayers.size();
        if (nbMatchingPlayers > maxTurns) {
            log.i(TAG,
                  String.format("only %d out of the %d matching players can be attacked, filtering only the richest of them...",
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
     *            the filtered list of players to attack. They must verify the thresholds specified
     *            by the given {@link PlayerFilter}.
     * @param params
     *            the parameters to follow. In particular the storing and repair frequencies are
     *            used.
     * @return the total gold stolen
     */
    private int attackAll(List<Player> playersToAttack, AttackParams params) {
        log.i(TAG, playersToAttack.size(), " players to attack");
        if (rol.getCurrentState().turns == 0) {
            log.e(TAG, "No turns available, impossible to attack.");
            return 0;
        } else if (rol.getCurrentState().turns < playersToAttack.size()) {
            log.e(TAG, "Not enough turns to attack this many players, attack aborted.");
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
                fakeTime.changePage();
                repairWeapons();
            }
            // store gold as specified
            if (nbAttackedPlayers % params.getStoragePeriod() == 0 || isLastPlayer) {
                fakeTime.changePage();
                storeGoldIntoChest();
                fakeTime.pauseWhenSafe();
            } else {
                fakeTime.changePageLong();
            }
        }
        log.i(TAG, Format.gold(totalGoldStolen), " total gold stolen from ", nbAttackedPlayers, " players");
        log.i(TAG, "The chest now contains ", Format.gold(rol.getCurrentState().chestGold), " gold.");
        return totalGoldStolen;
    }

    /**
     * Attacks the specified player.
     *
     * @param player
     *            the player to attack
     * @return the gold stolen from that player
     */
    private int attack(Player player) {
        log.d(TAG, "Attacking player ", player.getName(), "...");
        log.indent();
        log.v(TAG, "Displaying player page...");
        final int playerGold = rol.displayPlayer(player.getName());
        log.indent();
        if (playerGold == RoLAdapter.ERROR_REQUEST) {
            log.e(TAG, "Something's wrong: request failed");
            log.deindent(2);
            return -1;
        } else if (playerGold != player.getGold()) {
            log.w(TAG, "Something's wrong: the player does not have the expected gold");
            log.deindent(2);
            return -1;
        }
        log.deindent(1);

        fakeTime.actionInPage();

        log.v(TAG, "Attacking...");
        final int goldStolen = rol.attack(player.getName());
        log.deindent(1);
        if (goldStolen > 0) {
            log.i(TAG, "Victory! ", Format.gold(goldStolen), " gold stolen from player ", player.getName(),
                  ", current gold: ", Format.gold(rol.getCurrentState().gold));
        } else if (goldStolen == RoLAdapter.ERROR_STORM_ACTIVE) {
            log.e(TAG, "Cannot attack: a storm is raging upon your kingdom!");
        } else if (goldStolen == RoLAdapter.ERROR_REQUEST) {
            log.e(TAG, "Attack request failed, something went wrong");
        } else {
            log.w(TAG, "Defeat! Ach, player ", player.getName(), " was too sronk! Current gold: ",
                  rol.getCurrentState().gold);
        }
        return goldStolen;
    }

    private int storeGoldIntoChest() {
        log.v(TAG, "Storing gold into the chest...");
        log.indent();
        log.v(TAG, "Displaying chest page...");
        final int amount = rol.displayChestPage();
        log.v(TAG, Format.gold(amount) + " gold to store");

        fakeTime.actionInPage();

        log.v(TAG, "Storing everything...");
        log.indent();
        final boolean success = rol.storeInChest(amount);
        if (success) {
            log.v(TAG, "The gold is safe!");
        } else {
            log.v(TAG, "Something went wrong!");
        }
        log.deindent(2);
        log.i(TAG, Format.gold(amount), " gold stored in chest, total: " + Format.gold(rol.getCurrentState().chestGold));
        return amount;
    }

    private void repairWeapons() {
        log.v(TAG, "Repairing weapons...");
        log.indent();
        log.v(TAG, "Displaying weapons page...");
        final int wornness = rol.displayWeaponsPage();
        log.v(TAG, "Weapons worn at ", wornness, "%");

        fakeTime.actionInPage();

        log.v(TAG, "Repair request...");
        final boolean success = rol.repairWeapons();
        log.deindent(1);
        if (!success) {
            log.e(TAG, "Couldn't repair weapons, is there enough gold?");
        } else {
            log.i(TAG, "Weapons repaired");
        }
    }
}
