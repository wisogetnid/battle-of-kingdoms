package org.battleofkingdoms.battle

/** One player's part in a battle; [army] is empty if they committed nothing. */
data class ArmyResult(
    val army: Army,
    val score: Int,
    val casualties: Army,
    val survivors: Army,
    val vpGained: Int,
    val gainedRenown: Boolean,
)

/** One 1-vs-1 duel within a battle, with each side's strength against the other. */
data class Duel(
    val first: String,
    val second: String,
    val firstStrength: Int,
    val secondStrength: Int,
) {
    /** [player]'s strength minus their opponent's; 0 if they aren't in this duel. */
    fun margin(player: String): Int = when (player) {
        first -> firstStrength - secondStrength
        second -> secondStrength - firstStrength
        else -> 0
    }
}

/** The outcome of one battle, which is also its battle log entry. */
data class BattleResult(
    val pot: Int,
    val winner: String?,
    val armies: Map<String, ArmyResult>,
    val duels: List<Duel>,
) {
    val rolledOver: Boolean get() = winner == null
}
