package com.jbion.riseoflords;

import java.util.List;
import java.util.stream.Collectors;

import com.jbion.riseoflords.model.Player;
import com.jbion.riseoflords.util.Log;

public class Sequencer {

    private static final String TAG = Sequencer.class.getSimpleName();

    private Log log = Log.get();
    private Sleeper fakeTime = new Sleeper();
    private WSAdapter rol = new WSAdapter();

    public static void main(String[] args) {
        new Sequencer().start();
    }

    private void start() {
        login("darklink", "kili");
        AttackParams params = new AttackParams();
        params.start(2300).end(3500).goldThreshold(450000);
        attackSeries(params);
    }

    /**
     * Logs in with the specified credentials, and wait for standard time.
     * 
     * @param username
     *            the username to connect with
     * @param password
     *            the password to connect with
     */
    private void login(String username, String password) {
        log.d(TAG, "Logging in with username: " + username);
        boolean success = rol.login(username, password);
        log.indent();
        if (success) {
            log.i(TAG, "Logged in with username: " + username);
        } else {
            throw new RuntimeException("Login failure.");
        }
        fakeTime.sleep(6000, 7000);
        log.deindent(1);
    }

    /**
     * Attacks as many pages of players as necessary to meet the parameters.
     * 
     * @param params
     *            the parameters for the attacks
     * @return the number of players attacked
     */
    private int attackSeries(AttackParams params) {
        int totalTurnsUsed = 0;
        while (params.getNbTurns() > 0) {
            int nTurnsUsed = attackPage(params);
            totalTurnsUsed += nTurnsUsed;
            params.decrementTurns(nTurnsUsed);
            params.nextPage();
            fakeTime.sleep(1500, 2000);
        }
        return totalTurnsUsed;
    }

    /**
     * Attacks one page of players.
     * 
     * @param params
     *            the parameters for the attacks
     * @return the number of players attacked
     */
    private int attackPage(AttackParams params) {
        int start = params.getStartRank();
        log.i(TAG, "Robbing players in ranks ", start, " to ", start + 98);
        List<Player> players = rol.getPlayers(start);
        List<Player> richPlayers = players.stream().filter(u -> u.getGold() > params.getGoldThreshold())
                .limit(params.getNbTurns()).collect(Collectors.toList());
        log.i(TAG, richPlayers.size(), " players have more than ", params.getGoldThreshold(), " gold");
        fakeTime.readPage();
        int nbUsersBeforeRepairing = 0;
        int nbUsersBeforeStoring = 0;
        for (Player player : richPlayers) {
            // attack player
            attack(player);
            nbUsersBeforeRepairing++;
            nbUsersBeforeStoring++;
            // repair weapons as specified
            if (nbUsersBeforeRepairing >= params.getRepairFrequency()) {
                fakeTime.changePage();
                repairWeapons();
                fakeTime.changePage();
                nbUsersBeforeRepairing = 0;
            }
            // store gold as specified
            if (nbUsersBeforeStoring >= params.getStoringFrequency()) {
                fakeTime.changePage();
                storeGold();
                fakeTime.pauseWhenSafe();
                nbUsersBeforeStoring = 0;
            }
            fakeTime.sleep(1000, 3000);
        }
        // store remaining gold
        if (nbUsersBeforeStoring > 0) {
            fakeTime.changePage();
            storeGold();
            fakeTime.pauseWhenSafe();
        }
        return richPlayers.size();
    }

    private void attack(Player player) {
        log.d(TAG, "Attacking player ", player.getName(), "...");
        log.indent();
        log.v(TAG, "Displaying player page...");
        boolean success = rol.displayUserPage(player.getName());
        log.indent();
        if (!success) {
            log.e(TAG, "Something's wrong...");
            return;
        }
        log.deindent(1);

        fakeTime.actionInPage();

        log.v(TAG, "Attacking...");
        success = rol.attack(player.getName());
        log.indent();
        if (success) {
            log.v(TAG, "Victory!");
        } else {
            log.v(TAG, "Defeat!");
        }
        log.deindent(2);
    }

    private int storeGold() {
        log.v(TAG, "Storing gold in chest...");
        log.indent();
        log.v(TAG, "Displaying chest page...");
        int amount = rol.getCurrentGoldFromChestPage();
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
        log.i(TAG, amount, " gold stored in chest");
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
