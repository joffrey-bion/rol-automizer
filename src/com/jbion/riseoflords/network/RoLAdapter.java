package com.jbion.riseoflords.network;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.jbion.riseoflords.model.AccountState;
import com.jbion.riseoflords.model.Player;
import com.jbion.riseoflords.network.parsers.Parser;

public class RoLAdapter {
    private static final String FAKE_USER_AGENT = "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/36.0.1985.143 Safari/537.36";

    private static final String BASE_URL_INDEX = "http://www.riseoflords.com/index.php";
    private static final String BASE_URL_GAME = "http://www.riseoflords.com/jeu.php";

    private static final String PAGE_LOGIN = "verifpass";
    private static final String PAGE_LOGOUT = "logout";
    private static final String PAGE_USERS_LIST = "main/conseil_de_guerre";
    private static final String PAGE_USER_DETAILS = "main/fiche";
    private static final String PAGE_ATTACK = "main/combats";
    private static final String PAGE_CHEST = "main/tresor";
    private static final String PAGE_WEAPONS = "main/arsenal";

    private final Random rand = new Random();
    private final CloseableHttpClient http;
    private final AccountState state;

    private final ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
        @Override
        public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        }
    };

    public RoLAdapter(AccountState state) {
        this.state = state;
        BasicCookieStore cookieStore = new BasicCookieStore();
        http = HttpClients.custom().setDefaultCookieStore(cookieStore).setUserAgent(FAKE_USER_AGENT).build();
    }

    private String randomCoord(int min, int max) {
        return String.valueOf(rand.nextInt(max - min + 1) + min);
    }

    /**
     * Builds and returns a RequestBuilder for a GET request on the specified page, based on
     * {@link #BASE_URL_GAME}.
     * 
     * @param page
     *            the page to point to
     * @return the built URL
     */
    private static RequestBuilder getGameRequest(String page) {
        return RequestBuilder.get().setUri(BASE_URL_GAME).addParameter("p", page);
    }

    /**
     * Builds and returns a RequestBuilder for a GET request on the specified page, based on
     * {@link #BASE_URL_INDEX}.
     * 
     * @param page
     *            the page to point to
     * @return the built URL
     */
    private static RequestBuilder getIndexRequest(String page) {
        return RequestBuilder.get().setUri(BASE_URL_INDEX).addParameter("p", page);
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
        HttpPost request = new HttpPost(BASE_URL_INDEX + "?p=" + PAGE_LOGIN);
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("LogPseudo", username));
            params.add(new BasicNameValuePair("LogPassword", password));
            UrlEncodedFormEntity postContent = new UrlEncodedFormEntity(params);
            request.setEntity(postContent);

            String response = http.execute(request, responseHandler);
            boolean success = response.contains("Identification réussie!");
            if (!success) {
                System.err.println(response);
            }
            return success;
        } catch (IOException e) {
            throw new IllegalStateException("Exception not handled yet.", e);
        }
    }

    /**
     * Logs the current user out.
     * 
     * @return true if the request succeeded, false otherwise
     */
    public boolean logout() {
        HttpUriRequest request = getIndexRequest(PAGE_LOGOUT).build();
        return executeForSuccess(request, r -> r.contains("Déjà inscrit? Connectez-vous"), false);
    }

    /**
     * Returns the list of users starting at the specified rank.
     * 
     * @param startRank
     *            the rank of the first user to return
     * @return 99 users at most, starting at the specified rank.
     */
    public List<Player> getPlayers(int startRank) {
        RequestBuilder builder = getGameRequest(PAGE_USERS_LIST);
        builder.addParameter("Debut", String.valueOf(startRank + 1));
        if (rand.nextBoolean()) {
            builder.addParameter("x", randomCoord(5, 35));
            builder.addParameter("y", randomCoord(5, 25));
        }
        HttpUriRequest request = builder.build();
        String response = executeForResponse(request, r -> r.contains("Recherche pseudo:"));
        return Parser.parseUserList(response);
    }

    /**
     * Displays the specified player's detail page. Used to fake a visit on the user detail page
     * before an attack. The result does not matter.
     * 
     * @param username
     *            the user to lookup
     * @return true if the request succeeded, false otherwise
     */
    public boolean displayPlayerPage(String username) {
        RequestBuilder builder = getGameRequest(PAGE_USER_DETAILS);
        builder.addParameter("voirpseudo", username);
        HttpUriRequest request = builder.build();
        return executeForSuccess(request, r -> r.contains("Seigneur " + username));
    }

    /**
     * Attacks the specified user with one game turn.
     * 
     * @param username
     *            the name of the user to attack
     * @return the gold stolen during the attack
     */
    public int attack(String username) {
        HttpPost request = new HttpPost(BASE_URL_GAME + "?p=" + PAGE_ATTACK + "&a=ok");
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("PseudoDefenseur", username));
        params.add(new BasicNameValuePair("NbToursToUse", "1"));
        setPostData(request, params);
        String response = executeForResponse(request,
                r -> r.contains("remporte le combat!") || r.contains("perd cette bataille!"));
        return Parser.parseGoldStolen(response);
    }

    /**
     * Gets the chest page from the server, and returns the amount of money that could be stored in
     * the chest.
     * 
     * @return the amount of money that could be stored in the chest, which is the current amount of
     *         gold of the player
     */
    public int displayChestPage() {
        HttpUriRequest request = getGameRequest(PAGE_CHEST).build();
        String response = executeForResponse(request, r -> r.contains("ArgentAPlacer"));
        return Parser.parseGoldAmount(response);
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
        HttpPost request = new HttpPost(BASE_URL_GAME + "?p=" + PAGE_CHEST);
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("ArgentAPlacer", String.valueOf(amount)));
        params.add(new BasicNameValuePair("x", randomCoord(10, 60)));
        params.add(new BasicNameValuePair("y", randomCoord(10, 60)));
        setPostData(request, params);
        return executeForSuccess(request, r -> Parser.parseGoldAmount(r) == 0);
    }

    /**
     * Displays the weapons page. Used to fake a visit on the weapons page before repairing or
     * buying weapons and equipment.
     * 
     * @return the percentage of wornness of the weapons
     */
    public int displayWeaponsPage() {
        HttpUriRequest request = getGameRequest(PAGE_WEAPONS).build();
        String response = executeForResponse(request, r -> r.contains("Faites votre choix"));
        return Parser.parseWeaponsWornness(response);
    }

    /**
     * Repairs weapons.
     * 
     * @return true if the repair succeeded, false otherwise
     */
    public boolean repairWeapons() {
        RequestBuilder builder = getGameRequest(PAGE_WEAPONS);
        builder.addParameter("a", "repair");
        builder.addParameter("onglet", "");
        HttpUriRequest request = builder.build();
        String response = executeForResponse(request, r -> r.contains("Faites votre choix"));
        return Parser.parseWeaponsWornness(response) == 0;
    }

    private static void setPostData(HttpPost postRequest, List<NameValuePair> params) {
        try {
            UrlEncodedFormEntity postContent = new UrlEncodedFormEntity(params);
            postRequest.setEntity(postContent);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Exception not handled yet.", e);
        }
    }

    private boolean executeForSuccess(HttpUriRequest request, Predicate<String> responseSuccessful) {
        return executeForSuccess(request, responseSuccessful, true);
    }

    private boolean executeForSuccess(HttpUriRequest request, Predicate<String> responseSuccessful, boolean updateState) {
        try {
            String response = http.execute(request, responseHandler);
            boolean success = responseSuccessful.test(response);
            if (!success) {
                System.err.println(response);
            } else if (updateState) {
                Parser.updateState(state, response);
            }
            return success;
        } catch (IOException e) {
            throw new IllegalStateException("Exception not handled yet.", e);
        }
    }

    private String executeForResponse(HttpUriRequest request, Predicate<String> responseSuccessful) {
        try {
            String response = http.execute(request, responseHandler);
            if (!responseSuccessful.test(response)) {
                System.err.println(response);
            } else {
                Parser.updateState(state, response);
            }
            return response;
        } catch (IOException e) {
            throw new IllegalStateException("Exception not handled yet.", e);
        }
    }
}
