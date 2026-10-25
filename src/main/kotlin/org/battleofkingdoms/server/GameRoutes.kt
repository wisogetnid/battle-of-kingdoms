package org.battleofkingdoms.server

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.battleofkingdoms.game.AlreadyActed
import org.battleofkingdoms.game.DuplicatePlayer
import org.battleofkingdoms.game.GameException
import org.battleofkingdoms.game.RuleViolation
import org.battleofkingdoms.game.UnknownGame
import org.battleofkingdoms.game.UnknownPlayer
import org.battleofkingdoms.game.WrongPhase
import java.util.UUID
import kotlin.random.Random

/** HTTP mapping only: parse the request, call one [GameServer] method, map the result or error. */
fun Route.gameRoutes(server: GameServer) {
    route("/games") {
        post {
            respondOrError {
                val request = call.receive<CreateGameRequest>()
                val gameId = server.createGame(request.players, request.host, request.seed ?: Random.nextLong())
                call.respond(HttpStatusCode.Created, CreateGameResponse(gameId))
            }
        }

        route("/{id}") {
            get {
                respondOrError {
                    val gameId = gameId()
                    val player = call.request.queryParameters["player"]
                        ?: throw BadRequestException("Query parameter 'player' is required")
                    call.respond(PlayerView.of(gameId, server.getGame(gameId), player))
                }
            }

            post("/players") {
                respondOrError {
                    val gameId = gameId()
                    server.joinGame(gameId, call.receive<JoinRequest>().name)
                    call.respond(HttpStatusCode.Created)
                }
            }
        }
    }
}

private fun RoutingContext.gameId(): UUID {
    val raw = call.parameters["id"].orEmpty()
    return runCatching { UUID.fromString(raw) }.getOrElse { throw UnknownGame(raw) }
}

private suspend fun RoutingContext.respondOrError(handle: suspend RoutingContext.() -> Unit) {
    try {
        handle()
    } catch (e: GameException) {
        call.respond(status(e), ErrorResponse(e.message.orEmpty()))
    } catch (e: BadRequestException) {
        call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message.orEmpty()))
    }
}

private fun status(e: GameException): HttpStatusCode = when (e) {
    is RuleViolation -> HttpStatusCode.BadRequest
    is WrongPhase, is AlreadyActed, is DuplicatePlayer -> HttpStatusCode.Conflict
    is UnknownGame, is UnknownPlayer -> HttpStatusCode.NotFound
}
