package com.jbion.riseoflords;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jbion.riseoflords.model.User;
import com.jbion.riseoflords.network.HttpRequestor;
import com.jbion.riseoflords.network.exceptions.WebserviceException;
import com.jbion.riseoflords.util.Log;

public class WSAdapter {
    private static final String LOG_TAG = WSAdapter.class.getSimpleName();

    private static final String UTF8 = "UTF-8";
    private static final String BASE_URL = "http://www.riseoflords.com/jeu.php";

    private static final String PAGE_USERS_LIST = "main/conseil_de_guerre";
    private static final String PAGE_USER_DETAILS = "main/fiche";
    private static final String PAGE_ATTACK = "main/combats";

    private static final String PARAM_USERS_LIST_BEGINNING = "Debut";
    private static final String PARAM_USER_DETAILS_LOGIN = "voirpseudo";
    private static final String PARAM_ATTACK = "a";
    private static final String PARAM_ATTACK_VALUE = "ok";

    private static final String POST_ATTACK_USER = "PseudoDefenseur";
    private static final String POST_ATTACK_TURNS = "NbToursToUse";

    private final HttpRequestor http = new HttpRequestor();

    private static URL getUrl(String page, String paramKey, String paramValue) {
        Map<String, String> query = new HashMap<>();
        query.put(paramKey, paramValue);
        return getPageUrl(page, query);
    }

    /**
     * Builds and returns an URL pointing to the specified page, with the specified
     * query parameters.
     * 
     * @param page
     *            the page to point to
     * @param query
     *            the query parameters to include in the URL query
     * @return the built URL
     */
    private static URL getPageUrl(String page, Map<String, String> query) {
        try {
            String url = BASE_URL + "?p=" + page;
            for (Entry<String, String> entry : query.entrySet()) {
                String key = URLEncoder.encode(entry.getKey(), UTF8);
                String value = URLEncoder.encode(entry.getValue(), UTF8);
                url += "&" + key + "=" + value;
            }
            Log.v(LOG_TAG, "getPageUrl url=" + url);
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("the provided page is malformed", e);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("unsupported encoding UTF-8...", e);
        }
    }

    /**
     * Returns the list of users starting at the specified rank.
     * 
     * @param startRank
     *            the rank of the first user to return
     * @return 99 users at most, starting at the specified rank.
     * @throws WebserviceException
     *             if an error occurred
     */
    public List<User> listUsers(int startRank) throws WebserviceException {
        http.doGet(getUrl(PAGE_USERS_LIST, PARAM_USERS_LIST_BEGINNING, String.valueOf(startRank)));
        // TODO parse user list
        return null;
    }

    /**
     * Used to fake a user detail page access on the server. THe result does not
     * matter.
     * 
     * @param user
     *            the user to lookup
     * @throws WebserviceException
     *             if an error occurred
     */
    public void getUserPage(User user) throws WebserviceException {
        http.doGet(getUrl(PAGE_USER_DETAILS, PARAM_USER_DETAILS_LOGIN, user.getName()));
    }

    public void attack(String username) {

        Map<String, String> query = new HashMap<>();
        query.put(PARAM_ATTACK, PARAM_ATTACK_VALUE);
        URL url = getPageUrl(PAGE_ATTACK, query);

        Map<String, String> post = new HashMap<>();
        post.put(POST_ATTACK_USER, username);
        post.put(POST_ATTACK_TURNS, "1");

        // TODO POST request with form-urlencoded content
        
        //http.doPost(url, post);
    }

}
