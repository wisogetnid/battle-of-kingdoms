package org.battleofkingdoms.game

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.battle.Battle
import org.battleofkingdoms.cards.Card
import org.battleofkingdoms.player.Player
import org.battleofkingdoms.player.Player.State.ACTIVE
import org.battleofkingdoms.player.Player.State.WAITING
import java.util.*

const val CARD_DRAW_ON_NEW_TURN = 4

open class Game(
    var playernameToPlayer: Map<String, Player> = mutableMapOf(),
    val id: UUID = UUID.randomUUID(),
    var state: State = State.NOT_INITIALIZED
) {
    open fun state(): State = state
    open fun players(): Map<String, Player> = playernameToPlayer
    var board: Board = Board.withTestResources()

    fun setPlayerStateTo(playerName: String, newState: Player.State): Map<String, Player> {
        return playernameToPlayer + mapOf(
            playerName to playernameToPlayer[playerName]!!.copy(state = newState)
        )
    }

    fun updatePlayer(playerName: String, player: Player): Map<String, Player> {
        return playernameToPlayer + mapOf(
            playerName to player
        )
    }

    fun newTurn(): Game {
        playernameToPlayer = playernameToPlayer.values
            .map {
                it.name to it.copy(
                    hand = it.hand + board.resourceDeck.take(CARD_DRAW_ON_NEW_TURN),
                    state = ACTIVE,
                    committedArmy = Army()
                )
            }.toMap()
        playernameToPlayer.forEach {
            board = Board(board.resourceDeck.drop(CARD_DRAW_ON_NEW_TURN))
        }
        state = State.IN_PLAY
        return this
    }

    fun finishBuildUp(playerName: String): Game {
        playernameToPlayer = setPlayerStateTo(playerName, WAITING)

        return when (playernameToPlayer.all { WAITING == it.value.state }) {
            true -> this.newBattle()
            else -> this
        }
    }

    fun newBattle(): Game {
        playernameToPlayer = playernameToPlayer.values
            .map { it.name to it.copy(state = ACTIVE) }
            .fold(emptyMap()) { acc, entry -> acc + entry }
        state = State.BATTLE
        return this
    }

    fun commitArmy(playerName: String, army: Army): Game {
        playernameToPlayer = updatePlayer(
            playerName,
            playernameToPlayer[playerName]!!
                .copy(
                    state = WAITING,
                    hand = removeCardsOneByOne(playerName, army),
                    committedArmy = army
                )
        )

        return when (playernameToPlayer.all { WAITING == it.value.state }) {
            true -> {
                val firstPlayer = playernameToPlayer.values.first()
                val secondPlayer = playernameToPlayer.values.last()

                val battleResult = Battle(
                    firstPlayer.committedArmy,
                    secondPlayer.committedArmy
                ).resolve()

                playernameToPlayer = updatePlayer(firstPlayer.name, firstPlayer
                    .copy(
                        hand = firstPlayer.hand + battleResult.armyRemaining.creatures))
                playernameToPlayer = updatePlayer(secondPlayer.name, secondPlayer
                    .copy(
                        hand = secondPlayer.hand + battleResult.opposingArmyRemaining.creatures))

                this.newTurn()
            }
            else -> this
        }
    }

    private fun removeCardsOneByOne(
        playerName: String,
        army: Army
    ): List<Card> {
        val cards = playernameToPlayer[playerName]!!.hand.toMutableList()
        army.creatures.forEach {
            cards.remove(it)
        }
        return cards
    }

    enum class State { IN_PLAY, NOT_INITIALIZED, BATTLE }
}