package org.battleofkingdoms.server

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ApplicationKtTests {

    @Test
    fun `creating a game returns its id`() = apiTest { api ->
        val gameId = api.createGame(players = 4, host = "anna")

        assertNotNull(UUID.fromString(gameId))
    }

    @Test
    fun `creating a game without a seed works`() = apiTest { api ->
        api.createGame(players = 3, host = "anna", seed = null)
    }

    @Test
    fun `creating a game with an invalid player count is a bad request`() = apiTest { api ->
        assertError(HttpStatusCode.BadRequest, api.post("/games", mapOf("players" to 6, "host" to "anna")))
        assertError(HttpStatusCode.BadRequest, api.post("/games", mapOf("players" to 2, "host" to "anna")))
    }

    @Test
    fun `creating a game with a blank host is a bad request`() = apiTest { api ->
        assertError(HttpStatusCode.BadRequest, api.post("/games", mapOf("players" to 3, "host" to " ")))
    }

    @Test
    fun `a malformed or incomplete body is a bad request`() = apiTest { api ->
        assertError(HttpStatusCode.BadRequest, api.post("/games", mapOf("host" to "anna")))
        assertError(HttpStatusCode.BadRequest, client.post("/games") {
            contentType(ContentType.Application.Json)
            setBody("{not json")
        })
    }

    @Test
    fun `joining a game returns created`() = apiTest { api ->
        val gameId = api.createGame(players = 3, host = "anna")

        api.join(gameId, "ben")

        val names = api.view(gameId, "ben")["others"].map { it["name"].asText() }
        assertEquals(listOf("anna"), names)
    }

    @Test
    fun `joining with a taken name or a full game is a conflict`() = apiTest { api ->
        val gameId = api.createGame(players = 3, host = "anna")
        api.join(gameId, "ben")

        assertError(HttpStatusCode.Conflict, api.post("/games/$gameId/players", mapOf("name" to "ben")))
        api.join(gameId, "cleo")
        assertError(HttpStatusCode.Conflict, api.post("/games/$gameId/players", mapOf("name" to "dora")))
    }

    @Test
    fun `joining with a blank name is a bad request`() = apiTest { api ->
        val gameId = api.createGame(players = 3, host = "anna")

        assertError(HttpStatusCode.BadRequest, api.post("/games/$gameId/players", mapOf("name" to "")))
    }

    @Test
    fun `an unknown or malformed game id is not found`() = apiTest { api ->
        assertError(HttpStatusCode.NotFound, api.post("/games/${UUID.randomUUID()}/players", mapOf("name" to "ben")))
        assertError(HttpStatusCode.NotFound, api.post("/games/not-a-uuid/players", mapOf("name" to "ben")))
        assertError(HttpStatusCode.NotFound, api.viewResponse(UUID.randomUUID().toString(), "anna"))
    }

    @Test
    fun `viewing as an unknown player is not found`() = apiTest { api ->
        val gameId = api.createGame(players = 3, host = "anna")

        assertError(HttpStatusCode.NotFound, api.viewResponse(gameId, "zed"))
    }

    @Test
    fun `viewing without a player is a bad request`() = apiTest { api ->
        val gameId = api.createGame(players = 3, host = "anna")

        assertError(HttpStatusCode.BadRequest, client.get("/games/$gameId"))
    }

    @Test
    fun `a waiting game shows the lobby and no actions`() = apiTest { api ->
        val gameId = api.createGame(players = 3, host = "anna")

        val view = api.view(gameId, "anna")

        assertEquals(gameId, view["gameId"].asText())
        assertEquals("WAITING_FOR_PLAYERS", view["phase"].asText())
        assertEquals(0, view["turn"].asInt())
        assertEquals(3, view["victoryPointsToWin"].asInt())
        assertTrue(view["availableActions"].isEmpty)
    }

    @Test
    fun `a started game shows my hand, the pot, and build-up actions`() = apiTest { api ->
        val gameId = api.startGame("anna", "ben", "cleo")

        val view = api.view(gameId, "anna")

        assertEquals("BUILD_UP", view["phase"].asText())
        assertEquals(1, view["turn"].asInt())
        assertEquals(1, view["pot"].asInt())
        assertEquals("anna", view["me"]["name"].asText())
        assertEquals(4, view["me"]["hand"]["HORDE"].asInt())
        val resources = listOf("WOOD", "IRON").sumOf { view["me"]["hand"].path(it).asInt(0) }
        assertEquals(2, resources)
        assertEquals(listOf("UPGRADE", "READY"), view["availableActions"].map { it.asText() })
        assertTrue(view["battleLog"].isEmpty)
        assertTrue(view["winner"].isNull)
    }

    @Test
    fun `a view never contains another player's hand`() = apiTest { api ->
        val gameId = api.startGame("anna", "ben", "cleo")

        val view = api.view(gameId, "anna")

        val others = view["others"]
        assertEquals(listOf("ben", "cleo"), others.map { it["name"].asText() })
        others.forEach { other ->
            assertEquals(setOf("name", "vp", "renown", "handSize", "acted"), other.fieldNames().asSequence().toSet())
            assertEquals(6, other["handSize"].asInt())
        }
        assertFalse(view.toString().contains("commits"))
    }
}
