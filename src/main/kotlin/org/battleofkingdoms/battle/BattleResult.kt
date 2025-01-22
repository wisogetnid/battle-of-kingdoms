package org.battleofkingdoms.battle

data class BattleResult(val armyRemaining: Army, val opposingArmyRemaining: Army, val outcome: BattleOutcome) {
    enum class BattleOutcome {
        WIN, LOSS, DRAW
    }
}
