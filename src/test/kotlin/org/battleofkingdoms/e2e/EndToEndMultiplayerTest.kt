package org.battleofkingdoms.e2e

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import org.battleofkingdoms.server.TestApi
import org.battleofkingdoms.server.apiTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val SEED = 2024L
private const val MAX_TURNS = 20
private val NAMES = listOf("anna", "ben", "cleo", "dora", "emil")
private val CREATURES = listOf("HORDE", "SHIELD", "BOW")

/**
 * Full seeded games played over HTTP only. Turn 1 every player commits 1 Horde, which forces
 * a tie and a rollover. From turn 2 the host upgrades whatever she can and commits everything,
 * while everyone else commits 1 Horde, so the host wins the double pot and then one more.
 */
class EndToEndMultiplayerTest {

    @ParameterizedTest(name = "{0} players")
    @ValueSource(ints = [3, 5])
    fun `a seeded game with a rollover is played over HTTP to a winner`(playerCount: Int) = apiTest { api ->
        val players = NAMES.take(playerCount)

        val final = play(api, players)

        assertEquals("GAME_OVER", final["phase"].asText())
        assertEquals("anna", final["winner"].asText())
        assertEquals(3, final["me"]["vp"].asInt())
        val log = final["battleLog"]
        assertTrue(log[0]["winner"].isNull, "turn 1 should be a tie")
        assertEquals(2, log[1]["pot"].asInt(), "the tied artifact should roll over")
        assertEquals(players.toSet(), log[0]["armies"].fieldNames().asSequence().toSet())
        assertTrue(final["availableActions"].isEmpty)
    }

    @ParameterizedTest(name = "{0} players")
    @ValueSource(ints = [3, 5])
    fun `replaying the same seed and actions over HTTP gives an identical battle log`(playerCount: Int) {
        val players = NAMES.take(playerCount)
        val logs = List(2) {
            lateinit var log: JsonNode
            apiTest { api -> log = play(api, players)["battleLog"] }
            log
        }

        assertEquals(logs[0], logs[1])
    }

    /** Plays the scripted game and returns the host's final view. */
    private suspend fun play(api: TestApi, players: List<String>): JsonNode {
        val gameId = api.createGame(players.size, players.first(), SEED)
        players.drop(1).forEach { api.join(gameId, it) }

        var turn = 1
        while (api.view(gameId, players.first())["phase"].asText() != "GAME_OVER") {
            check(turn <= MAX_TURNS) { "Game didn't end within $MAX_TURNS turns" }
            if (turn > 1) upgradeAll(api, gameId, players.first())
            players.forEach { expectOk(api.post("/games/$gameId/players/$it/ready")) }
            players.forEach { expectOk(api.post("/games/$gameId/players/$it/army", army(api, gameId, it, turn, players))) }
            turn++
        }
        return api.view(gameId, players.first())
    }

    private suspend fun upgradeAll(api: TestApi, gameId: String, name: String) {
        while (true) {
            val hand = api.view(gameId, name)["me"]["hand"]
            val into = when {
                !hand.has("HORDE") -> return
                hand.has("IRON") -> "BOW"
                hand.has("WOOD") -> "SHIELD"
                else -> return
            }
            expectOk(api.post("/games/$gameId/players/$name/upgrades", mapOf("into" to into)))
        }
    }

    private suspend fun army(api: TestApi, gameId: String, name: String, turn: Int, players: List<String>) =
        when {
            turn == 1 || name != players.first() -> mapOf("HORDE" to 1)
            else -> {
                val hand = api.view(gameId, name)["me"]["hand"]
                CREATURES.filter { hand.has(it) }.associateWith { hand[it].asInt() }
            }
        }

    private suspend fun expectOk(response: HttpResponse) =
        assertEquals(HttpStatusCode.OK, response.status, TestApi.json(response).toString())
}
