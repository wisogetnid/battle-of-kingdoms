package org.battleofkingdoms.server

import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private const val GAME = "/game"
private const val GAME_ID = "/{game-id}"
private const val PLAYER = "/player"

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    routing {
        get(GAME) {
            call.respond("TODO: starting options")
        }

        post(GAME) {
            call.respond("TODO: start new game")
        }

        get(GAME + GAME_ID) {
            val gameId = call.parameters["game-id"]
            call.respond("TODO: return state of game: " + gameId)
        }

        post(GAME + GAME_ID) {
            val gameId = call.parameters["game-id"]
            call.respond("TODO: execute action in game: " + gameId)
        }

        post(GAME + GAME_ID + PLAYER) {
            val gameId = call.parameters["game-id"]
            call.respond("TODO: add new player to game: " + gameId)
        }
    }
}