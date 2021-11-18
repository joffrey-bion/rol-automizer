package org.hildan.bots.riseoflords.client.parsers

import org.hildan.bots.riseoflords.model.AccountState
import org.hildan.bots.riseoflords.model.Alignment
import org.hildan.bots.riseoflords.model.Army
import org.hildan.bots.riseoflords.model.Player
import org.hildan.bots.riseoflords.client.RiseOfLordsClient
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URL
import javax.imageio.ImageIO

object Parser {

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
            val usernameCell = it.parent() ?: error("username cell element not found")
            require(usernameCell.tagName() == "td")
            val userRow = usernameCell.parent() ?: error("user row element not found")
            require(userRow.tagName() == "tr")
            parsePlayer(userRow)
        }
    }

    /**
     * Creates a new player from the cells in the specified `<tr>` element [playerRow].
     */
    private fun parsePlayer(playerRow: Element): Player {
        require(playerRow.tagName() == "tr")
        val fields = playerRow.getElementsByTag("td")

        val rank = fields[0].textAsNumber()
        val name = fields[2].child(0).text().trim()

        val goldElt = fields[3]
        val gold = if (goldElt.hasText()) goldElt.textAsNumber() else getGoldFromImgElement(goldElt.child(0))

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
     * Gets the amount of stolen golden from the given [attackReportResponse], or null in case of read error.
     */
    fun parseGoldStolen(attackReportResponse: String): Int {
        val body = Jsoup.parse(attackReportResponse).body()
        val victoryText = body.getElementsByAttributeValue("class", "combat_gagne").single()
        val divVictory = victoryText.parent()?.parent() ?: error("Victory div element not found")
        return divVictory.getElementsByTag("b")[0].textAsNumber()
    }

    /**
     * Gets and parses this element's text as an [Int].
     */
    private fun Element.textAsNumber(): Int = text().trim().replace(".", "").toInt()

    /**
     * Parses the [weaponsPageResponse] to return the current state of wornness of the weapons (in percents).
     */
    fun parseWeaponsWornness(weaponsPageResponse: String): Int {
        val body = Jsoup.parse(weaponsPageResponse).body()
        val elts = body.getElementsByAttributeValueContaining("title", "Armes endommag\u00e9es")
        val input = elts[0]
        val value = input.text().trim()
        return value.removeSuffix("%").toInt()
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
        require(goldImageElement.tagName() == "img")
        val goldImgUrl = goldImageElement.attr("src")
        require(goldImgUrl.isNotEmpty()) { "empty gold image url" }
        val img = ImageIO.read(URL(RiseOfLordsClient.BASE_URL + "/" + goldImgUrl))
        return GoldImageOCR.readAmount(img)
    }
}
