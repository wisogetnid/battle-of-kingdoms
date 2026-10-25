package org.battleofkingdoms.server

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val PLAYERS = arrayOf("anna", "ben", "cleo")

class ActionRoutesTests {

    @Test
    fun `an upgrade spends a Horde and the resource and returns my view`() = apiTest { api ->
        val gameId = api.startGame(*PLAYERS)
        val before = api.view(gameId, "anna")["me"]["hand"]
        val (into, resource) = affordableUpgrade(before)

        val response = api.upgrade(gameId, "anna", into)

        assertEquals(HttpStatusCode.OK, response.status)
        val hand = TestApi.json(response)["me"]["hand"]
        assertEquals(3, hand["HORDE"].asInt())
        assertEquals(1, hand[into].asInt())
        assertEquals(before[resource].asInt() - 1, hand.path(resource).asInt(0))
        assertEquals(1, TestApi.json(response)["me"]["upgradedThisTurn"][into].asInt())
    }

    @Test
    fun `a tier-2 upgrade of a unit created this turn is a bad request`() = apiTest { api ->
        val gameId = api.startGame(*PLAYERS)
        val (into, _) = affordableUpgrade(api.view(gameId, "anna")["me"]["hand"])
        api.upgrade(gameId, "anna", into)

        val tier2 = mapOf("BOW" to "WARBOW", "SWORD" to "KNIGHT", "MAGE" to "HIGH_MAGE").getValue(into)
        assertError(HttpStatusCode.BadRequest, api.upgrade(gameId, "anna", tier2))
    }

    @Test
    fun `an upgrade without the resource, into a Horde, or into an unknown creature is a bad request`() =
        apiTest { api ->
            val gameId = api.startGame(*PLAYERS)

            assertError(HttpStatusCode.BadRequest, api.upgrade(gameId, "anna", "HORDE"))
            assertError(HttpStatusCode.BadRequest, api.upgrade(gameId, "anna", "DRAGON"))
            assertError(HttpStatusCode.BadRequest, api.upgrade(gameId, "anna", "SHIELD"))
            var response = api.upgrade(gameId, "anna", "BOW")
            while (response.status == HttpStatusCode.OK) response = api.upgrade(gameId, "anna", "BOW")
            assertError(HttpStatusCode.BadRequest, response)
        }

    @Test
    fun `a trade swaps a resource for a Horde and returns my view`() = apiTest { api ->
        val gameId = api.startGame(*PLAYERS)
        val before = api.view(gameId, "anna")["me"]["hand"]
        val resource = listOf("WOOD", "IRON", "ESSENCE").first { before.has(it) }

        val response = api.trade(gameId, "anna", resource)

        assertEquals(HttpStatusCode.OK, response.status)
        val hand = TestApi.json(response)["me"]["hand"]
        assertEquals(before["HORDE"].asInt() + 1, hand["HORDE"].asInt())
        assertEquals(before[resource].asInt() - 1, hand.path(resource).asInt(0))
    }

    @Test
    fun `trading something I don't have, a creature, or an unknown name is a bad request`() = apiTest { api ->
        val gameId = api.startGame(*PLAYERS)
        val hand = api.view(gameId, "anna")["me"]["hand"]
        val missing = listOf("WOOD", "IRON", "ESSENCE").first { !hand.has(it) }

        assertError(HttpStatusCode.BadRequest, api.trade(gameId, "anna", missing))
        assertError(HttpStatusCode.BadRequest, api.trade(gameId, "anna", "HORDE"))
        assertError(HttpStatusCode.BadRequest, api.trade(gameId, "anna", "GOLD"))
    }

    @Test
    fun `trading after ready or outside build-up is a conflict`() = apiTest { api ->
        val gameId = api.startGame(*PLAYERS)
        api.post("/games/$gameId/players/anna/ready")
        assertError(HttpStatusCode.Conflict, api.trade(gameId, "anna", "WOOD"))

        allReady(api, gameId)
        assertError(HttpStatusCode.Conflict, api.trade(gameId, "ben", "WOOD"))
        assertError(HttpStatusCode.NotFound, api.trade(gameId, "zed", "WOOD"))
    }

    @Test
    fun `ready returns my view with nothing left to do`() = apiTest { api ->
        val gameId = api.startGame(*PLAYERS)

        val view = TestApi.json(api.post("/games/$gameId/players/anna/ready"))

        assertTrue(view["me"]["acted"].asBoolean())
        assertTrue(view["availableActions"].isEmpty)
        assertEquals("BUILD_UP", view["phase"].asText())
    }

    @Test
    fun `acting again after ready is a conflict`() = apiTest { api ->
        val gameId = api.startGame(*PLAYERS)
        api.post("/games/$gameId/players/anna/ready")

        assertError(HttpStatusCode.Conflict, api.post("/games/$gameId/players/anna/ready"))
        assertError(HttpStatusCode.Conflict, api.upgrade(gameId, "anna", "BOW"))
    }

    @Test
    fun `actions in the wrong phase are a conflict`() = apiTest { api ->
        val waiting = api.createGame(players = 3, host = "anna")
        assertError(HttpStatusCode.Conflict, api.post("/games/$waiting/players/anna/ready"))

        val gameId = api.startGame(*PLAYERS)
        assertError(HttpStatusCode.Conflict, api.commit(gameId, "anna", mapOf("HORDE" to 1)))
        allReady(api, gameId)
        assertError(HttpStatusCode.Conflict, api.upgrade(gameId, "anna", "BOW"))
    }

    @Test
    fun `actions by an unknown player or in an unknown game are not found`() = apiTest { api ->
        val gameId = api.startGame(*PLAYERS)

        assertError(HttpStatusCode.NotFound, api.post("/games/$gameId/players/zed/ready"))
        assertError(HttpStatusCode.NotFound, api.post("/games/${UUID.randomUUID()}/players/anna/ready"))
    }

    @Test
    fun `when everyone is ready the battle starts`() = apiTest { api ->
        val gameId = api.startGame(*PLAYERS)

        allReady(api, gameId)

        val view = api.view(gameId, "anna")
        assertEquals("BATTLE", view["phase"].asText())
        assertEquals(listOf("COMMIT_ARMY"), view["availableActions"].map { it.asText() })
    }

    @Test
    fun `committing more than I own, a negative count, or an unknown creature is a bad request`() = apiTest { api ->
        val gameId = api.startGame(*PLAYERS)
        allReady(api, gameId)

        assertError(HttpStatusCode.BadRequest, api.commit(gameId, "anna", mapOf("HORDE" to 5)))
        assertError(HttpStatusCode.BadRequest, api.commit(gameId, "anna", mapOf("BOW" to 1)))
        assertError(HttpStatusCode.BadRequest, api.commit(gameId, "anna", mapOf("HORDE" to -1)))
        assertError(HttpStatusCode.BadRequest, api.commit(gameId, "anna", mapOf("DRAGON" to 1)))
        assertError(HttpStatusCode.BadRequest, api.commit(gameId, "anna", mapOf("SHIELD" to 1)))
    }

    @Test
    fun `committing twice is a conflict`() = apiTest { api ->
        val gameId = api.startGame(*PLAYERS)
        allReady(api, gameId)
        api.commit(gameId, "anna", emptyMap())

        assertError(HttpStatusCode.Conflict, api.commit(gameId, "anna", emptyMap()))
    }

    @Test
    fun `a committed army is invisible to others until the battle resolves`() = apiTest { api ->
        val gameId = api.startGame(*PLAYERS)
        allReady(api, gameId)
        val handSizeBefore = annaHandSize(api.view(gameId, "ben"))

        val mine = TestApi.json(api.commit(gameId, "anna", mapOf("HORDE" to 3)))
        val bens = api.view(gameId, "ben")

        assertTrue(mine["me"]["acted"].asBoolean())
        assertTrue(bens["battleLog"].isEmpty)
        assertEquals(handSizeBefore, annaHandSize(bens))
        assertTrue(bens["others"].single { it["name"].asText() == "anna" }["acted"].asBoolean())
        assertTrue("\"army\"" !in bens.toString())
    }

    @Test
    fun `after the battle everyone sees all armies in the battle log`() = apiTest { api ->
        val gameId = api.startGame(*PLAYERS)
        allReady(api, gameId)

        api.commit(gameId, "anna", mapOf("HORDE" to 3))
        api.commit(gameId, "ben", mapOf("HORDE" to 1))
        api.commit(gameId, "cleo", emptyMap())

        val view = api.view(gameId, "ben")
        val entry = view["battleLog"].single()
        assertEquals("anna", entry["winner"].asText())
        assertEquals(1, entry["pot"].asInt())
        assertEquals(3, entry["armies"]["anna"]["army"]["HORDE"].asInt())
        assertEquals(5, entry["armies"]["anna"]["score"].asInt()) // +2 against ben, +3 against cleo
        val annaVsBen = entry["duels"].single { it["players"].map { p -> p.asText() } == listOf("anna", "ben") }
        assertEquals(3, annaVsBen["strengths"]["anna"].asInt())
        assertEquals(1, annaVsBen["strengths"]["ben"].asInt())
        assertEquals(3, entry["duels"].size())
        assertEquals(1, entry["armies"]["anna"]["casualties"]["HORDE"].asInt())
        assertTrue(entry["armies"]["cleo"]["army"].isEmpty)
        assertEquals("BUILD_UP", view["phase"].asText())
        assertEquals(2, view["turn"].asInt())
    }

    private suspend fun allReady(api: TestApi, gameId: String) =
        PLAYERS.forEach { api.post("/games/$gameId/players/$it/ready") }

    private fun annaHandSize(view: JsonNode): Int =
        view["others"].single { it["name"].asText() == "anna" }["handSize"].asInt()

    private fun affordableUpgrade(hand: JsonNode): Pair<String, String> = when {
        hand.has("WOOD") -> "BOW" to "WOOD"
        hand.has("IRON") -> "SWORD" to "IRON"
        else -> "MAGE" to "ESSENCE"
    }
}

private suspend fun TestApi.trade(gameId: String, name: String, resource: String): HttpResponse =
    post("/games/$gameId/players/$name/trades", mapOf("resource" to resource))

private suspend fun TestApi.upgrade(gameId: String, name: String, into: String): HttpResponse =
    post("/games/$gameId/players/$name/upgrades", mapOf("into" to into))

private suspend fun TestApi.commit(gameId: String, name: String, army: Map<String, Int>): HttpResponse =
    post("/games/$gameId/players/$name/army", army)
