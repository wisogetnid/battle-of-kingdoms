package org.battleofkingdoms.web

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.html.respondHtml
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.html.HTML
import org.battleofkingdoms.game.GameException
import org.battleofkingdoms.game.RuleViolation
import org.battleofkingdoms.game.UnknownGame
import org.battleofkingdoms.game.UnknownPlayer
import org.battleofkingdoms.server.GameServer
import org.battleofkingdoms.server.PlayerView
import org.battleofkingdoms.server.status
import java.util.UUID
import kotlin.random.Random

/** Browser UI: parse the form, call one [GameServer] method, then redirect or render a page. */
fun Route.webRoutes(server: GameServer) {
    get("/") {
        call.respondHtml { homePage(gameId = call.request.queryParameters["game"]) }
    }

    route("/ui/games") {
        post {
            val form = call.receiveParameters()
            orErrorPage({ homePage(error = it) }) {
                val name = form["name"].orEmpty()
                val gameId = server.createGame(form.int("players"), name, form.seed())
                seeOther(playerPath(gameId.toString(), name))
            }
        }

        route("/{id}") {
            post("/players") {
                val form = call.receiveParameters()
                orErrorPage({ homePage(gameId = rawGameId(), error = it) }) {
                    val name = form["name"].orEmpty()
                    server.joinGame(gameId(), name)
                    seeOther(playerPath(rawGameId(), name))
                }
            }

            get("/players/{name}") {
                orErrorPage({ notFoundPage(it) }) {
                    val view = PlayerView.of(gameId(), server.getGame(gameId()), playerName())
                    call.respondHtml { gamePage(view) }
                }
            }
        }
    }
}

/**
 * Runs [handle]; a rejected request renders [errorPage] with the engine's message and the JSON
 * API's status, except an unknown game or player, which gets the not-found page.
 */
private suspend fun RoutingContext.orErrorPage(
    errorPage: HTML.(String) -> Unit,
    handle: suspend RoutingContext.() -> Unit,
) {
    try {
        handle()
    } catch (e: GameException) {
        val message = e.message.orEmpty()
        when (e) {
            is UnknownGame, is UnknownPlayer -> call.respondHtml(status(e)) { notFoundPage(message) }
            else -> call.respondHtml(status(e)) { errorPage(message) }
        }
    }
}

/** Post/Redirect/Get: answer a successful POST with `303`, so reloading never repeats it. */
private suspend fun RoutingContext.seeOther(path: String) {
    call.response.headers.append(HttpHeaders.Location, path)
    call.respond(HttpStatusCode.SeeOther)
}

private fun RoutingContext.rawGameId(): String = call.parameters["id"].orEmpty()

private fun RoutingContext.gameId(): UUID =
    runCatching { UUID.fromString(rawGameId()) }.getOrElse { throw UnknownGame(rawGameId()) }

private fun RoutingContext.playerName(): String = call.parameters["name"].orEmpty()

private fun Parameters.int(field: String): Int =
    this[field]?.trim()?.toIntOrNull() ?: throw RuleViolation("'$field' must be a whole number")

private fun Parameters.seed(): Long = when {
    this["seed"].isNullOrBlank() -> Random.nextLong()
    else -> this["seed"]!!.trim().toLongOrNull() ?: throw RuleViolation("'seed' must be a whole number")
}
