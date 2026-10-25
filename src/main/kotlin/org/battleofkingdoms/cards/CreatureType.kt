package org.battleofkingdoms.cards

import org.battleofkingdoms.cards.ResourceType.ESSENCE
import org.battleofkingdoms.cards.ResourceType.IRON
import org.battleofkingdoms.cards.ResourceType.WOOD

/**
 * Every creature has attack 1. The Horde (tier 0) comes as income and belongs to no line;
 * each [Line] has a tier-1 unit, upgraded from a Horde, and a tier-2 unit, upgraded from the
 * line's tier-1 unit. Both upgrades cost one [upgradeResource] of the line.
 */
enum class CreatureType(val tier: Int, val line: Line?, val upgradeResource: ResourceType?) : Card {
    HORDE(tier = 0, line = null, upgradeResource = null),
    BOW(tier = 1, line = Line.BOW, upgradeResource = WOOD),
    SWORD(tier = 1, line = Line.SWORD, upgradeResource = IRON),
    MAGE(tier = 1, line = Line.MAGE, upgradeResource = ESSENCE),
    WARBOW(tier = 2, line = Line.BOW, upgradeResource = WOOD),
    KNIGHT(tier = 2, line = Line.SWORD, upgradeResource = IRON),
    HIGH_MAGE(tier = 2, line = Line.MAGE, upgradeResource = ESSENCE);

    val attack: Int get() = 1

    /** The unit an upgrade into this one consumes: the Horde for tier 1, the line's tier-1 unit for tier 2. */
    val upgradesFrom: CreatureType?
        get() = when (tier) {
            0 -> null
            1 -> HORDE
            else -> entries.single { it.line == line && it.tier == tier - 1 }
        }
}
