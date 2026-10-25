package org.battleofkingdoms.web

import kotlinx.html.FlowContent
import kotlinx.html.HTML
import kotlinx.html.TABLE
import kotlinx.html.a
import kotlinx.html.b
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.head
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.table
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.title
import kotlinx.html.tr
import kotlinx.html.ul
import org.battleofkingdoms.game.GameConfig

const val RULES_PATH = "/rules"

/** The rules for players, the only full copy; every number comes from [config], so the page matches the game. */
fun HTML.rulesPage(config: GameConfig) {
    head { title("Rules – Battle of Kingdoms") }
    body {
        h1 { +"Battle of Kingdoms: How to Play" }
        p { a(href = "/") { +"Home" } }
        p {
            +("${config.minPlayers}–${config.maxPlayers} players lead small tribes. Every turn you build up an army, then " +
                "secretly decide how much of it to send into one big battle. Win a battle to earn a victory point (VP) " +
                "or an artifact. ")
            b { +"The first player to ${config.victoryPointsToWin} VP wins." }
        }

        h2 { +"A turn" }
        p { +"Every turn has two phases. Everyone acts at the same time." }
        buildUp(config)
        battle()
        bonuses(config)
        prize(config)
        tips(config)
    }
}

private fun FlowContent.buildUp(config: GameConfig) {
    h3 { +"1. Build-up" }
    p {
        +("You receive ${config.baseCreaturesPerTurn} Horde and draw ${config.resourcesPerTurn} resources at random " +
            "(Wood, Iron or Essence). Everyone gets the same, and you start the game with nothing else. Then do any " +
            "of the following, as often as you can afford, and mark yourself ready:")
    }
    ul {
        li { +"Trade any resource for 1 Horde." }
        li { +"Upgrade to tier 1: Horde + Wood → Bow, Horde + Iron → Sword, Horde + Essence → Mage." }
        li { +"Upgrade to tier 2: Bow + Wood → Warbow, Sword + Iron → Knight, Mage + Essence → High Mage." }
    }
    p { +"A unit climbs only one tier per turn: a Bow you build this turn can become a Warbow next turn." }
}

private fun FlowContent.battle() {
    h3 { +"2. Battle" }
    p {
        +("Everyone secretly commits any part of their creatures, or nothing. Then all armies are revealed, and " +
            "every pair of players who committed something fights a duel.")
    }
    ul {
        li {
            +("Duel strength: every creature attacks for 1, plus its single best bonus against that opponent's " +
                "army (table below). Bonuses never add up.")
        }
        li { +"Score: add up your margins over all your duels. Winning one duel 5:2 and losing another 3:4 scores +3 − 1 = +2." }
        li {
            +("Winner: the single highest score. A tie at the top, or nobody committing, means nobody wins and " +
                "nobody scores. A player who commits alone wins without a fight.")
        }
        li { +"Sitting out: if you commit nothing, you fight no duels, score 0 and lose nothing." }
        li {
            +("Casualties: everyone who fought loses half of what they committed, picked at random. The winner " +
                "rounds down, everyone else rounds up (3 creatures: the winner loses 1, the others lose 2). " +
                "Survivors go back to your hand.")
        }
    }
}

private fun FlowContent.bonuses(config: GameConfig) {
    h2 { +"Bonuses" }
    p { +"The lines counter each other in a triangle: Bow beats Mage, Sword beats Bow, Mage beats Sword." }
    table {
        tr {
            listOf("Attacker", "vs Horde", "vs the tier-1 unit it counters", "vs any other tier-1 unit", "vs the tier-2 unit it counters")
                .forEach { th { +it } }
        }
        bonusRow("Horde", null, null, null, null)
        bonusRow("Tier 1 (Bow, Sword, Mage)", config.tier1VsHorde, config.tier1VsCountered, null, null)
        bonusRow(
            "Tier 2 (Warbow, Knight, High Mage)",
            config.tier2VsHorde, config.tier2VsCounteredTier1, config.tier2VsOtherTier1, config.tier2VsCounteredTier2,
        )
    }
    val best = maxOf(config.tier2VsHorde, config.tier2VsCounteredTier1)
    p {
        +("Example: a Warbow facing an army with a Bow and a Horde takes its best bonus, +$best, so it attacks for " +
            "${1 + best}. Against an army of only Swords it gets +${config.tier2VsOtherTier1} and attacks for " +
            "${1 + config.tier2VsOtherTier1}. Any matchup not in the table gives nothing.")
    }
}

private fun TABLE.bonusRow(attacker: String, vararg bonuses: Int?) = tr {
    td { +attacker }
    bonuses.forEach { td { +(it?.let { "+$it" } ?: "—") } }
}

private fun FlowContent.prize(config: GameConfig) {
    val vp = config.victoryPointsPerBattle
    h2 { +"The prize: a VP or an artifact" }
    p {
        +"The battle's winner turns over the top card of the artifact stack. Only the winner sees it. They take either:"
    }
    ul {
        li { +"the artifact: it's theirs for the rest of the game and everyone sees it, but they get no VP for this battle; or" }
        li { +"$vp VP: the artifact is shuffled back into the stack. Only the winner knows which one it was." }
    }
    p {
        +("Once the stack is empty, a winner simply takes the VP. A game can also be played without artifacts; " +
            "then every win is worth $vp VP.")
    }
    table {
        tr { th { +"Artifact" }; th { +"Effect for its owner" } }
        artifactRow("Resilient Banner", "Your casualties always round down, win or lose.")
        artifactRow("Horn of Frenzy", "Every Horde in your army gets +${config.frenzyBonus} in every duel.")
        artifactRow("Famous Crown", "${config.crownHorde} extra Horde in every turn's income.")
        artifactRow("Transmuter Stone", "Once per turn during build-up, swap one resource for a random one of the other two kinds.")
    }
    p { +"Artifacts start working from the turn after you take them." }
}

private fun TABLE.artifactRow(name: String, effect: String) = tr {
    td { +name }
    td { +effect }
}

private fun FlowContent.tips(config: GameConfig) {
    h2 { +"Tips" }
    ul {
        li { +"Every Horde you commit gives each upgraded opponent its Horde bonus, but Horde are also your cheapest casualties." }
        li {
            +("A tier-2 unit costs three cards over two turns and hits hard: against 3 Horde it attacks for " +
                "${1 + config.tier2VsHorde}, while a tier-1 unit plus a Horde attacks for ${2 + config.tier1VsHorde}.")
        }
        li { +"You see every army after each battle. Remember what each opponent likes to field." }
        li { +"Sitting a battle out keeps your army safe for a bigger one later." }
    }
}
