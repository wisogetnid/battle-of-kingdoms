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

/** The spec's default bonus table: tier 1 +2 vs Horde, tier 2 +3 vs Horde, +1 same tier, +2 one tier down. */
private val DEFAULT = Bonuses(tier1VsHorde = 2, tier2VsHorde = 3, sameTierCounter = 1, higherTierCounter = 2)

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

    @Test
    fun `bonus values come from the table passed in`() {
        val custom = Bonuses(tier1VsHorde = 5, tier2VsHorde = 7, sameTierCounter = 0, higherTierCounter = 4)

        Assertions.assertEquals(6, Army.of(BOW to 1).duelStrength(Army.of(HORDE to 1), custom))
        Assertions.assertEquals(8, Army.of(WARBOW to 1).duelStrength(Army.of(HORDE to 1), custom))
        Assertions.assertEquals(1, Army.of(BOW to 1).duelStrength(Army.of(MAGE to 1), custom))
        Assertions.assertEquals(5, Army.of(WARBOW to 1).duelStrength(Army.of(MAGE to 1), custom))
    }

    companion object {
        @JvmStatic
        fun bonusTable() = listOf(
            // Bow line counters the Mage line
            Arguments.of(BOW, HORDE, 2), Arguments.of(BOW, MAGE, 1), Arguments.of(BOW, HIGH_MAGE, 0),
            Arguments.of(WARBOW, HORDE, 3), Arguments.of(WARBOW, MAGE, 2), Arguments.of(WARBOW, HIGH_MAGE, 1),
            // Sword line counters the Bow line
            Arguments.of(SWORD, HORDE, 2), Arguments.of(SWORD, BOW, 1), Arguments.of(SWORD, WARBOW, 0),
            Arguments.of(KNIGHT, HORDE, 3), Arguments.of(KNIGHT, BOW, 2), Arguments.of(KNIGHT, WARBOW, 1),
            // Mage line counters the Sword line
            Arguments.of(MAGE, HORDE, 2), Arguments.of(MAGE, SWORD, 1), Arguments.of(MAGE, KNIGHT, 0),
            Arguments.of(HIGH_MAGE, HORDE, 3), Arguments.of(HIGH_MAGE, SWORD, 2), Arguments.of(HIGH_MAGE, KNIGHT, 1),
        )

        @JvmStatic
        fun noBonus() = listOf(
            Arguments.of(HORDE, HORDE), Arguments.of(HORDE, BOW), Arguments.of(HORDE, KNIGHT),
            Arguments.of(BOW, SWORD), Arguments.of(BOW, BOW), Arguments.of(BOW, WARBOW), Arguments.of(BOW, KNIGHT),
            Arguments.of(WARBOW, SWORD), Arguments.of(WARBOW, BOW), Arguments.of(WARBOW, KNIGHT),
            Arguments.of(SWORD, MAGE), Arguments.of(KNIGHT, HIGH_MAGE), Arguments.of(MAGE, BOW), Arguments.of(HIGH_MAGE, WARBOW),
        )

        @JvmStatic
        fun armies() = listOf(
            Arguments.of(Army(), Army.of(HORDE to 1), 0, "empty army"),
            Arguments.of(Army.of(HORDE to 3), Army.of(BOW to 1, KNIGHT to 1), 3, "horde gets no bonus"),
            Arguments.of(Army.of(MAGE to 1), Army(), 1, "empty opponent gives no bonus"),
            Arguments.of(Army.of(BOW to 2), Army.of(HORDE to 1), 6, "tier 1 against Horde"),
            Arguments.of(Army.of(WARBOW to 1), Army.of(HORDE to 5), 4, "tier 2 against Horde, one bonus however many"),
            Arguments.of(Army.of(BOW to 1), Army.of(HORDE to 1, MAGE to 1), 3, "best bonus, not +2 and +1"),
            Arguments.of(Army.of(WARBOW to 1), Army.of(MAGE to 1, HIGH_MAGE to 1), 3, "best of +2 and +1"),
            Arguments.of(Army.of(WARBOW to 1), Army.of(HORDE to 1, MAGE to 1, HIGH_MAGE to 1), 4, "best of three, never the sum"),
            Arguments.of(Army.of(MAGE to 2), Army.of(KNIGHT to 1), 2, "lower tier gets nothing against higher"),
            Arguments.of(Army.of(SWORD to 1, HORDE to 1), Army.of(BOW to 1), 3, "each creature picks its own best bonus"),
            Arguments.of(Army.of(KNIGHT to 1, BOW to 1), Army.of(MAGE to 2, HORDE to 1), 7, "worked example: cleo against ben"),
        )
    }
}
