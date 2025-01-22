package org.battleofkingdoms.game

import org.battleofkingdoms.cards.Card
import org.battleofkingdoms.cards.creatures.Horde
import org.battleofkingdoms.cards.resources.Iron
import org.battleofkingdoms.cards.resources.Wood
import org.battleofkingdoms.player.Player
import java.util.*

open class Game(val numberOfPlayers: Int, val id: UUID = UUID.randomUUID()) {
    open fun state(): State = State.NOT_INITIALIZED
    open fun players(): Set<Player> = emptySet()
    var resourceDeck: List<Card> = wood(20) + iron(20) + horde(20).shuffled()

    constructor(existingGame: Game) : this(existingGame.numberOfPlayers, existingGame.id)

    private fun wood(count: Int): List<Card> = (1..count).map { Wood() }
    private fun iron(count: Int): List<Card> = (1..count).map { Iron() }
    private fun horde(count: Int): List<Card> = (1..count).map { Horde() }

    enum class State { WAIT_FOR_PLAYERS_TO_JOIN, IN_PLAY, NOT_INITIALIZED, BATTLE }
}