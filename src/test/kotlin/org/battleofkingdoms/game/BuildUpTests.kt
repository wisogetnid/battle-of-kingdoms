package org.battleofkingdoms.game

import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.CreatureType.BOW
import org.battleofkingdoms.cards.CreatureType.HIGH_MAGE
import org.battleofkingdoms.cards.CreatureType.HORDE
import org.battleofkingdoms.cards.CreatureType.KNIGHT
import org.battleofkingdoms.cards.CreatureType.MAGE
import org.battleofkingdoms.cards.CreatureType.SWORD
import org.battleofkingdoms.cards.CreatureType.WARBOW
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
    @MethodSource("tier2Upgrades")
    fun `a tier-2 upgrade consumes the settled tier-1 unit and one more resource, no Horde`(
        into: CreatureType,
        from: CreatureType,
        resource: ResourceType,
    ) {
        val game = startedGame(hand = Hand.of(HORDE to 1, from to 1, resource to 1))

        val upgraded = game.upgrade("anna", into)

        assertEquals(Hand.of(HORDE to 1, into to 1), upgraded.player("anna").hand)
        assertEquals(mapOf(into to 1), upgraded.player("anna").upgradedThisTurn)
    }

    @ParameterizedTest
    @MethodSource("tier2Upgrades")
    fun `a tier-2 upgrade without the tier-1 unit or its resource is a rule violation`(
        into: CreatureType,
        from: CreatureType,
        resource: ResourceType,
    ) {
        assertFailsWith<RuleViolation> { startedGame(hand = Hand.of(HORDE to 2, resource to 2)).upgrade("anna", into) }
        assertFailsWith<RuleViolation> { startedGame(hand = Hand.of(HORDE to 2, from to 1)).upgrade("anna", into) }
    }

    @ParameterizedTest
    @MethodSource("tier2Upgrades")
    fun `a unit upgraded this turn can't climb again until next turn`(
        into: CreatureType,
        from: CreatureType,
        resource: ResourceType,
    ) {
        val sameTurn = startedGame(hand = Hand.of(HORDE to 1, resource to 2)).upgrade("anna", from)

        assertFailsWith<RuleViolation> { sameTurn.upgrade("anna", into) }

        val nextTurn = nextTurn(sameTurn)
        assertEquals(emptyMap(), nextTurn.player("anna").upgradedThisTurn)
        assertEquals(1, nextTurn.upgrade("anna", into).player("anna").hand.count(into))
    }

    @Test
    fun `only units held since before this turn can climb`() {
        val game = startedGame(hand = Hand.of(HORDE to 1, BOW to 1, WOOD to 3)).upgrade("anna", BOW)

        val once = game.upgrade("anna", WARBOW) // the settled Bow
        assertEquals(mapOf(BOW to 1, WARBOW to 1), once.player("anna").upgradedThisTurn)
        assertFailsWith<RuleViolation> { once.upgrade("anna", WARBOW) } // only the fresh Bow is left
    }

    @Test
    fun `upgrading into a tier-1 unit is tracked for this turn only`() {
        val game = startedGame(hand = Hand.of(HORDE to 2, WOOD to 1, IRON to 1))
            .upgrade("anna", BOW).upgrade("anna", SWORD)

        assertEquals(mapOf(BOW to 1, SWORD to 1), game.player("anna").upgradedThisTurn)
        assertEquals(emptyMap(), game.player("ben").upgradedThisTurn)
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

    @Test
    fun `affordable upgrades are the tier-1 units whose resource I hold`() {
        val game = startedGame(hand = Hand.of(HORDE to 1, WOOD to 1, ESSENCE to 2))

        assertEquals(listOf(BOW, MAGE), game.affordableUpgrades("anna"))
    }

    @Test
    fun `a settled tier-1 unit with its resource makes tier 2 affordable`() {
        val game = startedGame(hand = Hand.of(BOW to 1, SWORD to 1, WOOD to 1))

        assertEquals(listOf(WARBOW), game.affordableUpgrades("anna"))
    }

    @Test
    fun `a unit upgraded this turn doesn't make tier 2 affordable`() {
        val game = startedGame(hand = Hand.of(HORDE to 1, WOOD to 2)).upgrade("anna", BOW)

        assertEquals(emptyList(), game.affordableUpgrades("anna"))
        assertFailsWith<RuleViolation> { game.upgrade("anna", WARBOW) }
    }

    @Test
    fun `nothing is affordable once ready or outside build-up`() {
        val hand = Hand.of(HORDE to 1, WOOD to 1, IRON to 1, ESSENCE to 1)
        val ready = startedGame(hand = hand).ready("anna")
        assertEquals(emptyList(), ready.affordableUpgrades("anna"))

        val battle = allReady(startedGame(hand = hand))
        assertEquals(BATTLE, battle.phase)
        assertEquals(emptyList(), battle.affordableUpgrades("anna"))

        val waiting = Game.create(GameConfig(), SEED, 3, "anna")
        assertEquals(emptyList(), waiting.affordableUpgrades("anna"))
    }

    @Test
    fun `every affordable upgrade succeeds and every other one is rejected`() {
        val game = startedGame(hand = Hand.of(HORDE to 1, MAGE to 1, ESSENCE to 1, IRON to 1))
        val affordable = game.affordableUpgrades("anna")

        CreatureType.entries.forEach { into ->
            val succeeded = runCatching { game.upgrade("anna", into) }.isSuccess
            assertEquals(into in affordable, succeeded, "upgrade into $into")
        }
    }

    private fun startedGame(hand: Hand? = null): Game {
        val game = Game.create(GameConfig(), SEED, 3, "anna").join("ben").join("cleo")
        return when (hand) {
            null -> game
            else -> game.copy(players = game.players + ("anna" to game.player("anna").copy(hand = hand)))
        }
    }

    private fun allReady(game: Game): Game = game.players.keys.fold(game) { g, name -> g.ready(name) }

    /** Everyone readies and commits nothing: a tie, then the next turn's build-up. */
    private fun nextTurn(game: Game): Game =
        allReady(game).let { battle -> battle.players.keys.fold(battle) { g, name -> g.commitArmy(name, Army()) } }

    companion object {
        @JvmStatic
        fun resources() = ResourceType.entries.toList()

        @JvmStatic
        fun tier2Upgrades() = listOf(
            Arguments.of(WARBOW, BOW, WOOD),
            Arguments.of(KNIGHT, SWORD, IRON),
            Arguments.of(HIGH_MAGE, MAGE, ESSENCE),
        )

        @JvmStatic
        fun upgrades() = listOf(
            Arguments.of(BOW, WOOD),
            Arguments.of(SWORD, IRON),
            Arguments.of(MAGE, ESSENCE),
        )
    }
}
