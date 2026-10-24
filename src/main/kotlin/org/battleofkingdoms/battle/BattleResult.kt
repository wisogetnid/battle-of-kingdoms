package org.battleofkingdoms.battle

/** One player's part in a battle; [army] is empty if they committed nothing. */
data class ArmyResult(
    val army: Army,
    val strength: Int,
    val casualties: Army,
    val survivors: Army,
    val vpGained: Int,
    val gainedRenown: Boolean,
)

/** The outcome of one battle, which is also its battle log entry. */
data class BattleResult(
    val pot: Int,
    val winner: String?,
    val armies: Map<String, ArmyResult>,
) {
    val rolledOver: Boolean get() = winner == null
}
