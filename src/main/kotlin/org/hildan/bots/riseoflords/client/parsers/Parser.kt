package org.hildan.bots.riseoflords.client.parsers

import org.hildan.bots.riseoflords.client.RiseOfLordsClient
import org.hildan.bots.riseoflords.model.*
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
        state.gold = body.findValueInImgTagUnder("onmouseover", "A chaque tour de jeu").toLong()
        state.chestGold = body.findValueInImgTagUnder("onmouseover", "Votre coffre magique est le seul").toLong()
        state.mana = body.findValueInImgTagUnder("onmouseover", "Votre mana repr\u00e9sente").toInt()
        state.turns = body.findValueInImgTagUnder("onmouseover", "Un nouveau tour de jeu").toInt()
        state.adventurins = body.findValueInImgTagUnder("href", "main/aventurines_detail").toInt()
    }

    private fun Element.findValueInImgTagUnder(attrKey: String, attrContains: String): String {
        val nestedImgTag = getElementByAttributeValueContaining(attrKey, attrContains).child(0)
        val imgSrc = nestedImgTag.attr("src")
        return imgSrc.substringAfter("num=", "0").substringBefore('&').replace(".", "")
    }

    /**
     * Updates the specified [state] based on the top elements of the specified page [response].
     */
    fun parseCastle(response: String): Castle {
        val body = Jsoup.parse(response).body()
        return Castle(
            nMessages = body.findIntTextInElementByAttrValueContaining("onmouseover", "Nouvelles missives reçues"),
            nExpectedAdventureStones = body.findIntTextInElementByAttrValueContaining("onmouseover", "Prévision du nombre d\\'aventurines générées à la prochaine journée"),
            nPrisoners = body.findIntTextInElementByAttrValueContaining("onmouseover", "Nombre de prisonniers dans vos geoles"),
        )
    }

    private fun Element.findIntTextInElementByAttrValueContaining(attrKey: String, attrContains: String): Int {
        val element = getElementByAttributeValueContaining(attrKey, attrContains)
        return element.text().toInt()
    }

    private fun Element.getElementByAttributeValueContaining(attrKey: String, attrContains: String): Element {
        return getElementsByAttributeValueContaining(attrKey, attrContains).single()
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

        val rank = fields[0].textAsLong().toInt()
        val name = fields[2].child(0).text().trim()

        val goldElt = fields[3]
        val gold = if (goldElt.hasText()) goldElt.textAsLong() else getGoldFromImgElement(goldElt.child(0))

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
    fun parseGoldStolen(attackReportResponse: String): Long {
        val body = Jsoup.parse(attackReportResponse).body()
        val victoryText = body.getElementsByAttributeValue("class", "combat_gagne").single()
        val divVictory = victoryText.parent()?.parent() ?: error("Victory div element not found")
        return divVictory.getElementsByTag("b")[0].textAsLong()
    }

    /**
     * Gets and parses this element's text as an [Int].
     */
    private fun Element.textAsLong(): Long = text().trim().replace(".", "").toLong()

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
    fun parsePlayerGold(playerPageResponse: String): Long {
        val body = Jsoup.parse(playerPageResponse).body()
        val elts = body.getElementsByAttributeValueContaining("src", "aff_montant")
        val img = elts[0]
        return getGoldFromImgElement(img)
    }

    /**
     * Uses an OCR to recognize a number in the specified `<img>` element [goldImageElement], or -1 in case of error.
     */
    private fun getGoldFromImgElement(goldImageElement: Element): Long {
        require(goldImageElement.tagName() == "img")
        val goldImgUrl = goldImageElement.attr("src")
        require(goldImgUrl.isNotEmpty()) { "empty gold image url" }
        val img = ImageIO.read(URL(RiseOfLordsClient.BASE_URL + "/" + goldImgUrl))
        return GoldImageOCR.readAmount(img)
    }

    fun parseExcaliburState(response: String) = when {
        "Tentez votre chance" in response -> ExcaliburState.AVAILABLE
        "Vous avez déjà tenté votre chance aujourd'hui" in response -> ExcaliburState.ALREADY_TRIED_TODAY
        else -> error("Unknown excalibur status")
    }
}
