package org.battleofkingdoms.web

import io.ktor.server.html.respondHtml
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.battleofkingdoms.server.GameServer

/** Browser UI: parse the form, call one [GameServer] method, then redirect or render a page. */
fun Route.webRoutes(server: GameServer) {
    get("/") {
        call.respondHtml { homePage(gameId = call.request.queryParameters["game"]) }
    }
}
