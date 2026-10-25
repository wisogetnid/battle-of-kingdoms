package org.battleofkingdoms.battle

import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.cards.CreatureType.BOW
import org.battleofkingdoms.cards.CreatureType.HORDE
import org.battleofkingdoms.cards.CreatureType.KNIGHT
import org.battleofkingdoms.cards.CreatureType.MAGE
import org.battleofkingdoms.cards.CreatureType.SWORD
import org.battleofkingdoms.cards.CreatureType.WARBOW
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
        val result = resolve(mapOf("anna" to Army.of(HORDE to 1), "ben" to Army.of(HORDE to 3), "cleo" to Army.of(HORDE to 2)))

        Assertions.assertEquals(
            listOf(Duel("anna", "ben", 1, 3), Duel("anna", "cleo", 1, 2), Duel("ben", "cleo", 3, 2)),
            result.duels,
        )
    }

    @Test
    fun `a player who commits nothing sits out and nobody scores against them`() {
        val result = resolve(mapOf("anna" to Army.of(HORDE to 1), "ben" to Army(), "cleo" to Army.of(HORDE to 2)))

        Assertions.assertEquals(listOf(Duel("anna", "cleo", 1, 2)), result.duels)
        Assertions.assertEquals(mapOf("anna" to -1, "ben" to 0, "cleo" to 1), result.armies.mapValues { it.value.score })
        Assertions.assertEquals(Army(), result.armies.getValue("ben").casualties)
        Assertions.assertEquals("cleo", result.winner)
    }

    @Test
    fun `beating an empty army no longer decides a battle`() {
        // anna and bob tie their duel 3:3; before, anna won by scoring 3 against cleo's empty army vs bob's 2
        val result = resolve(mapOf("anna" to Army.of(HORDE to 3), "bob" to Army.of(HORDE to 1, MAGE to 1), "cleo" to Army()))

        Assertions.assertEquals(listOf(Duel("anna", "bob", 3, 3)), result.duels)
        Assertions.assertEquals(mapOf("anna" to 0, "bob" to 0, "cleo" to 0), result.armies.mapValues { it.value.score })
        Assertions.assertNull(result.winner)
    }

    @Test
    fun `a lone army wins uncontested without duels`() {
        val result = resolve(mapOf("anna" to Army(), "ben" to Army.of(HORDE to 3), "cleo" to Army()))

        Assertions.assertEquals(emptyList<Duel>(), result.duels)
        Assertions.assertEquals("ben", result.winner)
        Assertions.assertEquals(0, result.armies.getValue("ben").score)
        Assertions.assertEquals(1, result.armies.getValue("ben").casualties.size) // the winner loses floor(3 / 2)
    }

    @ParameterizedTest(name = "{0} players")
    @MethodSource("playerCounts")
    fun `a round robin fights every pair exactly once`(players: Int) {
        val commits = (1..players).associate { "p$it" to Army.of(HORDE to it) }

        Assertions.assertEquals(players * (players - 1) / 2, resolve(commits).duels.size)
    }

    @Test
    fun `a bonus counts only in the duel against that opponent`() {
        val result = resolve(mapOf("anna" to Army.of(BOW to 2), "ben" to Army.of(MAGE to 1), "cleo" to Army.of(SWORD to 2)))

        // anna's Bows get +1 against ben's Mage only; cleo's Swords +1 against anna's Bows only;
        // ben's Mage +1 against cleo's Swords only
        Assertions.assertEquals(
            listOf(Duel("anna", "ben", 4, 1), Duel("anna", "cleo", 2, 4), Duel("ben", "cleo", 2, 2)),
            result.duels,
        )
    }

    @Test
    fun `a score is the sum of duel margins`() {
        val result = resolve(mapOf("anna" to Army.of(BOW to 2), "ben" to Army.of(MAGE to 1), "cleo" to Army.of(SWORD to 2)))

        // anna: +3 vs ben, -2 vs cleo; ben: -3 vs anna, 0 vs cleo; cleo: +2 vs anna, 0 vs ben
        Assertions.assertEquals(mapOf("anna" to 1, "ben" to -3, "cleo" to 2), result.armies.mapValues { it.value.score })
    }

    @Test
    fun `the worked example from the spec`() {
        val result = resolve(
            mapOf(
                "anna" to Army.of(WARBOW to 1, HORDE to 2),
                "ben" to Army.of(MAGE to 2, HORDE to 1),
                "cleo" to Army.of(KNIGHT to 1, BOW to 1),
            )
        )

        Assertions.assertEquals(
            listOf(Duel("anna", "ben", 7, 5), Duel("anna", "cleo", 5, 7), Duel("ben", "cleo", 3, 7)),
            result.duels,
        )
        Assertions.assertEquals(mapOf("anna" to 0, "ben" to -6, "cleo" to 6), result.armies.mapValues { it.value.score })
        Assertions.assertEquals("cleo", result.winner)
    }

    @Test
    fun `fielding Horde hands every upgraded opponent its Horde bonus`() {
        val result = resolve(mapOf("anna" to Army.of(HORDE to 3), "ben" to Army.of(BOW to 2)))

        Assertions.assertEquals(listOf(Duel("anna", "ben", 3, 4)), result.duels)
        Assertions.assertEquals("ben", result.winner)
    }

    @Test
    fun `the bonus table is passed in`() {
        val commits = mapOf("anna" to Army.of(SWORD to 2), "ben" to Army.of(BOW to 1, HORDE to 1))
        val none = Bonuses(0, 0, 0, 0, 0, 0)

        Assertions.assertNull(Battle.resolve(commits, none, Random(SEED)).winner)
        Assertions.assertEquals("anna", Battle.resolve(commits, BONUSES, Random(SEED)).winner)
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("winners")
    fun `scores always add up to zero`(commits: Map<String, Army>, expectedWinner: String?, case: String) {
        Assertions.assertEquals(0, resolve(commits).armies.values.sumOf { it.score })
    }

    @Test
    fun `without bonuses the largest army wins by n times its size minus the total`() {
        val result = resolve(mapOf("anna" to Army.of(HORDE to 5), "ben" to Army.of(HORDE to 2), "cleo" to Army.of(HORDE to 2)))

        // strengths 5, 2, 2 (total 9): score = 3 × strength − 9
        Assertions.assertEquals(mapOf("anna" to 6, "ben" to -3, "cleo" to -3), result.armies.mapValues { it.value.score })
        Assertions.assertEquals("anna", result.winner)
    }

    @Test
    fun `resolving a battle gives nobody VP`() {
        val result = resolve(mapOf("anna" to Army.of(BOW to 2), "ben" to Army.of(HORDE to 1), "cleo" to Army()))

        Assertions.assertEquals("anna", result.winner)
        Assertions.assertTrue(result.armies.values.all { it.vpGained == 0 })
    }

    @Test
    fun `a paid battle gives its VP to the winner only`() {
        val result = resolve(mapOf("anna" to Army.of(BOW to 2), "ben" to Army.of(HORDE to 1), "cleo" to Army())).paid(3)

        Assertions.assertEquals(mapOf("anna" to 3, "ben" to 0, "cleo" to 0), result.armies.mapValues { it.value.vpGained })
        Assertions.assertEquals(3, result.pot)
    }

    @Test
    fun `with no winner a paid battle gives nobody VP`() {
        val result = resolve(mapOf("anna" to Army.of(HORDE to 2), "ben" to Army.of(HORDE to 2), "cleo" to Army())).paid(1)

        Assertions.assertNull(result.winner)
        Assertions.assertTrue(result.armies.values.all { it.vpGained == 0 })
        Assertions.assertEquals(1, result.pot)
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
        val result = Battle.resolve(mapOf("anna" to army, "ben" to Army.of(HORDE to 1)), BONUSES, Random(seed))
            .armies.getValue("anna")

        CreatureType.entries.forEach { type ->
            Assertions.assertEquals(army.creatures[type] ?: 0, count(result.casualties, type) + count(result.survivors, type))
        }
    }

    @Test
    fun `casualties are picked by the seeded random`() {
        val commits = mapOf("anna" to Army.of(HORDE to 2, BOW to 2), "ben" to Army.of(HORDE to 1, SWORD to 1, MAGE to 1))
        val outcomes = (0L..20L).map { seed -> Battle.resolve(commits, BONUSES, Random(seed)).armies.mapValues { it.value.casualties } }

        Assertions.assertEquals(outcomes, (0L..20L).map { seed -> Battle.resolve(commits, BONUSES, Random(seed)).armies.mapValues { it.value.casualties } })
        Assertions.assertTrue(outcomes.distinct().size > 1, "different seeds should pick different casualties")
    }

    @Test
    fun `a fixed seed always picks the same casualties`() {
        val commits = mapOf("anna" to Army.of(HORDE to 3, BOW to 2), "ben" to Army.of(HORDE to 1, SWORD to 1, BOW to 1))

        val result = Battle.resolve(commits, BONUSES, Random(42))

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

    private fun resolve(commits: Map<String, Army>) = Battle.resolve(commits, BONUSES, Random(SEED))

    private fun count(army: Army, type: CreatureType) = army.creatures[type] ?: 0

    companion object {
        private const val SEED = 7L
        private val BONUSES = Bonuses(
            tier1VsHorde = 1, tier1VsCountered = 1,
            tier2VsHorde = 4, tier2VsCounteredTier1 = 4, tier2VsOtherTier1 = 2, tier2VsCounteredTier2 = 2,
        )

        @JvmStatic
        fun winners() = listOf(
            Arguments.of(mapOf("anna" to Army.of(HORDE to 3), "ben" to Army.of(HORDE to 2), "cleo" to Army.of(HORDE to 1)), "anna", "highest strength wins"),
            Arguments.of(mapOf("anna" to Army.of(HORDE to 2), "ben" to Army.of(HORDE to 2), "cleo" to Army.of(HORDE to 1)), null, "tie at the top"),
            Arguments.of(mapOf("anna" to Army.of(BOW to 1), "ben" to Army.of(SWORD to 1), "cleo" to Army.of(MAGE to 1)), null, "a perfect triangle is a tie"),
            Arguments.of(mapOf("anna" to Army.of(HORDE to 1), "ben" to Army.of(HORDE to 3), "cleo" to Army.of(HORDE to 1)), "ben", "tie below the top doesn't matter"),
            Arguments.of(mapOf("anna" to Army(), "ben" to Army(), "cleo" to Army()), null, "nobody commits"),
            Arguments.of(mapOf("anna" to Army(), "ben" to Army.of(HORDE to 1), "cleo" to Army()), "ben", "single committer wins"),
            Arguments.of(mapOf("anna" to Army.of(BOW to 2), "ben" to Army.of(MAGE to 1), "cleo" to Army.of(SWORD to 2)), "cleo", "a bonus in one duel doesn't outweigh losing the other"),
            Arguments.of(mapOf("anna" to Army.of(BOW to 3), "ben" to Army.of(MAGE to 1), "cleo" to Army.of(SWORD to 2)), "anna", "a decisive duel margin decides"),
            Arguments.of(mapOf("anna" to Army.of(SWORD to 2), "ben" to Army.of(BOW to 3), "cleo" to Army()), "anna", "a counter beats a bigger army"),
            Arguments.of(mapOf("anna" to Army.of(WARBOW to 1, HORDE to 2), "ben" to Army.of(MAGE to 2, HORDE to 1), "cleo" to Army.of(KNIGHT to 1, BOW to 1)), "cleo", "no Horde, no Horde bonus for the others"),
        )

        @JvmStatic
        fun losses() = listOf(
            Arguments.of(mapOf("anna" to Army.of(HORDE to 5), "ben" to Army.of(HORDE to 3)), "anna", 2, "winner with odd army loses floor"),
            Arguments.of(mapOf("anna" to Army.of(HORDE to 4), "ben" to Army.of(HORDE to 3)), "anna", 2, "winner with even army loses half"),
            Arguments.of(mapOf("anna" to Army.of(HORDE to 1), "ben" to Army()), "anna", 0, "winner with one creature loses none"),
            Arguments.of(mapOf("anna" to Army.of(HORDE to 5), "ben" to Army.of(HORDE to 3)), "ben", 2, "loser with odd army loses ceil"),
            Arguments.of(mapOf("anna" to Army.of(HORDE to 5), "ben" to Army.of(HORDE to 4)), "ben", 2, "loser with even army loses half"),
            Arguments.of(mapOf("anna" to Army.of(HORDE to 3), "ben" to Army.of(HORDE to 1)), "ben", 1, "loser with one creature loses it"),
            Arguments.of(mapOf("anna" to Army.of(HORDE to 3), "ben" to Army.of(HORDE to 3)), "anna", 2, "tie: everyone loses ceil"),
            Arguments.of(mapOf("anna" to Army.of(HORDE to 3), "ben" to Army()), "ben", 0, "empty army loses nothing"),
        )

        @JvmStatic
        fun seeds() = (1L..10L).toList()

        @JvmStatic
        fun playerCounts() = listOf(3, 4, 5)
    }
}
