package org.battleofkingdoms.simulator

import org.battleofkingdoms.cards.Artifact
import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.cards.CreatureType.BOW
import org.battleofkingdoms.cards.CreatureType.KNIGHT
import org.battleofkingdoms.cards.CreatureType.MAGE
import org.battleofkingdoms.cards.CreatureType.SWORD
import org.battleofkingdoms.cards.CreatureType.WARBOW
import org.battleofkingdoms.cards.Line
import org.battleofkingdoms.cards.ResourceType.IRON
import org.battleofkingdoms.game.Action
import org.battleofkingdoms.game.Phase
import org.battleofkingdoms.server.MyView
import org.battleofkingdoms.server.PlayerView
import org.battleofkingdoms.simulator.strategies.MonoLine
import org.battleofkingdoms.simulator.strategies.RandomStrategy
import org.battleofkingdoms.simulator.strategies.TierOneFocus
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A build-up view of "me" with [hand] (card names to counts), as the engine would offer it. */
fun buildUpView(
    hand: Map<String, Int>,
    affordable: List<CreatureType> = emptyList(),
    actions: List<Action> = listOf(Action.TRADE, Action.UPGRADE, Action.READY),
    artifacts: List<Artifact> = emptyList(),
) = PlayerView(
    gameId = UUID(0, 0),
    phase = Phase.BUILD_UP,
    turn = 3,
    pot = 1,
    victoryPointsToWin = 3,
    me = MyView("me", hand, emptyMap(), vp = 0, acted = false, artifacts = artifacts),
    others = emptyList(),
    availableActions = actions,
    battleLog = emptyList(),
    winner = null,
    affordableUpgrades = affordable,
)

/** Every registered strategy plays whole games against `random` without breaking a rule; how well it plays isn't tested. */
class StrategyTests {

    @ParameterizedTest(name = "{0}")
    @MethodSource("strategies")
    fun `plays full seeded games against random without errors`(id: String, strategy: Strategy) {
        (1L..5L).forEach { seed ->
            playGame(
                listOf(id to strategy) + List(PLAYERS - 1) { "random" to RandomStrategy },
                gameSeed = seed,
                engineSeed = seed * 31,
                botSeeds = List(PLAYERS) { seed + it },
            )
        }
    }

    @Test
    fun `mono-line climbs its own line, tier 2 first, and trades every other resource`() {
        val plan = MonoLine(Line.BOW)
        val move = { hand: Map<String, Int>, affordable: List<CreatureType> -> plan.buildUp(buildUpView(hand, affordable), Random(0)) }

        assertEquals(BuildUpMove.Upgrade(WARBOW), move(mapOf("BOW" to 1, "HORDE" to 1, "WOOD" to 2), listOf(BOW, WARBOW)))
        assertEquals(BuildUpMove.Upgrade(BOW), move(mapOf("HORDE" to 1, "WOOD" to 1, "IRON" to 1), listOf(BOW, SWORD)))
        assertEquals(BuildUpMove.Trade(IRON), move(mapOf("HORDE" to 1, "IRON" to 1), listOf(SWORD)))
        assertEquals(BuildUpMove.Ready, move(mapOf("HORDE" to 1, "WOOD" to 1), emptyList()))
    }

    @Test
    fun `tier-1-focus only ever takes tier-1 upgrades and never trades`() {
        val hand = mapOf("HORDE" to 2, "SWORD" to 1, "IRON" to 2, "WOOD" to 1, "ESSENCE" to 1)
        val move = { affordable: List<CreatureType>, seed: Int -> TierOneFocus.buildUp(buildUpView(hand, affordable), Random(seed)) }

        assertEquals(BuildUpMove.Upgrade(SWORD), move(listOf(SWORD, KNIGHT), 0))
        val picks = (1..30).map { (move(listOf(BOW, MAGE, KNIGHT), it) as BuildUpMove.Upgrade).into }
        assertEquals(setOf(BOW, MAGE), picks.toSet())
        assertTrue(picks.none { it.tier == 2 })
        assertEquals(BuildUpMove.Ready, move(listOf(KNIGHT), 0))
    }

    companion object {
        @JvmStatic
        fun strategies() = registry.map { (id, strategy) -> Arguments.of(id, strategy) }
    }
}
