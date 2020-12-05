package org.hildan.bots.riseoflords.network.parsers

import org.hildan.bots.riseoflords.model.AccountState
import org.hildan.bots.riseoflords.model.Alignment
import org.hildan.bots.riseoflords.model.Army
import org.hildan.bots.riseoflords.model.Player
import org.hildan.bots.riseoflords.network.RiseOfLordsClient
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URL
import javax.imageio.ImageIO

object Parser {

    private val logger = LoggerFactory.getLogger(Parser::class.java)

    /**
     * Updates the specified [state] based on the top elements of the specified page [response].
     */
    fun updateState(state: AccountState, response: String) {
        val body = Jsoup.parse(response).body()
        state.gold = findValueInTag(body, "onmouseover", "A chaque tour de jeu")
        state.chestGold = findValueInTag(body, "onmouseover", "Votre coffre magique")
        state.mana = findValueInTag(body, "onmouseover", "Votre mana repr\u00e9sente")
        state.turns = findValueInTag(body, "onmouseover", "Un nouveau tour de jeu")
        state.adventurins = findValueInTag(body, "href", "main/aventurines_detail")
    }

    private fun findValueInTag(root: Element, attrKey: String, attrContains: String): Int {
        val elts = root.getElementsByAttributeValueContaining(attrKey, attrContains)
        val imgSrc = elts[0].child(0).attr("src")
        return imgSrc.substringAfter("num=", "0")
            .substringBefore('&')
            .replace(".", "")
            .toInt()
    }

    /**
     * Parses the list of players contained in the specified [playerListPageResponse].
     */
    fun parsePlayerList(playerListPageResponse: String): List<Player> {
        val body = Jsoup.parse(playerListPageResponse).body()
        val elts = body.getElementsByAttributeValueContaining("href", "main/fiche&voirpseudo=")
        return elts.map {
            val usernameCell = it.parent()
            assert(usernameCell.tagName() == "td")
            val userRow = usernameCell.parent()
            assert(userRow.tagName() == "tr")
            parsePlayer(userRow)
        }
    }

    /**
     * Creates a new player from the cells in the specified `<tr>` element [playerRow].
     */
    private fun parsePlayer(playerRow: Element): Player {
        assert(playerRow.tagName() == "tr")
        val fields = playerRow.getElementsByTag("td")

        val rank = getTextAsNumber(fields[0])
        val name = fields[2].child(0).text().trim()

        val goldElt = fields[3]
        val gold = if (goldElt.hasText()) getTextAsNumber(goldElt) else getGoldFromImgElement(goldElt.child(0))

        val army = Army[fields[4].text().trim()]
        val alignment = Alignment[fields[5].child(0).text().trim()]

        return Player(
            rank = rank,
            name = name,
            gold = gold,
            army = army,
            alignment = alignment,
        )
    }

    /**
     * Gets the amount of stolen golden from the given [attackReportResponse], or -1 in case of read error.
     */
    fun parseGoldStolen(attackReportResponse: String): Int {
        val body = Jsoup.parse(attackReportResponse).body()
        val elts = body.getElementsByAttributeValue("class", "combat_gagne")
        if (elts.size == 0) {
            return -1
        }
        val divVictory = elts[0].parent().parent()
        return getTextAsNumber(divVictory.getElementsByTag("b")[0])
    }

    /**
     * Gets and parses the text contained in the specified [numberElement] as an [Int].
     */
    private fun getTextAsNumber(numberElement: Element): Int {
        val number = numberElement.text().trim { it <= ' ' }
        return Integer.valueOf(number.replace(".", ""))
    }

    /**
     * Parses the [weaponsPageResponse] to return the current state of wornness of the weapons (in percents).
     */
    fun parseWeaponsWornness(weaponsPageResponse: String): Int {
        val body = Jsoup.parse(weaponsPageResponse).body()
        val elts = body.getElementsByAttributeValueContaining("title", "Armes endommag\u00e9es")
        val input = elts[0]
        val value = input.text().trim { it <= ' ' }
        return if (value.endsWith("%")) {
            Integer.valueOf(value.substring(0, value.length - 1))
        } else {
            -1
        }
    }

    /**
     * Parses the amount of gold on the specified [playerPageResponse], or -1 in case of error.
     */
    fun parsePlayerGold(playerPageResponse: String): Int {
        val body = Jsoup.parse(playerPageResponse).body()
        val elts = body.getElementsByAttributeValueContaining("src", "aff_montant")
        val img = elts[0]
        return getGoldFromImgElement(img)
    }

    /**
     * Uses an OCR to recognize a number in the specified `<img>` element [goldImageElement], or -1 in case of error.
     */
    private fun getGoldFromImgElement(goldImageElement: Element): Int {
        assert(goldImageElement.tagName() == "img")
        val goldImgUrl = goldImageElement.attr("src")
        assert(goldImgUrl.isNotEmpty()) { "emtpy gold image url" }
        try {
            val img = ImageIO.read(URL(RiseOfLordsClient.BASE_URL + "/" + goldImgUrl))
            return GoldImageOCR.readAmount(img)
        } catch (e: IOException) {
            logger.error("Error downloading the gold image at {}", goldImgUrl)
        }
        return -1
    }
}