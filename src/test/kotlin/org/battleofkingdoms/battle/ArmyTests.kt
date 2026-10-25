package org.battleofkingdoms.battle

import org.battleofkingdoms.cards.CreatureType.BOW
import org.battleofkingdoms.cards.CreatureType.HORDE
import org.battleofkingdoms.cards.CreatureType.MAGE
import org.battleofkingdoms.cards.CreatureType.SWORD
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

private const val BONUS = 1

class ArmyTests {
    @ParameterizedTest(name = "{3}")
    @MethodSource("armies")
    fun `duel strength is one per creature plus the bonus per countering creature`(
        army: Army,
        opponent: Army,
        expectedStrength: Int,
        case: String,
    ) {
        Assertions.assertEquals(expectedStrength, army.duelStrength(opponent, BONUS))
    }

    @Test
    fun `the bonus size comes from the caller`() {
        Assertions.assertEquals(2 + 2 * 3, Army.of(SWORD to 2).duelStrength(Army.of(BOW to 1), counterBonus = 3))
    }

    companion object {
        @JvmStatic
        fun armies() = listOf(
            Arguments.of(Army(), Army.of(HORDE to 1), 0, "empty army"),
            Arguments.of(Army.of(HORDE to 3), Army.of(BOW to 1, SWORD to 1, MAGE to 1), 3, "horde counters nothing"),
            Arguments.of(Army.of(BOW to 2), Army.of(MAGE to 1), 4, "bow counters mage"),
            Arguments.of(Army.of(SWORD to 2), Army.of(BOW to 1), 4, "sword counters bow"),
            Arguments.of(Army.of(MAGE to 2), Army.of(SWORD to 1), 4, "mage counters sword"),
            Arguments.of(Army.of(BOW to 2), Army.of(SWORD to 1, HORDE to 2), 2, "bow gets nothing against sword"),
            Arguments.of(Army.of(SWORD to 3), Army.of(BOW to 1), 6, "one countered creature boosts every countering one"),
            Arguments.of(Army.of(SWORD to 1), Army.of(BOW to 3), 2, "many countered creatures give one bonus per countering one"),
            Arguments.of(Army.of(SWORD to 1, BOW to 1), Army.of(HORDE to 1), 2, "own bow gives own sword no bonus"),
            Arguments.of(Army.of(BOW to 1, SWORD to 1, MAGE to 1), Army.of(BOW to 1, SWORD to 1, MAGE to 1), 6, "mirror armies all get bonuses"),
            Arguments.of(Army.of(MAGE to 1), Army(), 1, "empty opponent"),
        )
    }
}
