package org.battleofkingdoms.game

data class GameConfig(
    val minPlayers: Int = 3,
    val maxPlayers: Int = 5,
    val startingHorde: Int = 3,
    val baseCreaturesPerTurn: Int = 1,
    val resourcesPerTurn: Int = 2,
    val renownPerBattle: Int = 1,
    val artifactsToWin: Int = 3,
)
