package org.battleofkingdoms.server

import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import org.battleofkingdoms.web.webRoutes

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() = gameModule(GameServer())

/** The whole application around one [GameServer], so tests can pass one with their own config. */
fun Application.gameModule(server: GameServer) {
    install(ContentNegotiation) {
        jackson()
    }
    routing {
        gameRoutes(server)
        webRoutes(server)
    }
}
