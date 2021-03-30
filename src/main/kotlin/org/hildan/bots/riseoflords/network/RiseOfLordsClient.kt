package org.hildan.bots.riseoflords.network

import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.BasicCookieStore
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.hildan.bots.riseoflords.model.AccountState
import org.hildan.bots.riseoflords.model.Player
import org.hildan.bots.riseoflords.network.parsers.Parser
import java.util.*
import kotlin.random.Random

private const val FAKE_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.90 Safari/537.36"

class LoginException(val username: String, val password: String) : Exception()

class RiseOfLordsClient {

    val currentState: AccountState = AccountState()

    private val http: CloseableHttpClient = HttpClients.custom()
        .setDefaultCookieStore(BasicCookieStore())
        .setUserAgent(FAKE_USER_AGENT)
        .build()

    private fun randomCoord(min: Int, max: Int): String = Random.nextInt(min, max).toString()

    /**
     * Performs the login request with the specified credentials. One needs to wait at least 5-6
     * seconds to fake real login.
     *
     * @param username the username to use
     * @param password the password to use
     * @throws LoginException if the login operation failed
     */
    fun login(username: String, password: String) {
        val response = http.post(URL_INDEX, PAGE_LOGIN) {
            formParam("LogPseudo", username)
            formParam("LogPassword", password)
        }
        if (response == null || !response.contains("Identification r\u00e9ussie!")) {
            throw LoginException(username, password)
        }
    }

    /**
     * Logs the current user out.
     *
     * @return true if the request succeeded, false otherwise
     */
    fun logout(): Boolean {
        val response = http.get(URL_INDEX, PAGE_LOGOUT)
        return response?.contains("D\u00e9j\u00e0 inscrit? Connectez-vous") ?: false
    }

    /**
     * Displays the village page, and updates the state.
     */
    fun displayHomePage() {
        val response = http.get(URL_GAME, PAGE_HOME) ?: error("displayHomePage request failed")
        if (response.contains("images/layout2012/carte")) {
            Parser.updateState(currentState, response)
        }
    }

    /**
     * Returns a list of 99 users, starting at the specified rank.
     *
     * @param startRank
     * the rank of the first user to return
     * @return 99 users at most, starting at the specified rank.
     */
    fun listPlayers(startRank: Int): List<Player> {
        val nullableResponse = http.get(URL_GAME, PAGE_USERS_LIST) {
            addParameter("Debut", (startRank + 1).toString())
            if (Random.nextBoolean()) {
                addParameter("x", randomCoord(5, 35))
                addParameter("y", randomCoord(5, 25))
            }
        }
        val response = nullableResponse ?: error("listPlayer request failed")
        return if ("Recherche pseudo:" in response) {
            Parser.updateState(currentState, response)
            Parser.parsePlayerList(response)
        } else {
            ArrayList()
        }
    }

    /**
     * Displays the specified player's detail page. Used to fake a visit on the user detail page
     * before an attack.
     *
     * @return the specified player's current gold, or [ERROR_REQUEST] if the request failed
     */
    fun displayPlayer(playerName: String): Int {
        val response = http.get(URL_GAME, PAGE_USER_DETAILS) {
            addParameter("voirpseudo", playerName)
        }
        return if (response != null && "Seigneur $playerName" in response) {
            Parser.updateState(currentState, response)
            Parser.parsePlayerGold(response)
        } else {
            ERROR_REQUEST
        }
    }

    /**
     * Attacks the user with the given [username] using one game turn.
     *
     * @return the gold stolen during the attack, or [ERROR_REQUEST] if the request failed
     */
    fun attack(username: String): Int {
        val response = http.post(URL_GAME, PAGE_ATTACK, {
            addParameter("a", "ok")
        }, {
            formParam("PseudoDefenseur", username)
            formParam("NbToursToUse", "1")
        })
        return when {
            response == null -> ERROR_REQUEST
            "remporte le combat!" in response || "perd cette bataille!" in response -> {
                Parser.updateState(currentState, response)
                Parser.parseGoldStolen(response)
            }
            "temp\u00eate magique s'abat" in response -> {
                Parser.updateState(currentState, response)
                ERROR_STORM_ACTIVE
            }
            else -> ERROR_REQUEST
        }
    }

    /**
     * Gets the chest page from the server, and returns the amount of money that could be stored in
     * the chest.
     *
     * @return the amount of money that could be stored in the chest, which is the current amount of
     * gold of the player, or [ERROR_REQUEST] if the request failed
     */
    fun displayChestPage(): Int {
        val response = http.get(URL_GAME, PAGE_CHEST) ?: return ERROR_REQUEST
        return if ("ArgentAPlacer" in response) {
            Parser.updateState(currentState, response)
            currentState.gold
        } else {
            ERROR_REQUEST
        }
    }

    /**
     * Stores the specified [amount] of gold into the chest. The amount has to match the current gold
     * of the user, which should first be retrieved by calling [displayChestPage].
     *
     * @return true if the request succeeded, false otherwise
     */
    fun storeInChest(amount: Int): Boolean {
        val response = http.post(URL_GAME, PAGE_CHEST) {
            formParam("ArgentAPlacer", amount.toString()) //
            formParam("x", randomCoord(10, 60)) //
            formParam("y", randomCoord(10, 60)) //
        }
        Parser.updateState(currentState, response ?: return false)
        return currentState.gold == 0
    }

    /**
     * Displays the weapons page. Used to fake a visit on the weapons page before repairing or
     * buying weapons and equipment.
     *
     * @return the percentage of wornness of the weapons, or [.ERROR_REQUEST] if the request
     * failed
     */
    fun displayWeaponsPage(): Int {
        val response = http.get(URL_GAME, PAGE_WEAPONS) ?: return ERROR_REQUEST
        return if ("Faites votre choix" in response) {
            Parser.updateState(currentState, response)
            Parser.parseWeaponsWornness(response)
        } else {
            ERROR_REQUEST
        }
    }

    /**
     * Repairs weapons.
     *
     * @return true if the repair succeeded, false otherwise
     */
    fun repairWeapons(): Boolean {
        val response = http.get(URL_GAME, PAGE_WEAPONS) {
            addParameter("a", "repair")
            addParameter("onglet", "")
        }
        if (response == null || "Faites votre choix" !in response) {
            return false
        }
        Parser.updateState(currentState, response)
        return Parser.parseWeaponsWornness(response) == 0
    }

    /**
     * Displays the sorcery page. Used to fake a visit on the sorcery page before casting a spell.
     *
     * @return the available mana, or [.ERROR_REQUEST] if the request failed
     */
    fun displaySorceryPage(): Int {
        val response = http.get(URL_GAME, PAGE_SORCERY) ?: return ERROR_REQUEST
        return if ("Niveau de vos sorciers" in response) {
            Parser.updateState(currentState, response)
            currentState.mana
        } else {
            ERROR_REQUEST
        }
    }

    /**
     * Casts the dissipation spell to get rid of the protective aura. Useful before self-casting a
     * storm.
     *
     * @return true if the request succeeded, false otherwise
     */
    fun dissipateProtectiveAura(): Boolean {
        val response = http.get(URL_GAME, PAGE_SORCERY) {
            addParameter("a", "lancer")
            addParameter("idsort", "14")
        }
        Parser.updateState(currentState, response ?: return false)
        return true // TODO handle failure
    }

    /**
     * Casts a magic storm on the specified player.
     *
     * @param playerName the name of the player on which to cast a magic storm
     * @return true if the request succeeded, false otherwise
     */
    fun castMagicStorm(playerName: String): Boolean {
        val response = http.post(URL_GAME, PAGE_SORCERY, {
            addParameter("a", "lancer")
            addParameter("idsort", "5")
        }, {
            formParam("tempete_pseudo_cible", playerName)
        })
        Parser.updateState(currentState, response ?: return false)
        return true // TODO handle failure
    }

    companion object {
        const val BASE_URL = "https://www.riseoflords.com"
        private const val URL_INDEX = "$BASE_URL/index.php"
        private const val URL_GAME = "$BASE_URL/jeu.php"
        private const val PAGE_LOGIN = "verifpass"
        private const val PAGE_LOGOUT = "logout"
        private const val PAGE_HOME = "main/carte_village"
        private const val PAGE_USERS_LIST = "main/conseil_de_guerre"
        private const val PAGE_USER_DETAILS = "main/fiche"
        private const val PAGE_ATTACK = "main/combats"
        private const val PAGE_CHEST = "main/tresor"
        private const val PAGE_WEAPONS = "main/arsenal"
        private const val PAGE_SORCERY = "main/autel_sorciers"
        const val ERROR_REQUEST = -1
        const val ERROR_STORM_ACTIVE = -2
    }
}

class PostDataBuilder(
    val postData: MutableList<NameValuePair> = ArrayList()
) {
    fun formParam(key: String, value: String) {
        postData.add(BasicNameValuePair(key, value))
    }
}

private fun CloseableHttpClient.get(baseUrl: String, page: String, configureUri: URIBuilder.() -> Unit = {}): String? {
    val request = HttpGet(uri(baseUrl, page, configureUri))
    return executeForText(request)
}

private fun CloseableHttpClient.post(
    baseUrl: String, page: String, configureUri: URIBuilder.() -> Unit = {}, configureBody: PostDataBuilder.() -> Unit
): String? {
    val request = HttpPost(uri(baseUrl, page, configureUri)).apply {
        entity = UrlEncodedFormEntity(PostDataBuilder().apply(configureBody).postData)
    }
    return executeForText(request)
}

private fun uri(baseUrl: String, page: String, configureUri: URIBuilder.() -> Unit) =
    URIBuilder(baseUrl).addParameter("p", page).apply(configureUri).build()

private fun CloseableHttpClient.executeForText(request: HttpUriRequest?): String? = execute(request) { response ->
    when (val status = response.statusLine.statusCode) {
        in 200..299 -> response.entity?.let { EntityUtils.toString(it) }
        else -> error("Unexpected response status: $status")
    }
}
