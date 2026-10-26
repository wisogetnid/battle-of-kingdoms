package org.battleofkingdoms.simulator

import org.battleofkingdoms.battle.Army
import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.cards.ResourceType
import org.battleofkingdoms.server.PlayerView

// The view's hand is keyed by card name; these read it as typed counts.

fun PlayerView.resources(type: ResourceType): Int = me.hand[type.name] ?: 0

fun PlayerView.count(type: CreatureType): Int = me.hand[type.name] ?: 0

/** Every creature in the hand. */
fun PlayerView.army(): Army = Army(CreatureType.entries.associateWith { count(it) }.filterValues { it > 0 })
