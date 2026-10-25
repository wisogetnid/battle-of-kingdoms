package org.battleofkingdoms.game

import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.cards.CreatureType.BOW
import org.battleofkingdoms.cards.CreatureType.HORDE
import org.battleofkingdoms.cards.CreatureType.MAGE
import org.battleofkingdoms.cards.CreatureType.SWORD
import org.battleofkingdoms.cards.Hand
import org.battleofkingdoms.cards.ResourceType
import org.battleofkingdoms.cards.ResourceType.ESSENCE
import org.battleofkingdoms.cards.ResourceType.IRON
import org.battleofkingdoms.cards.ResourceType.WOOD
import org.battleofkingdoms.game.Phase.BATTLE
import org.battleofkingdoms.game.Phase.BUILD_UP
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SEED = 42L

class BuildUpTests {

    @ParameterizedTest
    @MethodSource("upgrades")
    fun `an upgrade consumes 1 Horde and the matching resource`(into: CreatureType, resource: ResourceType) {
        val game = startedGame(hand = Hand.of(HORDE to 2, WOOD to 1, IRON to 1, ESSENCE to 1))

        val upgraded = game.upgrade("anna", into)

        val (otherA, otherB) = ResourceType.entries.filter { it != resource }
        assertEquals(Hand.of(HORDE to 1, into to 1, otherA to 1, otherB to 1), upgraded.player("anna").hand)
    }

    @ParameterizedTest
    @MethodSource("upgrades")
    fun `an upgrade without the matching resource is a rule violation`(into: CreatureType, resource: ResourceType) {
        val (otherA, otherB) = ResourceType.entries.filter { it != resource }
        val game = startedGame(hand = Hand.of(HORDE to 1, otherA to 3, otherB to 3))

        assertFailsWith<RuleViolation> { game.upgrade("anna", into) }
    }

    @ParameterizedTest
    @MethodSource("upgrades")
    fun `an upgrade without a Horde is a rule violation`(into: CreatureType, resource: ResourceType) {
        val game = startedGame(hand = Hand.of(resource to 1))

        assertFailsWith<RuleViolation> { game.upgrade("anna", into) }
    }

    @Test
    fun `upgrading into a Horde is a rule violation`() {
        val game = startedGame(hand = Hand.of(HORDE to 1, WOOD to 1, IRON to 1))

        assertFailsWith<RuleViolation> { game.upgrade("anna", HORDE) }
    }

    @Test
    fun `a player may upgrade several times`() {
        val game = startedGame(hand = Hand.of(HORDE to 4, WOOD to 2, IRON to 1, ESSENCE to 1))

        val upgraded = game.upgrade("anna", BOW).upgrade("anna", BOW).upgrade("anna", SWORD).upgrade("anna", MAGE)

        assertEquals(Hand.of(BOW to 2, SWORD to 1, MAGE to 1), upgraded.player("anna").hand)
    }

    @Test
    fun `an upgrade after ready is rejected`() {
        val game = startedGame(hand = Hand.of(HORDE to 1, WOOD to 1)).ready("anna")

        assertFailsWith<AlreadyActed> { game.upgrade("anna", BOW) }
    }

    @Test
    fun `an upgrade outside build-up is rejected`() {
        val waiting = Game.create(GameConfig(), SEED, 3, "anna")
        val battle = allReady(startedGame())

        assertFailsWith<WrongPhase> { waiting.upgrade("anna", BOW) }
        assertFailsWith<WrongPhase> { battle.upgrade("anna", BOW) }
    }

    @Test
    fun `an upgrade by an unknown player is rejected`() {
        assertFailsWith<UnknownPlayer> { startedGame().upgrade("zed", BOW) }
    }

    @Test
    fun `ready marks only that player as acted`() {
        val game = startedGame().ready("anna")

        assertTrue(game.player("anna").acted)
        assertFalse(game.player("ben").acted)
        assertEquals(BUILD_UP, game.phase)
    }

    @Test
    fun `ready twice is rejected`() {
        assertFailsWith<AlreadyActed> { startedGame().ready("anna").ready("anna") }
    }

    @Test
    fun `ready outside build-up is rejected`() {
        val waiting = Game.create(GameConfig(), SEED, 3, "anna")

        assertFailsWith<WrongPhase> { waiting.ready("anna") }
        assertFailsWith<WrongPhase> { allReady(startedGame()).ready("anna") }
    }

    @Test
    fun `ready by an unknown player is rejected`() {
        assertFailsWith<UnknownPlayer> { startedGame().ready("zed") }
    }

    @Test
    fun `when all players are ready the battle starts with nobody having acted`() {
        val game = allReady(startedGame())

        assertEquals(BATTLE, game.phase)
        assertTrue(game.players.values.none { it.acted })
    }

    @ParameterizedTest
    @MethodSource("resources")
    fun `a trade swaps one resource for one Horde`(resource: ResourceType) {
        val game = startedGame(hand = Hand.of(HORDE to 1, resource to 2))

        val traded = game.trade("anna", resource)

        assertEquals(Hand.of(HORDE to 2, resource to 1), traded.player("anna").hand)
    }

    @Test
    fun `a player may trade every resource, then upgrade with none left`() {
        val game = startedGame(hand = Hand.of(WOOD to 1, IRON to 1, ESSENCE to 1))

        val traded = game.trade("anna", WOOD).trade("anna", IRON).trade("anna", ESSENCE)

        assertEquals(Hand.of(HORDE to 3), traded.player("anna").hand)
        assertFailsWith<RuleViolation> { traded.upgrade("anna", BOW) }
    }

    @Test
    fun `trading a resource I don't have is a rule violation`() {
        val game = startedGame(hand = Hand.of(HORDE to 1, WOOD to 1))

        assertFailsWith<RuleViolation> { game.trade("anna", IRON) }
    }

    @Test
    fun `trading after ready, outside build-up, or as an unknown player is rejected`() {
        val game = startedGame(hand = Hand.of(WOOD to 2))

        assertFailsWith<AlreadyActed> { game.ready("anna").trade("anna", WOOD) }
        assertFailsWith<WrongPhase> { Game.create(GameConfig(), SEED, 3, "anna").trade("anna", WOOD) }
        assertFailsWith<WrongPhase> { allReady(game).trade("anna", WOOD) }
        assertFailsWith<UnknownPlayer> { game.trade("zed", WOOD) }
    }

    @Test
    fun `build-up offers trade, upgrade and ready until the player is ready`() {
        val game = startedGame()

        assertEquals(listOf(Action.TRADE, Action.UPGRADE, Action.READY), game.availableActions("anna"))
        assertEquals(emptyList(), game.ready("anna").availableActions("anna"))
    }

    private fun startedGame(hand: Hand? = null): Game {
        val game = Game.create(GameConfig(), SEED, 3, "anna").join("ben").join("cleo")
        return when (hand) {
            null -> game
            else -> game.copy(players = game.players + ("anna" to game.player("anna").copy(hand = hand)))
        }
    }

    private fun allReady(game: Game): Game = game.players.keys.fold(game) { g, name -> g.ready(name) }

    companion object {
        @JvmStatic
        fun resources() = ResourceType.entries.toList()

        @JvmStatic
        fun upgrades() = listOf(
            Arguments.of(BOW, WOOD),
            Arguments.of(SWORD, IRON),
            Arguments.of(MAGE, ESSENCE),
        )
    }
}
