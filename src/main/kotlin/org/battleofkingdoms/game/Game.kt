package org.battleofkingdoms.game

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.battle.Battle
import org.battleofkingdoms.cards.Card
import org.battleofkingdoms.player.Player
import org.battleofkingdoms.player.Player.State.ACTIVE
import org.battleofkingdoms.player.Player.State.WAITING

const val CARD_DRAW_ON_NEW_TURN = 4

class Game(var board: Board = Board.withTestResources()) {
    fun state(): Board.State = board.state
    fun players(): Map<String, Player> = board.playernameToPlayer

    fun setPlayerStateTo(playerName: String, newState: Player.State): Map<String, Player> {
        return board.playernameToPlayer + mapOf(
            playerName to board.playernameToPlayer[playerName]!!.copy(state = newState)
        )
    }

    fun updatePlayer(playerName: String, player: Player): Map<String, Player> {
        return board.playernameToPlayer + mapOf(
            playerName to player
        )
    }

    fun newTurn(): Game {
        board.playernameToPlayer.values.forEach{ player ->
            for (i in 1..CARD_DRAW_ON_NEW_TURN) {
                val card = board.resourceDeck.removeFirst()
                player.hand.add(card)
            }
            player.state = ACTIVE
        }
        board.state = Board.State.IN_PLAY
        return this
    }

    fun finishBuildUp(playerName: String): Game {
        board.playernameToPlayer = setPlayerStateTo(playerName, WAITING)

        return when (board.playernameToPlayer.all { WAITING == it.value.state }) {
            true -> this.newBattle()
            else -> this
        }
    }

    fun newBattle(): Game {
        board.playernameToPlayer = board.playernameToPlayer.values
            .map { it.name to it.copy(state = ACTIVE) }
            .fold(emptyMap()) { acc, entry -> acc + entry }
        board.state = Board.State.BATTLE
        return this
    }

    fun commitArmy(playerName: String, army: Army): Game {
        board.playernameToPlayer = updatePlayer(
            playerName,
            board.playernameToPlayer[playerName]!!
                .copy(
                    state = WAITING,
                    hand = removeCardsOneByOne(playerName, army),
                    committedArmy = army
                )
        )

        return when (board.playernameToPlayer.all { WAITING == it.value.state }) {
            true -> {
                val firstPlayer = board.playernameToPlayer.values.first()
                val secondPlayer = board.playernameToPlayer.values.last()

                val battleResult = Battle(
                    firstPlayer.committedArmy,
                    secondPlayer.committedArmy
                ).resolve()

                board.playernameToPlayer = updatePlayer(firstPlayer.name, firstPlayer
                    .copy(
                        hand = (firstPlayer.hand + battleResult.armyRemaining.creatures).toMutableList()))
                board.playernameToPlayer = updatePlayer(secondPlayer.name, secondPlayer
                    .copy(
                        hand = (secondPlayer.hand + battleResult.opposingArmyRemaining.creatures).toMutableList()))

                this.newTurn()
            }
            else -> this
        }
    }

    private fun removeCardsOneByOne(
        playerName: String,
        army: Army
    ): MutableList<Card> {
        val cards = board.playernameToPlayer[playerName]!!.hand.toMutableList()
        army.creatures.forEach {
            cards.remove(it)
        }
        return cards
    }
}