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
import com.jbion.riseoflords.util.Log;
import com.jbion.riseoflords.util.Sleeper;
import com.jbion.riseoflords.util.Sleeper.Speed;

public class Sequence {

    private static final String TAG = Sequence.class.getSimpleName();

    private static final Comparator<Player> richestFirst = Comparator.comparingInt(Player::getGold).reversed();

    private final Log log = Log.get();
    private final Sleeper fakeTime = new Sleeper(Speed.SLOW);
    private final AccountState state = new AccountState();
    private final RoLAdapter rol = new RoLAdapter(state);

    private final Config config;

    public Sequence(Config config) {
        this.config = config;
    }

    public void start() {
        log.i(TAG, "Starting sequence...");
        Account account = config.getAccount();
        login(account.getLogin(), account.getPassword());
        if (state.turns == 0) {
            log.e(TAG, "No turns available, impossible to attack.");
            return;
        } else {
            if (state.turns < config.getAttackParams().getMaxTurns()) {
                log.w(TAG, "Warning: the turns limit exceeds the number of available turns");
            }
            attackRichest(config.getPlayerFilter(), config.getAttackParams());
            fakeTime.changePageLong();
        }
        logout();
        log.i(TAG, "End of sequence.");
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
        boolean success = rol.login(username, password);
        if (success) {
            log.i(TAG, "Logged in with username: ", username);
            log.i(TAG, "");
            log.i(TAG, "Faking redirection page delay... (this takes a few seconds)");
            log.i(TAG, "");
        } else {
            throw new RuntimeException("Login failure.");
        }
        fakeTime.waitAfterLogin();
    }

    /**
     * Logs out.
     */
    private void logout() {
        log.d(TAG, "Logging out...");
        boolean success = rol.logout();
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
        log.i(TAG, "Starting massive attack on players ranked ", filter.getMinRank(), " to ", filter.getMaxRank(),
                " richer than ", filter.getGoldThreshold(), " gold (", params.getMaxTurns(), " attacks max)");
        log.i(TAG, "Searching players matching the config filter...");
        List<Player> players = new ArrayList<>();
        int startRank = filter.getMinRank();
        while (startRank < filter.getMaxRank()) {
            log.d(TAG, "Reading page of players ranked ", startRank, " to ", startRank + 98, "...");
            List<Player> filteredPage = rol.getPlayers(startRank).stream() // stream players
                    .filter(p -> p.getGold() >= filter.getGoldThreshold()) // above gold threshold
                    .filter(p -> p.getRank() <= filter.getMaxRank()) // below max rank
                    .sorted(richestFirst) // richest first
                    .limit(params.getMaxTurns()) // limit to max turns
                    .limit(state.turns) // limit to available turns
                    .collect(Collectors.toList());
            log.i(TAG, filteredPage.size(), " matching player(s) ranked ", startRank, " to ", startRank + 98);
            players.addAll(filteredPage);
            fakeTime.readPage();
            startRank += 99;
        }
        log.i(TAG, "");
        if (players.size() > params.getMaxTurns()) {
            log.i(TAG, "Too many players matching rank and gold criterias, filtering only the richest of them...");
            // too many players, select only the richest
            List<Player> playersToAttack = players.stream() // stream players
                    .sorted(richestFirst) // richest first
                    .limit(params.getMaxTurns()) // limit to max turns
                    .limit(state.turns) // limit to available turns
                    .collect(Collectors.toList());
            return attackAll(playersToAttack, params);
        } else {
            return attackAll(players, params);
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
        log.i(TAG, playersToAttack.size(), " players matching rank and gold criterias");
        int totalGoldStolen = 0;
        int nbAttackedPlayers = 0;
        for (Player player : playersToAttack) {
            // attack player
            int goldStolen = attack(player);
            if (goldStolen < 0) {
                // error, player not attacked
                continue;
            }
            totalGoldStolen += goldStolen;
            nbAttackedPlayers++;
            // repair weapons as specified
            if (nbAttackedPlayers % params.getRepairFrequency() == 0) {
                fakeTime.changePage();
                repairWeapons();
            }
            // store gold as specified
            if (nbAttackedPlayers % params.getStoringFrequency() == 0) {
                fakeTime.changePage();
                storeGoldIntoChest();
                fakeTime.pauseWhenSafe();
            } else {
                fakeTime.changePageLong();
            }
        }
        // repair weapons for last players attacked
        if (nbAttackedPlayers % params.getRepairFrequency() != 0) {
            fakeTime.changePage();
            repairWeapons();
        }
        // store remaining gold
        if (nbAttackedPlayers % params.getStoringFrequency() != 0) {
            storeGoldIntoChest();
            fakeTime.pauseWhenSafe();
        }
        log.i(TAG, totalGoldStolen, " total gold stolen from ", nbAttackedPlayers, " players");
        log.i(TAG, "The chest now contains ", state.chestGold, " gold.");
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
        boolean success = rol.displayPlayerPage(player.getName());
        log.indent();
        if (!success) {
            log.e(TAG, "Something's wrong...");
            return -1;
        }
        log.deindent(1);

        fakeTime.actionInPage();

        log.v(TAG, "Attacking...");
        int goldStolen = rol.attack(player.getName());
        log.deindent(1);
        if (goldStolen > 0) {
            log.i(TAG, "Victory! ", goldStolen, " gold stolen from player ", player.getName(), ", current gold: ",
                    state.gold);
        } else {
            log.w(TAG, "Defeat! Player ", player.getName(), " was too sronk! Current gold: ", state.gold);
        }
        return goldStolen;
    }

    private int storeGoldIntoChest() {
        log.v(TAG, "Storing gold into the chest...");
        log.indent();
        log.v(TAG, "Displaying chest page...");
        int amount = rol.displayChestPage();
        log.v(TAG, amount + " gold to store");

        fakeTime.actionInPage();

        log.v(TAG, "Storing everything...");
        log.indent();
        boolean success = rol.storeInChest(amount);
        if (success) {
            log.v(TAG, "The gold is safe!");
        } else {
            log.v(TAG, "Something went wrong!");
        }
        log.deindent(2);
        log.i(TAG, amount, " gold stored in chest, total: " + state.chestGold);
        return amount;
    }

    private void repairWeapons() {
        log.v(TAG, "Repairing weapons...");
        log.indent();
        log.v(TAG, "Displaying weapons page...");
        int wornness = rol.displayWeaponsPage();
        log.v(TAG, "Weapons worn at ", wornness, "%");

        fakeTime.actionInPage();

        log.v(TAG, "Repair request...");
        boolean success = rol.repairWeapons();
        log.deindent(1);
        if (!success) {
            log.e(TAG, "Couldn't repair weapons, is there enough gold?");
        } else {
            log.i(TAG, "Weapons repaired");
        }
    }
}
