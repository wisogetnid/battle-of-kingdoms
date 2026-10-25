package org.battleofkingdoms.game

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.Artifact.TRANSMUTER_STONE
import org.battleofkingdoms.cards.Hand
import org.battleofkingdoms.cards.ResourceType
import org.battleofkingdoms.cards.ResourceType.ESSENCE
import org.battleofkingdoms.cards.ResourceType.IRON
import org.battleofkingdoms.cards.ResourceType.WOOD
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SEED = 42L

class TransmuteTests {

    @Test
    fun `the Transmuter Stone swaps one resource for one of the other kinds`() {
        val game = withStone(started(), "anna", Hand.of(WOOD to 2))

        val hand = game.transmute("anna", WOOD).player("anna").hand

        assertEquals(1, hand.count(WOOD))
        assertEquals(1, hand.count(IRON) + hand.count(ESSENCE))
    }

    @Test
    fun `the new resource is drawn with the seeded random, and both other kinds come up`() {
        val drawn = { seed: Long -> withStone(started(seed), "anna", Hand.of(WOOD to 1)).transmute("anna", WOOD).player("anna").hand }
        val draws = (1L..30L).map(drawn)

        assertEquals(draws, (1L..30L).map(drawn))
        assertEquals(setOf(IRON, ESSENCE), draws.flatMap { it.counts.keys }.toSet())
    }

    @Test
    fun `the Stone works once per turn and again next turn`() {
        val used = withStone(started(), "anna", Hand.of(WOOD to 3)).transmute("anna", WOOD)

        assertFailsWith<RuleViolation> { used.transmute("anna", WOOD) }
        assertFalse(Action.TRANSMUTE in used.availableActions("anna"))

        val turn2 = nextTurn(used)
        assertTrue(Action.TRANSMUTE in turn2.availableActions("anna"))
        assertEquals(turn2.player("anna").hand.count(WOOD) - 1, turn2.transmute("anna", WOOD).player("anna").hand.count(WOOD))
    }

    @Test
    fun `only the Stone's owner may transmute, a resource they hold, while building up`() {
        val game = withStone(started(), "anna", Hand.of(WOOD to 1))

        assertFailsWith<RuleViolation> { game.transmute("ben", game.player("ben").hand.counts.keys.filterIsInstance<ResourceType>().first()) }
        assertFailsWith<RuleViolation> { game.transmute("anna", IRON) }
        assertFailsWith<AlreadyActed> { game.ready("anna").transmute("anna", WOOD) }
        assertFailsWith<WrongPhase> { readyAll(game).transmute("anna", WOOD) }
        assertFailsWith<UnknownPlayer> { game.transmute("zed", WOOD) }
    }

    @Test
    fun `TRANSMUTE is listed for the Stone's owner during build-up only`() {
        val game = withStone(started(), "anna", Hand.of(WOOD to 1))

        assertEquals(listOf(Action.TRADE, Action.UPGRADE, Action.TRANSMUTE, Action.READY), game.availableActions("anna"))
        assertEquals(listOf(Action.TRADE, Action.UPGRADE, Action.READY), game.availableActions("ben"))
        assertEquals(listOf(Action.COMMIT_ARMY), readyAll(game).availableActions("anna"))
    }

    private fun started(seed: Long = SEED) = Game.create(GameConfig(artifacts = emptyList()), seed, 3, "anna").join("ben").join("cleo")

    /** [name] owns the Stone and holds exactly [hand]. */
    private fun withStone(game: Game, name: String, hand: Hand) =
        game.copy(players = game.players + (name to game.player(name).copy(hand = hand, artifacts = setOf(TRANSMUTER_STONE))))

    private fun readyAll(game: Game) = game.players.keys.fold(game) { g, name -> g.ready(name) }

    /** Everyone readies and commits nothing: a battle without a winner, then the next turn. */
    private fun nextTurn(game: Game): Game = readyAll(game).let { ready -> ready.players.keys.fold(ready) { g, name -> g.commitArmy(name, Army()) } }
}
