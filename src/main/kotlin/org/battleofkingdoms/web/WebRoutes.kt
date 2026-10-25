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
import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.cards.ResourceType
import org.battleofkingdoms.game.GameException
import org.battleofkingdoms.game.RuleViolation
import org.battleofkingdoms.game.Take
import org.battleofkingdoms.game.UnknownGame
import org.battleofkingdoms.game.UnknownPlayer
import org.battleofkingdoms.server.GameServer
import org.battleofkingdoms.server.PlayerView
import org.battleofkingdoms.server.armyOf
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
                val gameId = server.createGame(form.int("players"), name, form.seed(), artifacts = form["artifacts"] != null)
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

            route("/players/{name}") {
                get {
                    orErrorPage({ notFoundPage(it) }) {
                        val view = PlayerView.of(gameId(), server.getGame(gameId()), playerName())
                        call.respondHtml { gamePage(view) }
                    }
                }

                post("/upgrades") {
                    playerAction(server) { server.upgrade(gameId(), playerName(), it.enum<CreatureType>("into")) }
                }

                post("/trades") {
                    playerAction(server) { server.trade(gameId(), playerName(), it.enum<ResourceType>("resource")) }
                }

                post("/ready") {
                    playerAction(server) { server.ready(gameId(), playerName()) }
                }

                post("/army") {
                    playerAction(server) { server.commitArmy(gameId(), playerName(), it.army()) }
                }

                post("/claim") {
                    playerAction(server) { server.claim(gameId(), playerName(), it.enum<Take>("take")) }
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

/**
 * One game action from my game page: on success back to the page with `303`; a rejected action
 * shows the page again with the engine's message.
 */
private suspend fun RoutingContext.playerAction(server: GameServer, action: RoutingContext.(Parameters) -> Unit) {
    val form = call.receiveParameters()
    orErrorPage({ gamePage(PlayerView.of(gameId(), server.getGame(gameId()), playerName()), error = it) }) {
        server.getGame(gameId()).player(playerName()) // unknown game or player → not found, before parsing the form
        action(form)
        seeOther(playerPath(rawGameId(), playerName()))
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

private inline fun <reified T : Enum<T>> Parameters.enum(field: String): T =
    enumValues<T>().find { it.name == this[field] } ?: throw RuleViolation("Unknown $field '${this[field].orEmpty()}'")

/** One count field per creature, e.g. `HORDE=2&KNIGHT=1`; a blank or missing field is 0. */
private fun Parameters.army() = armyOf(
    CreatureType.entries.associateWith { creature ->
        val count = this[creature.name]?.trim().orEmpty()
        if (count.isEmpty()) 0 else count.toIntOrNull() ?: throw RuleViolation("'${creature.name}' must be a whole number")
    },
)

private fun Parameters.seed(): Long = when {
    this["seed"].isNullOrBlank() -> Random.nextLong()
    else -> this["seed"]!!.trim().toLongOrNull() ?: throw RuleViolation("'seed' must be a whole number")
}
