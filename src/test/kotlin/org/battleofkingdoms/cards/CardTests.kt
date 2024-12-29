package org.battleofkingdoms.cards

import org.battleofkingdoms.cards.creatures.Bow
import org.battleofkingdoms.cards.creatures.Creature
import org.battleofkingdoms.cards.creatures.Horde
import org.battleofkingdoms.cards.creatures.Shield
import org.battleofkingdoms.cards.resources.Food
import org.battleofkingdoms.cards.resources.Iron
import org.battleofkingdoms.cards.resources.Resource
import org.battleofkingdoms.cards.resources.Wood
import org.battleofkingdoms.game.battle.Army
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class CardTests {
    @ParameterizedTest
    @MethodSource("resources")
    fun shouldHaveMandatoryFields(resource: Resource) {
        Assertions.assertNotNull(resource.title());
        Assertions.assertNotNull(resource.value());
    }

    @ParameterizedTest
    @MethodSource("creatures")
    fun shouldHaveMandatoryFields(creature: Creature, opposingArmy: Army, expectedAttack: Int) {
        Assertions.assertNotNull(creature.title());
        Assertions.assertEquals(expectedAttack, creature.attack(opposingArmy));
        Assertions.assertNotNull(creature.cost())
    }

    companion object {
        @JvmStatic
        fun resources() = listOf(
            Arguments.of(Wood()),
            Arguments.of(Iron()),
            Arguments.of(Food())
        )

        @JvmStatic
        fun creatures() = listOf(
            Arguments.of(Horde(), Army(), 1),
            Arguments.of(Shield(), Army(), 1),
            Arguments.of(Shield(), Army.of(Bow()), 2),
            Arguments.of(Bow(), Army(), 2)
        )
    }
}