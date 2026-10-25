package org.battleofkingdoms.server

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.HttpStatusCode
import org.battleofkingdoms.cards.Artifact
import org.battleofkingdoms.game.GameConfig
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val PLAYERS = arrayOf("anna", "ben", "cleo")
private val ALL_ARTIFACTS = GameConfig(artifacts = Artifact.entries)

class ClaimRoutesTests {

    @Test
    fun `only the winner sees the drawn artifact and can claim`() = apiTest(ALL_ARTIFACTS) { api ->
        val gameId = annaWins(api)

        val anna = api.view(gameId, "anna")
        val drawn = anna["me"]["drawnArtifact"].asText()

        assertEquals("CLAIM", anna["phase"].asText())
        assertTrue(drawn in Artifact.entries.map { it.name })
        assertEquals(listOf("CLAIM"), anna["availableActions"].map { it.asText() })
        assertEquals(4, anna["artifactsInStack"].asInt())
        assertTrue(anna["battleLog"].single()["prize"].isNull)
        listOf("ben", "cleo").forEach { name ->
            val view = api.view(gameId, name)
            assertTrue(view["me"]["drawnArtifact"].isNull)
            assertTrue(view["availableActions"].isEmpty)
            assertFalse(view.toString().contains(drawn), "$name must not see the drawn $drawn")
        }
    }

    @Test
    fun `taking the VP never reveals the declined artifact`() = apiTest(ALL_ARTIFACTS) { api ->
        val gameId = annaWins(api)
        val declined = api.view(gameId, "anna")["me"]["drawnArtifact"].asText()

        val mine = claim(api, gameId, "anna", "VICTORY_POINT")

        assertEquals(1, mine["me"]["vp"].asInt())
        assertEquals("BUILD_UP", mine["phase"].asText())
        assertEquals(4, mine["artifactsInStack"].asInt())
        PLAYERS.forEach { name ->
            val view = api.view(gameId, name)
            assertEquals("VICTORY_POINT", view["battleLog"].single()["prize"].asText())
            assertFalse(view.toString().contains(declined), "$name's view names the declined $declined")
        }
    }

    @Test
    fun `a taken artifact is public`() = apiTest(ALL_ARTIFACTS) { api ->
        val gameId = annaWins(api)
        val drawn = api.view(gameId, "anna")["me"]["drawnArtifact"].asText()

        val mine = claim(api, gameId, "anna", "ARTIFACT")

        assertEquals(listOf(drawn), mine["me"]["artifacts"].map { it.asText() })
        assertEquals(0, mine["me"]["vp"].asInt())
        assertTrue(mine["me"]["drawnArtifact"].isNull)
        listOf("ben", "cleo").forEach { name ->
            val view = api.view(gameId, name)
            assertEquals(3, view["artifactsInStack"].asInt())
            assertEquals(drawn, view["battleLog"].single()["prize"].asText())
            assertEquals(listOf(drawn), others(view, "anna")["artifacts"].map { it.asText() })
        }
    }

    @Test
    fun `claiming as anyone but the winner, or outside CLAIM, is a conflict`() = apiTest(ALL_ARTIFACTS) { api ->
        val buildUp = api.startGame(*PLAYERS)
        assertError(HttpStatusCode.Conflict, api.post("/games/$buildUp/players/anna/claim", mapOf("take" to "VICTORY_POINT")))

        val gameId = annaWins(api)
        assertError(HttpStatusCode.Conflict, api.post("/games/$gameId/players/ben/claim", mapOf("take" to "VICTORY_POINT")))
        assertError(HttpStatusCode.NotFound, api.post("/games/$gameId/players/zed/claim", mapOf("take" to "VICTORY_POINT")))
    }

    @Test
    fun `a claim must take ARTIFACT or VICTORY_POINT`() = apiTest(ALL_ARTIFACTS) { api ->
        val gameId = annaWins(api)

        assertError(HttpStatusCode.BadRequest, api.post("/games/$gameId/players/anna/claim", mapOf("take" to "FAMOUS_CROWN")))
        assertError(HttpStatusCode.BadRequest, api.post("/games/$gameId/players/anna/claim", emptyMap<String, String>()))
        assertEquals("CLAIM", api.view(gameId, "anna")["phase"].asText())
    }

    @Test
    fun `with an empty stack there is nothing to claim`() = apiTest(GameConfig(artifacts = emptyList())) { api ->
        val gameId = annaWins(api)

        val view = api.view(gameId, "anna")
        assertEquals("BUILD_UP", view["phase"].asText())
        assertEquals(1, view["me"]["vp"].asInt())
        assertEquals(0, view["artifactsInStack"].asInt())
        assertEquals("VICTORY_POINT", view["battleLog"].single()["prize"].asText())
    }

    @Test
    fun `a game created without artifacts has an empty stack`() = apiTest(ALL_ARTIFACTS) { api ->
        val created = api.post("/games", mapOf("players" to 3, "host" to "anna", "artifacts" to false))
        assertEquals(HttpStatusCode.Created, created.status)

        assertEquals(0, api.view(TestApi.json(created)["gameId"].asText(), "anna")["artifactsInStack"].asInt())
    }

    @Test
    fun `the Transmuter Stone's owner transmutes over HTTP, once per turn`() = apiTest(GameConfig(artifacts = listOf(Artifact.TRANSMUTER_STONE))) { api ->
        val gameId = annaWins(api)
        val mine = claim(api, gameId, "anna", "ARTIFACT")
        assertTrue("TRANSMUTE" in mine["availableActions"].map { it.asText() })
        val resource = listOf("WOOD", "IRON", "ESSENCE").first { mine["me"]["hand"].path(it).asInt(0) > 0 }

        val response = api.post("/games/$gameId/players/anna/transmute", mapOf("resource" to resource))

        assertEquals(HttpStatusCode.OK, response.status, TestApi.json(response).toString())
        assertEquals(mine["me"]["hand"][resource].asInt() - 1, TestApi.json(response)["me"]["hand"].path(resource).asInt(0))
        assertError(HttpStatusCode.BadRequest, api.post("/games/$gameId/players/anna/transmute", mapOf("resource" to resource)))
        assertError(HttpStatusCode.BadRequest, api.post("/games/$gameId/players/ben/transmute", mapOf("resource" to "WOOD")))
    }

    /** Starts a game; everyone readies, anna commits 1 Horde and the others nothing, so she wins uncontested. */
    private suspend fun annaWins(api: TestApi): String {
        val gameId = api.startGame(*PLAYERS)
        PLAYERS.forEach { api.post("/games/$gameId/players/$it/ready") }
        PLAYERS.forEach { name ->
            val army = if (name == "anna") mapOf("HORDE" to 1) else emptyMap()
            assertEquals(HttpStatusCode.OK, api.post("/games/$gameId/players/$name/army", army).status)
        }
        return gameId
    }

    private suspend fun claim(api: TestApi, gameId: String, name: String, take: String): JsonNode {
        val response = api.post("/games/$gameId/players/$name/claim", mapOf("take" to take))
        assertEquals(HttpStatusCode.OK, response.status, TestApi.json(response).toString())
        return TestApi.json(response)
    }

    private fun others(view: JsonNode, name: String): JsonNode = view["others"].single { it["name"].asText() == name }
}
