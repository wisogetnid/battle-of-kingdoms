package org.battleofkingdoms.game.server

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.http.*

// Simple game state
data class GameState(
    var currentPlayer: Int = 1,
    var board: List<String> = List(9) { "" },  // Example for a 3x3 board
    var gameOver: Boolean = false
)

data class Move(
    val player: Int,
    val position: Int  // Or whatever move data you need
)

class GameServer {
    // Store game state in memory (you might want to use a proper database later)
    private val gameState = GameState()

    fun start() {
        embeddedServer(Netty, port = 8080) {
            routing {
                // Get current game state
                get("/game") {
                    call.respond(gameState.toString())
                }

                // Make a move
                post("/move") {
                    val move = call.receive<Move>()
                    if (move.player == gameState.currentPlayer) {
                        // Update game state based on move
                        // (Add your game logic here)

                        // Switch to other player
                        gameState.currentPlayer = if (gameState.currentPlayer == 1) 2 else 1
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Not your turn!")
                    }
                }

                // Check whose turn it is
                get("/turn") {
                    call.respond(mapOf("currentPlayer" to gameState.currentPlayer))
                }
            }
        }.start(wait = true)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            GameServer().start()
        }
    }
}