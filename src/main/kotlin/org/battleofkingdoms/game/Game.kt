package org.battleofkingdoms.game

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.battle.Battle
import org.battleofkingdoms.cards.Card
import org.battleofkingdoms.player.Player
import org.battleofkingdoms.player.Player.State.ACTIVE
import org.battleofkingdoms.player.Player.State.WAITING

const val CARD_DRAW_ON_NEW_TURN = 4

class Game(var gameState: GameState = GameState.withTestResources()) {
    fun state(): GameState.State = gameState.state
    fun players(): Map<String, Player> = gameState.playernameToPlayer

    // GameEngine
    fun newTurn(): Game {
        gameState.playernameToPlayer.keys.forEach {
            performBuildUp(it)
        }

        gameState.playernameToPlayer.values.forEach { player ->
            player.state = ACTIVE
        }
        gameState.state = GameState.State.IN_PLAY
        return this
    }

    // GameEngine
    fun performBuildUp(playerName: String) {
        println("$playerName started build up phase")
        gameState = dealCards(gameState, playerName, CARD_DRAW_ON_NEW_TURN)
    }

    // Game
    private fun dealCards(gameState: GameState, playerName: String, cardsToDraw: Int): GameState {
        val newDeck = gameState.resourceDeck.toMutableList()
        val newCards = newDeck.take(cardsToDraw)
        // need to remove cards one by one
        newCards.forEach{ newDeck.remove(it)}

        val player = gameState.playernameToPlayer[playerName]!!
        val updatedPlayers = gameState.playernameToPlayer + mapOf(
            playerName to player.copy(hand = (player.hand + newCards).toMutableList())
        )

        return GameState(resourceDeck = newDeck, playernameToPlayer = updatedPlayers, id = gameState.id)
    }

    fun setPlayerStateTo(playerName: String, newState: Player.State): Map<String, Player> {
        return gameState.playernameToPlayer + mapOf(
            playerName to gameState.playernameToPlayer[playerName]!!.copy(state = newState)
        )
    }

    fun updatePlayer(playerName: String, player: Player): Map<String, Player> {
        return gameState.playernameToPlayer + mapOf(
            playerName to player
        )
    }

    fun finishBuildUp(playerName: String): Game {
        gameState.playernameToPlayer = setPlayerStateTo(playerName, WAITING)

        return when (gameState.playernameToPlayer.all { WAITING == it.value.state }) {
            true -> this.newBattle()
            else -> this
        }
    }

    fun newBattle(): Game {
        gameState.playernameToPlayer = gameState.playernameToPlayer.values
            .map { it.name to it.copy(state = ACTIVE) }
            .fold(emptyMap()) { acc, entry -> acc + entry }
        gameState.state = GameState.State.BATTLE
        return this
    }

    fun commitArmy(playerName: String, army: Army): Game {
        gameState.playernameToPlayer = updatePlayer(
            playerName,
            gameState.playernameToPlayer[playerName]!!
                .copy(
                    state = WAITING,
                    hand = removeCardsOneByOne(playerName, army),
                    committedArmy = army
                )
        )

        return when (gameState.playernameToPlayer.all { WAITING == it.value.state }) {
            true -> {
                val firstPlayer = gameState.playernameToPlayer.values.first()
                val secondPlayer = gameState.playernameToPlayer.values.last()

                val battleResult = Battle(
                    firstPlayer.committedArmy,
                    secondPlayer.committedArmy
                ).resolve()

                gameState.playernameToPlayer = updatePlayer(
                    firstPlayer.name, firstPlayer
                        .copy(
                            hand = (firstPlayer.hand + battleResult.armyRemaining.creatures).toMutableList()
                        )
                )
                gameState.playernameToPlayer = updatePlayer(
                    secondPlayer.name, secondPlayer
                        .copy(
                            hand = (secondPlayer.hand + battleResult.opposingArmyRemaining.creatures).toMutableList()
                        )
                )

                this.newTurn()
            }

            else -> this
        }
    }

    private fun removeCardsOneByOne(
        playerName: String,
        army: Army
    ): MutableList<Card> {
        val cards = gameState.playernameToPlayer[playerName]!!.hand.toMutableList()
        army.creatures.forEach {
            cards.remove(it)
        }
        return cards
    }
}