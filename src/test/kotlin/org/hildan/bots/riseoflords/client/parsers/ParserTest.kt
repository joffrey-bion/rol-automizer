package org.hildan.bots.riseoflords.client.parsers

import org.hildan.bots.riseoflords.model.AccountState
import org.hildan.bots.riseoflords.model.ExcaliburState
import java.nio.charset.Charset
import kotlin.test.*

class ParserTest {

    @Test
    fun login_success() {
        val html = readTextFromResource("/response_verifpass_success.html")
        assertTrue(Parser.isSuccessfulAuth(html))
    }

    @Test
    fun logout_success() {
        val html = readTextFromResource("/response_logout.html")
        assertTrue(Parser.isSuccessfulLogout(html))
    }

    @Test
    fun home() {
        val html = readTextFromResource("/page_home.html")
        val state = AccountState()
        Parser.updateState(state, html)
        assertEquals(385_000, state.gold)
        assertEquals(86_233_575, state.chestGold)
        assertEquals(8_388_607, state.mana)
        assertEquals(20, state.adventurins)
        assertEquals(500, state.turns)
    }

    @Test
    fun castle() {
        val html = readTextFromResource("/page_main_donjon.html")
        val castle = Parser.parseCastle(html)
        assertEquals(castle.nMessages, 0)
        assertEquals(castle.nExpectedAdventureStones, 2)
        assertEquals(castle.nPrisoners, 0)
    }

    @Test
    fun excalibur_available() {
        val html = readTextFromResource("/page_main_excalibur_available.html")
        assertEquals(Parser.parseExcaliburState(html), ExcaliburState.AVAILABLE)
    }

    @Test
    fun excalibur_alreadyTried() {
        val html = readTextFromResource("/page_main_excalibur_already_tried.html")
        assertEquals(Parser.parseExcaliburState(html), ExcaliburState.ALREADY_TRIED_TODAY)
    }

    @Test
    fun excalibur_failure() {
        val html = readTextFromResource("/page_main_excalibur_result_fail.html")
        assertTrue(Parser.isExcaliburFailure(html))
    }

    private fun readTextFromResource(resourceName: String) =
        javaClass.getResource(resourceName)?.readText(charset = Charset.forName("ISO-8859-15"))
            ?: error("Missing resource $resourceName")
}
