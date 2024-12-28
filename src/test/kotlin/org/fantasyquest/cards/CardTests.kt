package org.fantasyquest.cards

import org.fantasyquest.cards.creatures.Bow
import org.fantasyquest.cards.creatures.Creature
import org.fantasyquest.cards.creatures.Horde
import org.fantasyquest.cards.creatures.Shield
import org.fantasyquest.cards.resources.Food
import org.fantasyquest.cards.resources.Iron
import org.fantasyquest.cards.resources.Resource
import org.fantasyquest.cards.resources.Wood
import org.fantasyquest.game.battle.Army
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
    fun shouldHaveMandatoryFields(creature: Creature, opposingArmyCreatures: Map<Creature, Int>, expectedAttack: Int) {
        Assertions.assertNotNull(creature.title());
        Assertions.assertEquals(expectedAttack, creature.attack(Army(opposingArmyCreatures)));
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
            Arguments.of(Horde(), noEnemy(), 1),
            Arguments.of(Shield(), noEnemy(), 1),
            Arguments.of(Shield(), mapOf(Bow() to 1), 2),
            Arguments.of(Bow(), noEnemy(), 2)
        )

        private fun noEnemy() = emptyMap<Creature, Int>()
    }
}