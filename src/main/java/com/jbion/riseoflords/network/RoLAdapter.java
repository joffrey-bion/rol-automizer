package com.jbion.riseoflords.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;

import com.jbion.riseoflords.model.AccountState;
import com.jbion.riseoflords.model.Player;
import com.jbion.riseoflords.network.parsers.Parser;

public class RoLAdapter {

    private static final String URL_INDEX = "http://www.riseoflords.com/index.php";
    private static final String URL_GAME = "http://www.riseoflords.com/jeu.php";

    private static final String PAGE_LOGIN = "verifpass";
    private static final String PAGE_LOGOUT = "logout";
    private static final String PAGE_HOME = "main/carte_village";
    private static final String PAGE_USERS_LIST = "main/conseil_de_guerre";
    private static final String PAGE_USER_DETAILS = "main/fiche";
    private static final String PAGE_ATTACK = "main/combats";
    private static final String PAGE_CHEST = "main/tresor";
    private static final String PAGE_WEAPONS = "main/arsenal";
    private static final String PAGE_SORCERY = "main/autel_sorciers";

    public static final int ERROR_REQUEST = -1;
    public static final int ERROR_STORM_ACTIVE = -2;

    private final Random rand = new Random();
    private final RequestSender http;
    private final AccountState state;

    public RoLAdapter() {
        this.state = new AccountState();
        http = new RequestSender();
    }

    private String randomCoord(int min, int max) {
        return String.valueOf(rand.nextInt(max - min + 1) + min);
    }

    public AccountState getCurrentState() {
        return state;
    }

    /**
     * Performs the login request with the specified credentials. One needs to wait at least 5-6
     * seconds to fake real login.
     *
     * @param username
     *            the username to use
     * @param password
     *            the password to use
     * @return true for successful login, false otherwise
     */
    public boolean login(String username, String password) {
        HttpPost postRequest = Request.from(URL_INDEX, PAGE_LOGIN) //
                .addPostData("LogPseudo", username) //
                .addPostData("LogPassword", password) //
                .post();
        return http.execute(postRequest, r -> r.contains("Identification réussie!"));
    }

    /**
     * Logs the current user out.
     *
     * @return true if the request succeeded, false otherwise
     */
    public boolean logout() {
        HttpGet getRequest = Request.from(URL_INDEX, PAGE_LOGOUT).get();
        return http.execute(getRequest, r -> r.contains("Déjà inscrit? Connectez-vous"));
    }

    /**
     * Displays the village page, and updates the state.
     */
    public void homePage() {
        HttpGet getRequest = Request.from(URL_GAME, PAGE_HOME).get();
        String response = http.execute(getRequest);
        if (response.contains("images/layout2012/carte")) {
            Parser.updateState(state, response);
        }
    }

    /**
     * Returns a list of 99 users, starting at the specified rank.
     *
     * @param startRank
     *            the rank of the first user to return
     * @return 99 users at most, starting at the specified rank.
     */
    public List<Player> listPlayers(int startRank) {
        Request builder = Request.from(URL_GAME, PAGE_USERS_LIST);
        builder.addParameter("Debut", String.valueOf(startRank + 1));
        if (rand.nextBoolean()) {
            builder.addParameter("x", randomCoord(5, 35));
            builder.addParameter("y", randomCoord(5, 25));
        }

        String response = http.execute(builder.get());
        if (response.contains("Recherche pseudo:")) {
            Parser.updateState(state, response);
            return Parser.parsePlayerList(response);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Displays the specified player's detail page. Used to fake a visit on the user detail page
     * before an attack. The result does not matter.
     *
     * @param playerName
     *            the name of the player to lookup
     * @return the specified player's current gold, or {@link #ERROR_REQUEST} if the request failed
     */
    public int displayPlayer(String playerName) {
        HttpGet request = Request.from(URL_GAME, PAGE_USER_DETAILS) //
                .addParameter("voirpseudo", playerName) //
                .get();
        String response = http.execute(request);
        if (response.contains("Seigneur " + playerName)) {
            Parser.updateState(state, response);
            return Parser.parsePlayerGold(response);
        } else {
            return ERROR_REQUEST;
        }
    }

    /**
     * Attacks the specified user with one game turn.
     *
     * @param username
     *            the name of the user to attack
     * @return the gold stolen during the attack, or {@link #ERROR_REQUEST} if the request failed
     */
    public int attack(String username) {
        HttpPost request = Request.from(URL_GAME, PAGE_ATTACK) //
                .addParameter("a", "ok") //
                .addPostData("PseudoDefenseur", username) //
                .addPostData("NbToursToUse", "1") //
                .post();
        String response = http.execute(request);
        if (response.contains("remporte le combat!") || response.contains("perd cette bataille!")) {
            Parser.updateState(state, response);
            return Parser.parseGoldStolen(response);
        } else if (response.contains("tempête magique s'abat")) {
            Parser.updateState(state, response);
            return ERROR_STORM_ACTIVE;
        } else {
            return ERROR_REQUEST;
        }
    }

    /**
     * Gets the chest page from the server, and returns the amount of money that could be stored in
     * the chest.
     *
     * @return the amount of money that could be stored in the chest, which is the current amount of
     *         gold of the player, or {@link #ERROR_REQUEST} if the request failed
     */
    public int displayChestPage() {
        HttpGet request = Request.from(URL_GAME, PAGE_CHEST).get();
        String response = http.execute(request);
        if (response.contains("ArgentAPlacer")) {
            Parser.updateState(state, response);
            return state.gold;
        } else {
            return ERROR_REQUEST;
        }
    }

    /**
     * Stores the specified amount of gold into the chest. The amount has to match the current gold
     * of the user, which should first be retrieved by calling {@link #displayChestPage()}.
     *
     * @param amount
     *            the amount of gold to store into the chest
     * @return true if the request succeeded, false otherwise
     */
    public boolean storeInChest(int amount) {
        HttpPost request = Request.from(URL_GAME, PAGE_CHEST) //
                .addPostData("ArgentAPlacer", String.valueOf(amount)) //
                .addPostData("x", randomCoord(10, 60)) //
                .addPostData("y", randomCoord(10, 60)) //
                .post();
        String response = http.execute(request);
        Parser.updateState(state, response);
        return state.gold == 0;
    }

    /**
     * Displays the weapons page. Used to fake a visit on the weapons page before repairing or
     * buying weapons and equipment.
     *
     * @return the percentage of wornness of the weapons, or {@link #ERROR_REQUEST} if the request
     *         failed
     */
    public int displayWeaponsPage() {
        HttpGet request = Request.from(URL_GAME, PAGE_WEAPONS).get();
        String response = http.execute(request);
        if (response.contains("Faites votre choix")) {
            return Parser.parseWeaponsWornness(response);
        } else {
            return ERROR_REQUEST;
        }
    }

    /**
     * Repairs weapons.
     *
     * @return true if the repair succeeded, false otherwise
     */
    public boolean repairWeapons() {
        HttpGet request = Request.from(URL_GAME, PAGE_WEAPONS) //
                .addParameter("a", "repair") //
                .addParameter("onglet", "") //
                .get();
        String response = http.execute(request);
        if (response.contains("Faites votre choix")) {
            return Parser.parseWeaponsWornness(response) == 0;
        } else {
            return false;
        }
    }

    /**
     * Displays the sorcery page. Used to fake a visit on the sorcery page before casting a spell.
     *
     * @return the available mana, or {@link #ERROR_REQUEST} if the request failed
     */
    public int displaySorceryPage() {
        HttpUriRequest request = Request.from(URL_GAME, PAGE_SORCERY).get();
        String response = http.execute(request);
        if (response.contains("Niveau de vos sorciers")) {
            return state.mana;
        } else {
            return ERROR_REQUEST;
        }
    }

    /**
     * Casts the dissipation spell to get rid of the protective aura. Useful before self-casting a
     * storm.
     *
     * @return true if the request succeeded, false otherwise
     */
    public boolean dissipateProtectiveAura() {
        HttpGet request = Request.from(URL_GAME, PAGE_SORCERY) //
                .addParameter("a", "lancer") //
                .addParameter("idsort", "14") //
                .get();
        String response = http.execute(request);
        Parser.updateState(state, response);
        return true; // TODO handle failure
    }

    /**
     * Casts a magic storm on the specified player.
     *
     * @param playerName
     *            the amount of gold to store into the chest
     * @return true if the request succeeded, false otherwise
     */
    public boolean castMagicStorm(String playerName) {
        HttpPost request = Request.from(URL_GAME, PAGE_SORCERY) //
                .addParameter("a", "lancer") //
                .addParameter("idsort", "5") //
                .addPostData("tempete_pseudo_cible", playerName) //
                .post();
        String response = http.execute(request);
        Parser.updateState(state, response);
        return true; // TODO handle failure
    }
}
