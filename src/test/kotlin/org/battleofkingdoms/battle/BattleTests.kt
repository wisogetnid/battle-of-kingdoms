package org.battleofkingdoms.battle

import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.cards.CreatureType.BOW
import org.battleofkingdoms.cards.CreatureType.HORDE
import org.battleofkingdoms.cards.CreatureType.MAGE
import org.battleofkingdoms.cards.CreatureType.SWORD
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.random.Random

class BattleTests {
    @ParameterizedTest(name = "{2}")
    @MethodSource("winners")
    fun `the single strongest army wins, ties at the top mean no winner`(
        commits: Map<String, Army>,
        expectedWinner: String?,
        case: String,
    ) {
        Assertions.assertEquals(expectedWinner, resolve(commits).winner)
    }

    @Test
    fun `every pair fights one duel in join order`() {
        val result = resolve(mapOf("anna" to Army.of(HORDE to 1), "ben" to Army(), "cleo" to Army.of(HORDE to 2)))

        Assertions.assertEquals(
            listOf(Duel("anna", "ben", 1, 0), Duel("anna", "cleo", 1, 2), Duel("ben", "cleo", 0, 2)),
            result.duels,
        )
    }

    @ParameterizedTest(name = "{0} players")
    @MethodSource("playerCounts")
    fun `a round robin fights every pair exactly once`(players: Int) {
        val commits = (1..players).associate { "p$it" to Army.of(HORDE to it) }

        Assertions.assertEquals(players * (players - 1) / 2, resolve(commits).duels.size)
    }

    @Test
    fun `a bonus counts only in the duel against the countered opponent`() {
        val result = resolve(mapOf("anna" to Army.of(BOW to 2), "ben" to Army.of(MAGE to 1), "cleo" to Army.of(HORDE to 3)))

        Assertions.assertEquals(
            listOf(Duel("anna", "ben", 4, 1), Duel("anna", "cleo", 2, 3), Duel("ben", "cleo", 1, 3)),
            result.duels,
        )
    }

    @Test
    fun `a score is the sum of duel margins`() {
        val result = resolve(mapOf("anna" to Army.of(BOW to 2), "ben" to Army.of(MAGE to 1), "cleo" to Army.of(HORDE to 3)))

        // anna: +3 vs ben, -1 vs cleo; ben: -3 vs anna, -2 vs cleo; cleo: +1 vs anna, +2 vs ben
        Assertions.assertEquals(mapOf("anna" to 2, "ben" to -5, "cleo" to 3), result.armies.mapValues { it.value.score })
    }

    @Test
    fun `the worked example from the spec`() {
        val result = resolve(
            mapOf(
                "anna" to Army.of(BOW to 2, HORDE to 1),
                "ben" to Army.of(SWORD to 2),
                "cleo" to Army.of(MAGE to 1, HORDE to 2),
            )
        )

        Assertions.assertEquals(
            listOf(Duel("anna", "ben", 3, 4), Duel("anna", "cleo", 5, 3), Duel("ben", "cleo", 2, 4)),
            result.duels,
        )
        Assertions.assertEquals(mapOf("anna" to 1, "ben" to -1, "cleo" to 0), result.armies.mapValues { it.value.score })
        Assertions.assertEquals("anna", result.winner)
    }

    @Test
    fun `going wide - an all-horde army beats upgraded armies that only counter each other`() {
        val result = resolve(
            mapOf(
                "anna" to Army.of(HORDE to 4),
                "ben" to Army.of(BOW to 2, SWORD to 1),
                "cleo" to Army.of(MAGE to 2, HORDE to 1),
            )
        )

        // ben's Bows and cleo's Mages both get bonuses against each other, but not against anna
        Assertions.assertEquals(
            listOf(Duel("anna", "ben", 4, 3), Duel("anna", "cleo", 4, 3), Duel("ben", "cleo", 5, 5)),
            result.duels,
        )
        Assertions.assertEquals("anna", result.winner)
    }

    @Test
    fun `the counter bonus size is passed in`() {
        val commits = mapOf("anna" to Army.of(SWORD to 2), "ben" to Army.of(BOW to 1, HORDE to 1))

        Assertions.assertNull(Battle.resolve(commits, pot = 1, counterBonus = 0, Random(SEED)).winner)
        Assertions.assertEquals("anna", Battle.resolve(commits, pot = 1, counterBonus = 1, Random(SEED)).winner)
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("winners")
    fun `scores always add up to zero`(commits: Map<String, Army>, expectedWinner: String?, case: String) {
        Assertions.assertEquals(0, resolve(commits).armies.values.sumOf { it.score })
    }

    @Test
    fun `without bonuses the largest army wins by n times its size minus the total`() {
        val result = resolve(mapOf("anna" to Army.of(HORDE to 5), "ben" to Army.of(HORDE to 2), "cleo" to Army.of(BOW to 2)))

        // strengths 5, 2, 2 (total 9): score = 3 × strength − 9
        Assertions.assertEquals(mapOf("anna" to 6, "ben" to -3, "cleo" to -3), result.armies.mapValues { it.value.score })
        Assertions.assertEquals("anna", result.winner)
    }

    @Test
    fun `the winner gains the whole pot`() {
        val result = resolve(mapOf("anna" to Army.of(BOW to 2), "ben" to Army.of(HORDE to 1), "cleo" to Army()), pot = 3)

        Assertions.assertEquals(mapOf("anna" to 3, "ben" to 0, "cleo" to 0), result.armies.mapValues { it.value.vpGained })
        Assertions.assertFalse(result.rolledOver)
        Assertions.assertEquals(3, result.pot)
    }

    @Test
    fun `with no winner nobody gains VP and the pot rolls over`() {
        val result = resolve(mapOf("anna" to Army.of(HORDE to 2), "ben" to Army.of(BOW to 2), "cleo" to Army()), pot = 2)

        Assertions.assertTrue(result.armies.values.all { it.vpGained == 0 })
        Assertions.assertTrue(result.rolledOver)
        Assertions.assertEquals(2, result.pot)
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("losses")
    fun `the winner loses floor half, everyone else ceil half`(
        commits: Map<String, Army>,
        player: String,
        expectedLosses: Int,
        case: String,
    ) {
        val result = resolve(commits).armies.getValue(player)

        Assertions.assertEquals(expectedLosses, result.casualties.size)
        Assertions.assertEquals(result.army.size - expectedLosses, result.survivors.size)
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    fun `casualties and survivors split the committed army`(seed: Long) {
        val army = Army.of(HORDE to 3, BOW to 2, SWORD to 2, MAGE to 1)
        val result = Battle.resolve(mapOf("anna" to army, "ben" to Army.of(HORDE to 1)), pot = 1, BONUS, Random(seed))
            .armies.getValue("anna")

        CreatureType.entries.forEach { type ->
            Assertions.assertEquals(army.creatures[type] ?: 0, count(result.casualties, type) + count(result.survivors, type))
        }
    }

    @Test
    fun `casualties are picked by the seeded random`() {
        val commits = mapOf("anna" to Army.of(HORDE to 2, BOW to 2), "ben" to Army.of(HORDE to 1, SWORD to 1, MAGE to 1))
        val outcomes = (0L..20L).map { seed -> Battle.resolve(commits, pot = 1, BONUS, Random(seed)).armies.mapValues { it.value.casualties } }

        Assertions.assertEquals(outcomes, (0L..20L).map { seed -> Battle.resolve(commits, pot = 1, BONUS, Random(seed)).armies.mapValues { it.value.casualties } })
        Assertions.assertTrue(outcomes.distinct().size > 1, "different seeds should pick different casualties")
    }

    @Test
    fun `a fixed seed always picks the same casualties`() {
        val commits = mapOf("anna" to Army.of(HORDE to 3, BOW to 2), "ben" to Army.of(HORDE to 1, SWORD to 1, BOW to 1))

        val result = Battle.resolve(commits, pot = 1, BONUS, Random(42))

        Assertions.assertEquals("anna", result.winner)
        Assertions.assertEquals(Army.of(HORDE to 1, BOW to 1), result.armies.getValue("anna").casualties)
        Assertions.assertEquals(Army.of(HORDE to 1, SWORD to 1), result.armies.getValue("ben").casualties)
    }

    @Test
    fun `the result records every player's army`() {
        val commits = mapOf("anna" to Army.of(HORDE to 2), "ben" to Army(), "cleo" to Army.of(BOW to 1, SWORD to 1, MAGE to 1))

        val result = resolve(commits)

        Assertions.assertEquals(commits, result.armies.mapValues { it.value.army })
    }

    private fun resolve(commits: Map<String, Army>, pot: Int = 1) = Battle.resolve(commits, pot, BONUS, Random(SEED))

    private fun count(army: Army, type: CreatureType) = army.creatures[type] ?: 0

    companion object {
        private const val SEED = 7L
        private const val BONUS = 1

        @JvmStatic
        fun winners() = listOf(
            Arguments.of(mapOf("anna" to Army.of(HORDE to 3), "ben" to Army.of(HORDE to 2), "cleo" to Army.of(HORDE to 1)), "anna", "highest strength wins"),
            Arguments.of(mapOf("anna" to Army.of(HORDE to 2), "ben" to Army.of(SWORD to 1, HORDE to 1), "cleo" to Army.of(HORDE to 1)), null, "tie at the top"),
            Arguments.of(mapOf("anna" to Army.of(HORDE to 1), "ben" to Army.of(HORDE to 3), "cleo" to Army.of(HORDE to 1)), "ben", "tie below the top doesn't matter"),
            Arguments.of(mapOf("anna" to Army(), "ben" to Army(), "cleo" to Army()), null, "nobody commits"),
            Arguments.of(mapOf("anna" to Army(), "ben" to Army.of(HORDE to 1), "cleo" to Army()), "ben", "single committer wins"),
            Arguments.of(mapOf("anna" to Army.of(BOW to 2), "ben" to Army.of(MAGE to 1), "cleo" to Army.of(HORDE to 3)), "cleo", "a bonus in one duel doesn't outweigh losing the other"),
            Arguments.of(mapOf("anna" to Army.of(BOW to 3), "ben" to Army.of(MAGE to 1), "cleo" to Army.of(HORDE to 3)), "anna", "a decisive duel margin decides"),
            Arguments.of(mapOf("anna" to Army.of(SWORD to 2), "ben" to Army.of(BOW to 1, HORDE to 2), "cleo" to Army()), "anna", "a counter beats a bigger army"),
        )

        @JvmStatic
        fun losses() = listOf(
            Arguments.of(mapOf("anna" to Army.of(HORDE to 5), "ben" to Army.of(HORDE to 3)), "anna", 2, "winner with odd army loses floor"),
            Arguments.of(mapOf("anna" to Army.of(HORDE to 4), "ben" to Army.of(HORDE to 3)), "anna", 2, "winner with even army loses half"),
            Arguments.of(mapOf("anna" to Army.of(HORDE to 1), "ben" to Army()), "anna", 0, "winner with one creature loses none"),
            Arguments.of(mapOf("anna" to Army.of(HORDE to 5), "ben" to Army.of(HORDE to 3)), "ben", 2, "loser with odd army loses ceil"),
            Arguments.of(mapOf("anna" to Army.of(HORDE to 5), "ben" to Army.of(HORDE to 4)), "ben", 2, "loser with even army loses half"),
            Arguments.of(mapOf("anna" to Army.of(HORDE to 3), "ben" to Army.of(HORDE to 1)), "ben", 1, "loser with one creature loses it"),
            Arguments.of(mapOf("anna" to Army.of(HORDE to 3), "ben" to Army.of(BOW to 1, HORDE to 2)), "anna", 2, "tie: everyone loses ceil"),
            Arguments.of(mapOf("anna" to Army.of(HORDE to 3), "ben" to Army()), "ben", 0, "empty army loses nothing"),
        )

        @JvmStatic
        fun seeds() = (1L..10L).toList()

        @JvmStatic
        fun playerCounts() = listOf(3, 4, 5)
    }
}
