package org.battleofkingdoms.battle

import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.cards.CreatureType.BOW
import org.battleofkingdoms.cards.CreatureType.HIGH_MAGE
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

/** The spec's default bonus table: tier 1 +1 / +1; tier 2 +4 vs Horde, +4 vs the countered tier 1, +2 vs other tier 1, +2 vs the countered tier 2. */
private val DEFAULT = Bonuses(
    tier1VsHorde = 1, tier1VsCountered = 1,
    tier2VsHorde = 4, tier2VsCounteredTier1 = 4, tier2VsOtherTier1 = 2, tier2VsCounteredTier2 = 2,
)

class ArmyTests {
    @ParameterizedTest(name = "{0} vs {1}: +{2}")
    @MethodSource("bonusTable")
    fun `every cell of the bonus table`(attacker: CreatureType, target: CreatureType, expected: Int) {
        Assertions.assertEquals(expected, DEFAULT.against(attacker, target))
    }

    @ParameterizedTest(name = "{0} vs {1}: nothing")
    @MethodSource("noBonus")
    fun `matchups outside the table give nothing`(attacker: CreatureType, target: CreatureType) {
        Assertions.assertEquals(0, DEFAULT.against(attacker, target))
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("armies")
    fun `duel strength is one per creature plus its single best bonus`(
        army: Army,
        opponent: Army,
        expectedStrength: Int,
        case: String,
    ) {
        Assertions.assertEquals(expectedStrength, army.duelStrength(opponent, DEFAULT))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tier2Units")
    fun `three cards as one tier-2 unit beat a tier-1 unit and a Horde, which only tie 3 Horde`(tier2: CreatureType) {
        val opponent = Army.of(HORDE to 3)
        val strength = { army: Army -> army.duelStrength(opponent, DEFAULT) }

        Assertions.assertEquals(5, strength(Army.of(tier2 to 1)))
        Assertions.assertEquals(3, strength(Army.of(tier2.upgradesFrom!! to 1, HORDE to 1)))
        Assertions.assertEquals(3, strength(Army.of(HORDE to 3)))
    }

    @Test
    fun `bonus values come from the table passed in`() {
        val custom = Bonuses(
            tier1VsHorde = 5, tier1VsCountered = 0,
            tier2VsHorde = 7, tier2VsCounteredTier1 = 4, tier2VsOtherTier1 = 3, tier2VsCounteredTier2 = 6,
        )

        Assertions.assertEquals(6, Army.of(BOW to 1).duelStrength(Army.of(HORDE to 1), custom))
        Assertions.assertEquals(1, Army.of(BOW to 1).duelStrength(Army.of(MAGE to 1), custom))
        Assertions.assertEquals(8, Army.of(WARBOW to 1).duelStrength(Army.of(HORDE to 1), custom))
        Assertions.assertEquals(5, Army.of(WARBOW to 1).duelStrength(Army.of(MAGE to 1), custom))
        Assertions.assertEquals(4, Army.of(WARBOW to 1).duelStrength(Army.of(SWORD to 1), custom))
        Assertions.assertEquals(7, Army.of(WARBOW to 1).duelStrength(Army.of(HIGH_MAGE to 1), custom))
    }

    companion object {
        @JvmStatic
        fun tier2Units() = CreatureType.entries.filter { it.tier == 2 }

        @JvmStatic
        fun bonusTable() = listOf(
            // Bow line counters the Mage line
            Arguments.of(BOW, HORDE, 1), Arguments.of(BOW, MAGE, 1), Arguments.of(BOW, HIGH_MAGE, 0),
            Arguments.of(WARBOW, HORDE, 4), Arguments.of(WARBOW, MAGE, 4), Arguments.of(WARBOW, HIGH_MAGE, 2),
            Arguments.of(WARBOW, BOW, 2), Arguments.of(WARBOW, SWORD, 2),
            // Sword line counters the Bow line
            Arguments.of(SWORD, HORDE, 1), Arguments.of(SWORD, BOW, 1), Arguments.of(SWORD, WARBOW, 0),
            Arguments.of(KNIGHT, HORDE, 4), Arguments.of(KNIGHT, BOW, 4), Arguments.of(KNIGHT, WARBOW, 2),
            Arguments.of(KNIGHT, SWORD, 2), Arguments.of(KNIGHT, MAGE, 2),
            // Mage line counters the Sword line
            Arguments.of(MAGE, HORDE, 1), Arguments.of(MAGE, SWORD, 1), Arguments.of(MAGE, KNIGHT, 0),
            Arguments.of(HIGH_MAGE, HORDE, 4), Arguments.of(HIGH_MAGE, SWORD, 4), Arguments.of(HIGH_MAGE, KNIGHT, 2),
            Arguments.of(HIGH_MAGE, MAGE, 2), Arguments.of(HIGH_MAGE, BOW, 2),
        )

        @JvmStatic
        fun noBonus() = listOf(
            Arguments.of(HORDE, HORDE), Arguments.of(HORDE, BOW), Arguments.of(HORDE, KNIGHT),
            Arguments.of(BOW, SWORD), Arguments.of(BOW, BOW), Arguments.of(BOW, WARBOW), Arguments.of(BOW, KNIGHT),
            Arguments.of(WARBOW, KNIGHT), Arguments.of(WARBOW, WARBOW),
            Arguments.of(SWORD, MAGE), Arguments.of(KNIGHT, HIGH_MAGE), Arguments.of(MAGE, BOW), Arguments.of(HIGH_MAGE, WARBOW),
        )

        @JvmStatic
        fun armies() = listOf(
            Arguments.of(Army(), Army.of(HORDE to 1), 0, "empty army"),
            Arguments.of(Army.of(HORDE to 3), Army.of(BOW to 1, KNIGHT to 1), 3, "horde gets no bonus"),
            Arguments.of(Army.of(MAGE to 1), Army(), 1, "empty opponent gives no bonus"),
            Arguments.of(Army.of(BOW to 2), Army.of(HORDE to 1), 4, "tier 1 against Horde"),
            Arguments.of(Army.of(WARBOW to 1), Army.of(HORDE to 5), 5, "tier 2 against Horde, one bonus however many"),
            Arguments.of(Army.of(BOW to 1), Army.of(HORDE to 1, MAGE to 1), 2, "best bonus, not +1 and +1"),
            Arguments.of(Army.of(WARBOW to 1), Army.of(MAGE to 1, HIGH_MAGE to 1), 5, "best of +4 and +2"),
            Arguments.of(Army.of(WARBOW to 1), Army.of(HORDE to 1, MAGE to 1, HIGH_MAGE to 1), 5, "best of three, never the sum"),
            Arguments.of(Army.of(WARBOW to 1), Army.of(SWORD to 2), 3, "tier 2 against another line's tier 1"),
            Arguments.of(Army.of(MAGE to 2), Army.of(KNIGHT to 1), 2, "lower tier gets nothing against higher"),
            Arguments.of(Army.of(SWORD to 1, HORDE to 1), Army.of(BOW to 1), 3, "each creature picks its own best bonus"),
            Arguments.of(Army.of(KNIGHT to 1, BOW to 1), Army.of(MAGE to 2, HORDE to 1), 7, "worked example: cleo against ben"),
        )
    }
}
