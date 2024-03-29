package org.hildan.bots.riseoflords.model

data class AccountState(
    var gold: Long = 0,
    var chestGold: Long = 0,
    var mana: Int = 0,
    var adventurins: Int = 0,
    var turns: Int = 0,
)

data class Player(
    val rank: Int,
    val name: String,
    val gold: Long,
    val army: Army,
    val alignment: Alignment,
)

data class Castle(
    val nMessages: Int,
    val nExpectedAdventureStones: Int,
    val nPrisoners: Int,
)

sealed class AttackResult {
    data class Victory(val goldStolen: Long) : AttackResult()
    object StormActive : AttackResult()
    object Defeat : AttackResult()
}

enum class Alignment(private val shortName: String) {
    SAINT("Sai."),
    CHEVALERESQUE("Che."),
    ALTRUISTE("Alt."),
    JUSTE("Jus."),
    NEUTRE("Neu."),
    SANS_SCRUPULES("SsS."),
    VIL("Vil."),
    ABOMINABLE("Abo."),
    DEMONIAQUE("D\u00e9m.");

    companion object {
        private val ALIGNMENTS = values().associateBy { it.shortName }

        operator fun get(shortName: String): Alignment =
            ALIGNMENTS[shortName] ?: throw IllegalArgumentException("No alignment corresponds to the short name '$shortName'")
    }
}

enum class Army(
    private val shortNameMan: String,
    private val shortNameWoman: String
) {
    WARRIORS("Chev.", "Guer."),
    MAGES("Sorc.", "Sorc."),
    SUICIDERS("Suic.", "Suic."),
    HEALERS("Sage", "Pr\u00e9t.");

    companion object {
        private val ARMIES = values().associateBy { it.shortNameMan } + values().associateBy { it.shortNameWoman }

        operator fun get(shortName: String): Army =
            ARMIES[shortName] ?: throw IllegalArgumentException("No army corresponds to the short name '$shortName'")
    }
}

enum class ExcaliburState {
    AVAILABLE,
    ALREADY_TRIED_TODAY,
}
