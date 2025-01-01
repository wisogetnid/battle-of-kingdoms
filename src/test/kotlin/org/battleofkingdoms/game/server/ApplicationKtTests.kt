package org.battleofkingdoms.game.server

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import kotlin.test.*

private const val GAME = "/game"
private const val GAME_ID = "/1234"
private const val PLAYER = "/player"

class ApplicationKtTests {
    @Test
    fun testGameGET_shouldReturnStartingOptions() = testApplication {
        application {
            module()
        }
        val response = client.get(GAME)
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("TODO: starting options", response.bodyAsText())
    }

    @Test
    fun testGamePOST_shouldStartNewGame() = testApplication {
        application {
            module()
        }
        val response = client.post(GAME)
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("TODO: start new game", response.bodyAsText())
    }

    @Test
    fun testGameIdGET_shouldReturnGameState() = testApplication {
        application {
            module()
        }
        val response = client.get(GAME + GAME_ID)
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("TODO: return state of game: 1234", response.bodyAsText())
    }

    @Test
    fun testGameIdPOST_shouldExecuteAction() = testApplication {
        application {
            module()
        }
        val response = client.post(GAME + GAME_ID)
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("TODO: execute action in game: 1234", response.bodyAsText())
    }

    @Test
    fun testGameIdPlayerPOST_shouldAddNewPlayerToGame() = testApplication {
        application {
            module()
        }
        val response = client.post(GAME + GAME_ID + PLAYER)
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("TODO: add new player to game: 1234", response.bodyAsText())
    }
}