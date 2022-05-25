package org.hildan.bots.riseoflords.client.parsers

import org.hildan.bots.riseoflords.model.ExcaliburState
import java.nio.charset.Charset
import kotlin.test.*

class ParserTest {

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

    private fun readTextFromResource(resourceName: String) =
        javaClass.getResource(resourceName)?.readText(charset = Charset.forName("ISO-8859-15"))
            ?: error("Missing resource $resourceName")
}
