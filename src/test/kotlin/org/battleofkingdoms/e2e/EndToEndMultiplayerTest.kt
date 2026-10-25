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
private const val MAX_TURNS = 30
private val NAMES = listOf("anna", "ben", "cleo", "dora", "emil")
private val RESOURCES = listOf("WOOD", "IRON", "ESSENCE")

/** Tier 1 by resource, and tier 2 with the resource it costs, by tier-1 unit. */
private val TIER_1 = mapOf("WOOD" to "BOW", "IRON" to "SWORD", "ESSENCE" to "MAGE")
private val TIER_2 = mapOf("BOW" to ("WARBOW" to "WOOD"), "SWORD" to ("KNIGHT" to "IRON"), "MAGE" to ("HIGH_MAGE" to "ESSENCE"))

/**
 * Full seeded games played over HTTP only, written as a script that reacts to the seeded deals:
 * - turn 1: everyone commits 1 Horde, which forces a tie and a rollover;
 * - every turn, everyone but anna trades all their resources for Horde and commits 1 Horde;
 * - anna upgrades Horde into tier-1 units and keeps those at home, then upgrades settled
 *   tier-1 units into tier 2 when she draws the resource. She commits only her tier-2 units,
 *   which get the tier-2 bonus against the others' Horde; without any yet, she commits
 *   1 Horde and the battle is a tie.
 */
class EndToEndMultiplayerTest {

    @ParameterizedTest(name = "{0} players")
    @ValueSource(ints = [3, 5])
    fun `a seeded game with a rollover, trades and tier-2 units is played over HTTP to 5 VP`(playerCount: Int) =
        apiTest { api ->
            val players = NAMES.take(playerCount)

            val (final, trades) = play(api, players)

            assertEquals("GAME_OVER", final["phase"].asText())
            assertEquals("anna", final["winner"].asText())
            assertTrue(final["me"]["vp"].asInt() >= 5)
            assertTrue(final["availableActions"].isEmpty)
            assertTrue(trades > 0, "the others should have traded")
            val log = final["battleLog"].toList()
            assertTrue(log.first()["winner"].isNull, "turn 1 should be a tie")
            assertEquals(2, log[1]["pot"].asInt(), "the tied victory point should roll over")
            assertEquals(players.size * (players.size - 1) / 2, log.first()["duels"].size())
            val annasArmies = log.map { it["armies"]["anna"]["army"] }
            assertTrue(annasArmies.any { army -> TIER_2.values.any { (tier2, _) -> army.has(tier2) } }, "anna should field a tier-2 unit")
        }

    @ParameterizedTest(name = "{0} players")
    @ValueSource(ints = [3, 5])
    fun `replaying the same seed and actions over HTTP gives an identical battle log`(playerCount: Int) {
        val players = NAMES.take(playerCount)
        val logs = List(2) {
            lateinit var log: JsonNode
            apiTest { api -> log = play(api, players).first["battleLog"] }
            log
        }

        assertEquals(logs[0], logs[1])
    }

    /** Plays the scripted game; returns the host's final view and the number of trades made. */
    private suspend fun play(api: TestApi, players: List<String>): Pair<JsonNode, Int> {
        val anna = players.first()
        val others = players.drop(1)
        val gameId = api.createGame(players.size, anna, SEED)
        others.forEach { api.join(gameId, it) }

        var trades = 0
        var turn = 1
        while (api.view(gameId, anna)["phase"].asText() != "GAME_OVER") {
            check(turn <= MAX_TURNS) { "Game didn't end within $MAX_TURNS turns" }
            upgradeAll(api, gameId, anna)
            others.forEach { trades += tradeAll(api, gameId, it) }
            players.forEach { expectOk(api.post("/games/$gameId/players/$it/ready")) }
            val armies = others.associateWith { mapOf("HORDE" to 1) } + (anna to annasArmy(api, gameId, anna, turn))
            players.forEach { expectOk(api.post("/games/$gameId/players/$it/army", armies.getValue(it))) }
            turn++
        }
        return api.view(gameId, anna) to trades
    }

    /** Tier 2 first for every settled tier-1 unit with its resource, then Horde into tier 1, keeping 1 Horde. */
    private suspend fun upgradeAll(api: TestApi, gameId: String, name: String) {
        while (true) {
            val me = api.view(gameId, name)["me"]
            val hand = me["hand"]
            val settled = { unit: String -> hand.path(unit).asInt(0) - me["upgradedThisTurn"].path(unit).asInt(0) }
            val into = TIER_2.entries.firstOrNull { (tier1, cost) -> settled(tier1) > 0 && hand.has(cost.second) }?.value?.first
                ?: RESOURCES.firstOrNull { hand.has(it) && hand.path("HORDE").asInt(0) > 1 }?.let(TIER_1::getValue)
                ?: return
            expectOk(api.post("/games/$gameId/players/$name/upgrades", mapOf("into" to into)))
        }
    }

    private suspend fun tradeAll(api: TestApi, gameId: String, name: String): Int {
        val hand = api.view(gameId, name)["me"]["hand"]
        val trades = RESOURCES.flatMap { resource -> List(hand.path(resource).asInt(0)) { resource } }
        trades.forEach { expectOk(api.post("/games/$gameId/players/$name/trades", mapOf("resource" to it))) }
        return trades.size
    }

    /** anna's tier-2 units, or 1 Horde in turn 1 or while she has none. */
    private suspend fun annasArmy(api: TestApi, gameId: String, name: String, turn: Int): Map<String, Int> {
        val hand = api.view(gameId, name)["me"]["hand"]
        val tier2 = TIER_2.values.map { it.first }.filter { hand.has(it) }.associateWith { hand[it].asInt() }
        return when {
            turn == 1 || tier2.isEmpty() -> mapOf("HORDE" to 1)
            else -> tier2
        }
    }

    private suspend fun expectOk(response: HttpResponse) =
        assertEquals(HttpStatusCode.OK, response.status, TestApi.json(response).toString())
}
