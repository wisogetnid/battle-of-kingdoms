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

/** Which unit each resource upgrades into, and which unit counters it. */
private val UPGRADE = mapOf("WOOD" to "BOW", "IRON" to "SWORD", "ESSENCE" to "MAGE")
private val COUNTERED_BY = mapOf("BOW" to "SWORD", "SWORD" to "MAGE", "MAGE" to "BOW")
private val RESOURCE_FOR = UPGRADE.entries.associate { (resource, unit) -> unit to resource }

/**
 * Full seeded games played over HTTP only, written as a script that reacts to the seeded deals:
 * - turn 1: everyone commits 1 Horde, which forces a tie and a rollover;
 * - later turns: if ben can build k units of some kind and anna can build k of its counter,
 *   both do and commit exactly those k units, while everyone else commits 1 Horde. anna and ben
 *   field equally large armies, so anna wins through her counter bonus alone;
 * - otherwise everyone commits 1 Horde again and the pot keeps rolling over.
 */
class EndToEndMultiplayerTest {

    @ParameterizedTest(name = "{0} players")
    @ValueSource(ints = [3, 5])
    fun `a seeded game with a rollover and counter wins is played over HTTP to a winner`(playerCount: Int) =
        apiTest { api ->
            val players = NAMES.take(playerCount)

            val final = play(api, players)

            assertEquals("GAME_OVER", final["phase"].asText())
            assertEquals("anna", final["winner"].asText())
            assertTrue(final["me"]["vp"].asInt() >= 3)
            assertTrue(final["availableActions"].isEmpty)
            val log = final["battleLog"].toList()
            assertTrue(log.first()["winner"].isNull, "turn 1 should be a tie")
            assertEquals(2, log[1]["pot"].asInt(), "the tied victory point should roll over")
            assertEquals(players.size * (players.size - 1) / 2, log.first()["duels"].size())
            val won = log.filter { !it["winner"].isNull }
            assertTrue(won.isNotEmpty() && won.all { decidedByCounter(it) }, "every win should come from a counter")
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
        val (anna, ben) = players
        val gameId = api.createGame(players.size, anna, SEED)
        players.drop(1).forEach { api.join(gameId, it) }

        var turn = 1
        while (api.view(gameId, anna)["phase"].asText() != "GAME_OVER") {
            check(turn <= MAX_TURNS) { "Game didn't end within $MAX_TURNS turns" }
            val duel = if (turn == 1) null else counterDuel(hand(api, gameId, anna), hand(api, gameId, ben))
            val armies = players.associateWith { mapOf("HORDE" to 1) }.toMutableMap()
            if (duel != null) {
                val (bensUnit, annasUnit, k) = duel
                repeat(k) { upgrade(api, gameId, ben, bensUnit) }
                repeat(k) { upgrade(api, gameId, anna, annasUnit) }
                armies[ben] = mapOf(bensUnit to k)
                armies[anna] = mapOf(annasUnit to k)
            }
            players.forEach { expectOk(api.post("/games/$gameId/players/$it/ready")) }
            players.forEach { expectOk(api.post("/games/$gameId/players/$it/army", armies.getValue(it))) }
            turn++
        }
        return api.view(gameId, anna)
    }

    /** The largest matchup where ben builds `k` of a unit and anna `k` of its counter, if any. */
    private fun counterDuel(annasHand: JsonNode, bensHand: JsonNode): Triple<String, String, Int>? =
        UPGRADE.values.map { bensUnit ->
            val annasUnit = COUNTERED_BY.getValue(bensUnit)
            val k = minOf(
                bensHand.path(RESOURCE_FOR.getValue(bensUnit)).asInt(0),
                annasHand.path(RESOURCE_FOR.getValue(annasUnit)).asInt(0),
                bensHand.path("HORDE").asInt(0),
                annasHand.path("HORDE").asInt(0),
            )
            Triple(bensUnit, annasUnit, k)
        }.filter { it.third > 0 }.maxByOrNull { it.third }

    /** Won through a counter: without bonuses the winner's army wasn't strictly the largest. */
    private fun decidedByCounter(entry: JsonNode): Boolean {
        val sizes = entry["armies"].fields().asSequence().associate { (name, army) ->
            name to army["army"].fields().asSequence().sumOf { it.value.asInt() }
        }
        val winner = entry["winner"].asText()
        return sizes.any { (name, size) -> name != winner && size >= sizes.getValue(winner) }
    }

    private suspend fun hand(api: TestApi, gameId: String, name: String) = api.view(gameId, name)["me"]["hand"]

    private suspend fun upgrade(api: TestApi, gameId: String, name: String, into: String) =
        expectOk(api.post("/games/$gameId/players/$name/upgrades", mapOf("into" to into)))

    private suspend fun expectOk(response: HttpResponse) =
        assertEquals(HttpStatusCode.OK, response.status, TestApi.json(response).toString())
}
