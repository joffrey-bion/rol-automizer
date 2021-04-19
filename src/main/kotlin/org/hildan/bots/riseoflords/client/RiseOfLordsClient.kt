package org.hildan.bots.riseoflords.client

import org.hildan.bots.riseoflords.client.parsers.Parser
import org.hildan.bots.riseoflords.model.AccountState
import org.hildan.bots.riseoflords.model.AttackResult
import org.hildan.bots.riseoflords.model.Player
import java.net.CookieManager
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import kotlin.random.Random

private const val FAKE_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.90 Safari/537.36"

class LoginException(val username: String) : Exception()

class RiseOfLordsClient {

    val currentState: AccountState = AccountState()

    private val http: HttpClient = HttpClient.newBuilder()
        .cookieHandler(CookieManager())
        .followRedirects(HttpClient.Redirect.ALWAYS)
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
        if ("Identification r\u00e9ussie!" !in response) {
            throw LoginException(username)
        }
    }

    /**
     * Logs the current user out.
     *
     * @return true if the request succeeded, false otherwise
     */
    fun logout(): Boolean {
        val response = http.get(URL_INDEX, PAGE_LOGOUT)
        return "D\u00e9j\u00e0 inscrit? Connectez-vous" in response
    }

    /**
     * Displays the village page, and updates the state.
     */
    fun displayHomePage() {
        val response = http.get(URL_GAME, PAGE_HOME)
        if (response.contains("images/layout2012/carte")) {
            Parser.updateState(currentState, response)
        }
    }

    /**
     * Returns one page of the player list, starting at the specified [startRank].
     */
    fun displayPlayerListPage(startRank: Int): List<Player> {
        val response = http.get(URL_GAME, PAGE_USERS_LIST) {
            queryParam("Debut", (startRank + 1).toString())
            // when clicking the OK button (manual rank input), x/y coordinates of the click in the button are passed
            // when clicking "prev/next" arrows, the coords are not passed
            if (Random.nextBoolean()) {
                queryParam("x", randomCoord(5, 35))
                queryParam("y", randomCoord(5, 25))
            }
        }
        if ("Recherche pseudo:" !in response) {
            error("Unexpected response for player list")
        }
        Parser.updateState(currentState, response)
        return Parser.parsePlayerList(response)
    }

    /**
     * Displays the specified player's detail page. Used to fake a visit on the user detail page
     * before an attack.
     *
     * @return the specified player's current gold
     */
    fun displayPlayer(playerName: String): Int {
        val response = http.get(URL_GAME, PAGE_USER_DETAILS) {
            queryParam("voirpseudo", playerName)
        }
        if ("Seigneur $playerName" !in response) {
            error("Unexpected response for player page of player '$playerName'")
        }
        Parser.updateState(currentState, response)
        return Parser.parsePlayerGold(response)
    }

    /**
     * Attacks the user with the given [username] using [nTurns] game turns.
     */
    fun attack(username: String, nTurns: Int = 1): AttackResult {
        val response = http.post(URL_GAME, PAGE_ATTACK, {
            queryParam("a", "ok")
        }, {
            formParam("PseudoDefenseur", username)
            formParam("NbToursToUse", nTurns.toString())
        })
        return when {
            "remporte le combat!" in response -> {
                Parser.updateState(currentState, response)
                AttackResult.Victory(Parser.parseGoldStolen(response))
            }
            "perd cette bataille!" in response -> {
                Parser.updateState(currentState, response)
                AttackResult.Defeat
            }
            "temp\u00eate magique s'abat" in response -> {
                Parser.updateState(currentState, response)
                AttackResult.StormActive
            }
            else -> error("Invalid response for attack request")
        }
    }

    /**
     * Gets the chest page from the server, and returns the amount of money that could be stored in
     * the chest.
     *
     * @return the amount of money that could be stored in the chest, which is the current amount of
     * gold of the player
     */
    fun displayChestPage(): Int {
        val response = http.get(URL_GAME, PAGE_CHEST)
        if ("ArgentAPlacer" !in response) {
            error("Invalid response for chest page")
        }
        Parser.updateState(currentState, response)
        return currentState.gold
    }

    /**
     * Stores the specified [amount] of gold into the chest. The amount has to match the current gold
     * of the user, which should first be retrieved by calling [displayChestPage].
     *
     * @return true if the request succeeded, false otherwise
     */
    fun storeInChest(amount: Int): Boolean {
        val response = http.post(URL_GAME, PAGE_CHEST) {
            formParam("ArgentAPlacer", amount.toString())
            formParam("x", randomCoord(10, 60))
            formParam("y", randomCoord(10, 60))
        }
        Parser.updateState(currentState, response)
        return currentState.gold == 0
    }

    /**
     * Displays the weapons page. Used to fake a visit on the weapons page before repairing or
     * buying weapons and equipment.
     *
     * @return the percentage of wornness of the weapons
     */
    fun displayWeaponsPage(): Int {
        val response = http.get(URL_GAME, PAGE_WEAPONS)
        if ("Faites votre choix" !in response) {
            error("Invalid response for weapons page")
        }
        Parser.updateState(currentState, response)
        return Parser.parseWeaponsWornness(response)
    }

    /**
     * Repairs weapons.
     *
     * @return true if the repair succeeded, false otherwise
     */
    fun repairWeapons(): Boolean {
        val response = http.get(URL_GAME, PAGE_WEAPONS) {
            queryParam("a", "repair")
            queryParam("onglet", "")
        }
        if ("Faites votre choix" !in response) {
            error("Invalid response for weapons page")
        }
        Parser.updateState(currentState, response)
        return Parser.parseWeaponsWornness(response) == 0
    }

    /**
     * Displays the sorcery page. Used to fake a visit on the sorcery page before casting a spell.
     */
    fun displaySorceryPage() {
        val response = http.get(URL_GAME, PAGE_SORCERY)
        if ("Niveau de vos sorciers" !in response) {
            error("Invalid response for sorcerers page")
        }
        Parser.updateState(currentState, response)
    }

    /**
     * Clones the given [quantity] of sorcerers.
     *
     * @return true if the request succeeded, false otherwise
     */
    fun cloneSorcerers(quantity: Int): Boolean {
        val response = http.post(URL_GAME, PAGE_SORCERY, {
            queryParam("a", "lancer")
            queryParam("idsort", "1")
        }, {
            formParam("clonage_nombre_cible", quantity.toString())
        })
        if ("ont été clonés" !in response) {
            return false
        }
        Parser.updateState(currentState, response)
        return true
    }

    /**
     * Casts the dissipation spell to get rid of the protective aura. Useful before self-casting a
     * storm.
     *
     * @return true if the request succeeded, false otherwise
     */
    fun dissipateProtectiveAura(): Boolean {
        val response = http.get(URL_GAME, PAGE_SORCERY) {
            queryParam("a", "lancer")
            queryParam("idsort", "14")
        }
        Parser.updateState(currentState, response)
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
            queryParam("a", "lancer")
            queryParam("idsort", "5")
        }, {
            formParam("tempete_pseudo_cible", playerName)
        })
        Parser.updateState(currentState, response)
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

        const val SORCERER_CLONE_COST_GOLD = 5_000
        const val SORCERER_CLONE_COST_MANA = 35_000
    }
}

private class UriBuilder(val baseUrl: String) {
    private val queryParams: MutableList<Pair<String, String>> = ArrayList()

    fun queryParam(key: String, value: String): UriBuilder {
        queryParams.add(key to value)
        return this
    }

    fun build(): URI = URI("$baseUrl?${queryParams.urlEncodedString()}")
}

private class PostDataBuilder {
    private val formParams: MutableList<Pair<String, String>> = ArrayList()

    fun formParam(key: String, value: String) {
        formParams.add(key to value)
    }

    fun build() = formParams.urlEncodedString()
}

fun List<Pair<String, String>>.urlEncodedString() = joinToString("&") { (key, value) ->
    buildString {
        append(URLEncoder.encode(key, StandardCharsets.UTF_8))
        append("=")
        append(URLEncoder.encode(value, StandardCharsets.UTF_8))
    }
}

private fun HttpClient.get(baseUrl: String, page: String, configureUri: UriBuilder.() -> Unit = {}): String {
    val request = HttpRequest.newBuilder(uri(baseUrl, page, configureUri)).GET()
    return executeForText(request)
}

private fun HttpClient.post(
    baseUrl: String,
    page: String,
    configureUri: UriBuilder.() -> Unit = {},
    configureBody: PostDataBuilder.() -> Unit,
): String {
    val request = HttpRequest.newBuilder(uri(baseUrl, page, configureUri))
        .POST(BodyPublishers.ofString(PostDataBuilder().apply(configureBody).build()))
        .header("Content-Type", "application/x-www-form-urlencoded")
    return executeForText(request)
}

private fun uri(baseUrl: String, page: String, configure: UriBuilder.() -> Unit) =
    UriBuilder(baseUrl).queryParam("p", page).apply(configure).build()

private fun HttpClient.executeForText(requestBuilder: HttpRequest.Builder): String {
    val request = requestBuilder.header("User-Agent", FAKE_USER_AGENT).build()
    val response = send(request, HttpResponse.BodyHandlers.ofString())
    return when (response.statusCode()) {
        in 200..299 -> response.body() ?: error("No response body for $request")
        else -> error("Non-OK response status (${response.statusCode()}) for $request")
    }
}
