package com.jbion.riseoflords.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

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

    public RoLAdapter() {
        BasicCookieStore cookieStore = new BasicCookieStore();
        http = HttpClients.custom().setDefaultCookieStore(cookieStore).setUserAgent(FAKE_USER_AGENT).build();
    }

    private String randomCoord(int min, int max) {
        return String.valueOf(rand.nextInt(max - min + 1) + min);
    }

    /**
     * Builds and returns a RequestBuilder for a GET request on the specified page.
     * 
     * @param page
     *            the page to point to
     * @return the built URL
     */
    private static RequestBuilder getRequest(String page) {
        return RequestBuilder.get().setUri(BASE_URL_GAME).addParameter("p", page);
    }

    /**
     * Performs the login request with the specified credentials. One needs to wait
     * at least 5-6 seconds to fake real login.
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
        HttpGet request = new HttpGet(BASE_URL_INDEX + "?p=" + PAGE_LOGOUT);
        try {
            String response = http.execute(request, responseHandler);
            boolean success = response.contains("Déjà inscrit? Connectez-vous");
            if (!success) {
                System.err.println(response);
            }
            return success;
        } catch (IOException e) {
            throw new IllegalStateException("Exception not handled yet.", e);
        }
    }

    /**
     * Returns the list of users starting at the specified rank.
     * 
     * @param startRank
     *            the rank of the first user to return
     * @return 99 users at most, starting at the specified rank.
     */
    public List<Player> getPlayers(int startRank) {
        RequestBuilder builder = getRequest(PAGE_USERS_LIST);
        builder.addParameter("Debut", String.valueOf(startRank + 1));
        if (rand.nextBoolean()) {
            builder.addParameter("x", randomCoord(5, 35));
            builder.addParameter("y", randomCoord(5, 25));
        }
        HttpUriRequest request = builder.build();
        try {
            String response = http.execute(request, responseHandler);
            return Parser.parseUserList(response);
        } catch (IOException e) {
            throw new IllegalStateException("Exception not handled yet.", e);
        }
    }

    /**
     * Displays the specified user's detail page. Used to fake a visit on the user
     * detail page before an attack. The result does not matter.
     * 
     * @param username
     *            the user to lookup
     * @return true if the request succeeded, false otherwise
     */
    public boolean displayUserPage(String username) {
        RequestBuilder builder = getRequest(PAGE_USER_DETAILS);
        builder.addParameter("voirpseudo", username);
        HttpUriRequest request = builder.build();
        try {
            String response = http.execute(request, responseHandler);
            boolean success = response.contains("Seigneur " + username);
            if (!success) {
                System.err.println(response);
            }
            return success;
        } catch (IOException e) {
            throw new IllegalStateException("Exception not handled yet.", e);
        }
    }

    /**
     * Attacks the specified user with one game turn.
     * 
     * @param username
     *            the name of the user to attack
     * @return the gold stolen during the attack
     */
    public int attack(String username) {
        HttpPost request = new HttpPost(BASE_URL_GAME + "?p=" + PAGE_ATTACK + "&" + "a" + "=" + "ok");
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("PseudoDefenseur", username));
            params.add(new BasicNameValuePair("NbToursToUse", "1"));
            UrlEncodedFormEntity postContent = new UrlEncodedFormEntity(params);
            request.setEntity(postContent);

            String response = http.execute(request, responseHandler);
            boolean success = response.contains("remporte le combat!");
            if (!success) {
                System.err.println(response);
            }
            return Parser.parseGoldStolen(response);
        } catch (IOException e) {
            throw new IllegalStateException("Exception not handled yet.", e);
        }
    }

    /**
     * Gets the chest page from the server, and returns the amount of money that
     * could be stored in the chest.
     * 
     * @return the amount of money that could be stored in the chest, which is the
     *         current amount of gold of the player
     */
    public int getCurrentGoldFromChestPage() {
        try {
            String response = http.execute(getRequest(PAGE_CHEST).build(), responseHandler);
            if (response.contains("ArgentAPlacer")) {
                return Parser.parseGoldAmount(response);
            } else {
                System.err.println(response);
                throw new IllegalStateException("Chest page failed");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Exception not handled yet.", e);
        }
    }

    /**
     * Stores the specified amount of gold into the chest. The amount has to match
     * the current gold of the user, which should first be retrieved by calling
     * {@link #getCurrentGoldFromChestPage()}.
     * 
     * @param amount
     *            the amount of gold to store into the chest
     * @return true if the request succeeded, false otherwise
     */
    public boolean storeInChest(int amount) {
        HttpPost request = new HttpPost(BASE_URL_GAME + "?p=" + PAGE_CHEST);
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("ArgentAPlacer", String.valueOf(amount)));
            params.add(new BasicNameValuePair("x", randomCoord(10, 60)));
            params.add(new BasicNameValuePair("y", randomCoord(10, 60)));
            UrlEncodedFormEntity postContent = new UrlEncodedFormEntity(params);
            request.setEntity(postContent);

            String response = http.execute(request, responseHandler);
            boolean success = Parser.parseGoldAmount(response) == 0;
            if (!success) {
                System.err.println(response);
            }
            return success;
        } catch (IOException e) {
            throw new IllegalStateException("Exception not handled yet.", e);
        }
    }

    /**
     * Displays the weapons page. Used to fake a visit on the weapons page before
     * repairing or buying weapons and equipment. The result does not matter.
     * 
     * @return the percentage of wornness of the weapons
     */
    public int displayWeaponsPage() {
        RequestBuilder builder = getRequest(PAGE_WEAPONS);
        HttpUriRequest request = builder.build();
        try {
            String response = http.execute(request, responseHandler);
            boolean success = response.contains("Faites votre choix");
            if (!success) {
                System.err.println(response);
            }
            return Parser.parseWeaponsWornness(response);
        } catch (IOException e) {
            throw new IllegalStateException("Exception not handled yet.", e);
        }
    }

    /**
     * Repairs weapons.
     * 
     * @return true if the repair succeeded, false otherwise
     */
    public boolean repairWeapons() {
        RequestBuilder builder = getRequest(PAGE_WEAPONS);
        builder.addParameter("a", "repair");
        builder.addParameter("onglet", "");
        HttpUriRequest request = builder.build();
        try {
            String response = http.execute(request, responseHandler);
            boolean success = response.contains("Faites votre choix");
            if (!success) {
                System.err.println(response);
            }
            return Parser.parseWeaponsWornness(response) == 0;
        } catch (IOException e) {
            throw new IllegalStateException("Exception not handled yet.", e);
        }
    }

}
