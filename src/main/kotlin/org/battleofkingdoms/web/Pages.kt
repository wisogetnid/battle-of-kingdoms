package org.battleofkingdoms.web

import io.ktor.http.encodeURLPathPart
import io.ktor.http.encodeURLQueryComponent
import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.HTML
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.checkBoxInput
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.head
import kotlinx.html.hiddenInput
import kotlinx.html.label
import kotlinx.html.li
import kotlinx.html.numberInput
import kotlinx.html.p
import kotlinx.html.submitInput
import kotlinx.html.textInput
import kotlinx.html.title
import kotlinx.html.ul
import org.battleofkingdoms.cards.Artifact
import org.battleofkingdoms.cards.Artifact.AMULET_OF_LUCK
import org.battleofkingdoms.cards.Artifact.CROWN
import org.battleofkingdoms.cards.Artifact.RESILIENT_BANNER
import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.cards.CreatureType.HORDE
import org.battleofkingdoms.cards.ResourceType
import org.battleofkingdoms.game.Action.CLAIM as CLAIM_PRIZE
import org.battleofkingdoms.game.Action.COMMIT_ARMY
import org.battleofkingdoms.game.Action.READY
import org.battleofkingdoms.game.Action.TRADE
import org.battleofkingdoms.game.Action.UPGRADE
import org.battleofkingdoms.game.Phase
import org.battleofkingdoms.game.Phase.BATTLE
import org.battleofkingdoms.game.Phase.BUILD_UP
import org.battleofkingdoms.game.Phase.CLAIM
import org.battleofkingdoms.game.Phase.GAME_OVER
import org.battleofkingdoms.game.Phase.WAITING_FOR_PLAYERS
import org.battleofkingdoms.game.Take
import org.battleofkingdoms.server.PlayerView

/** `HIGH_MAGE` → "High Mage", `WOOD` → "Wood". */
fun displayName(constant: String): String =
    constant.split('_').joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }

val Enum<*>.displayName: String get() = displayName(name)

fun gamePath(gameId: String): String = "/ui/games/${gameId.encodeURLPathPart()}"

fun playerPath(gameId: String, name: String): String = "${gamePath(gameId)}/players/${name.encodeURLPathPart()}"

fun invitePath(gameId: String): String = "/?game=${gameId.encodeURLQueryComponent()}"

/** Create a game, or join one: [gameId] comes from an invite link `/?game={id}`. */
fun HTML.homePage(gameId: String? = null, error: String? = null) {
    head { title("Battle of Kingdoms") }
    body {
        h1 { +"Battle of Kingdoms" }
        error(error)
        createGameForm()
        joinGameForm(gameId)
    }
}

fun HTML.notFoundPage(message: String) {
    head { title("Not found – Battle of Kingdoms") }
    body {
        h1 { +"Not found" }
        p { +message }
        p { a(href = "/") { +"Home" } }
    }
}

/** Everything here comes from [view], so the page shows exactly what the JSON player view shows. */
fun HTML.gamePage(view: PlayerView, error: String? = null) {
    val gameId = view.gameId.toString()
    head { title("${view.me.name} – Battle of Kingdoms") }
    body {
        h1 { +"Battle of Kingdoms" }
        p {
            +"Game $gameId. You are ${view.me.name}. "
            a(href = playerPath(gameId, view.me.name)) { +"Refresh" }
            +" "
            a(href = "/") { +"Home" }
        }
        error(error)
        status(view)
        if (view.phase == WAITING_FOR_PLAYERS) invite(view)
        me(view)
        actions(view)
        others(view)
        battleLog(view)
    }
}

private fun FlowContent.error(error: String?) {
    if (error != null) p { +"Error: $error" }
}

private fun FlowContent.status(view: PlayerView) {
    h2 { +"Status" }
    ul {
        li { +"Phase: ${view.phase.displayName}" }
        li { +"Turn: ${view.turn}" }
        li { +"Pot: ${view.pot} VP" }
        li { +"VP to win: ${view.victoryPointsToWin}" }
        if (playsWithArtifacts(view)) li { +"Artifacts left in the stack: ${view.artifactsInStack}" }
        if (view.winner != null) li { +"Winner: ${view.winner}" }
    }
}

private fun FlowContent.invite(view: PlayerView) {
    val gameId = view.gameId.toString()
    p {
        +"Share this invite link: "
        a(href = invitePath(gameId)) { +invitePath(gameId) }
    }
    p { +"Joined so far: ${(listOf(view.me.name) + view.others.map { it.name }).joinToString(", ")}" }
}

private fun FlowContent.me(view: PlayerView) {
    h2 { +"You" }
    ul {
        li {
            +"Hand:"
            hand(view.me.hand)
        }
        if (view.me.upgradedThisTurn.isNotEmpty()) {
            li { +"Upgraded this turn: ${counts(view.me.upgradedThisTurn.mapKeys { it.key.name })}" }
        }
        li { +"VP: ${view.me.vp}" }
        if (playsWithArtifacts(view)) li { +"Artifacts: ${artifacts(view.me.artifacts)}" }
        actedLabel(view.phase, view.me.acted)?.let { li { +"You are $it" } }
    }
}

/** Tier 2, tier 1 (each only if I hold any), Horde, then resources: easier to scan than one line. */
private fun FlowContent.hand(hand: Map<String, Int>) = ul {
    val held = hand.filterValues { it > 0 }
    listOf(2, 1).forEach { tier ->
        val units = held.filterKeys { name -> CreatureType.entries.any { it.name == name && it.tier == tier } }
        if (units.isNotEmpty()) li { +"Tier $tier: ${counts(units)}" }
    }
    li { +"Horde: ${held[HORDE.name] ?: 0}" }
    li { +"Resources: ${counts(held.filterKeys { name -> ResourceType.entries.any { it.name == name } }, empty = "none")}" }
}

/** One form per action I can take right now, as listed in [PlayerView.availableActions]. */
private fun FlowContent.actions(view: PlayerView) {
    val path = playerPath(view.gameId.toString(), view.me.name)
    h2 { +"Actions" }
    if (view.availableActions.isEmpty()) {
        p { +waitingFor(view) }
        return
    }
    if (UPGRADE in view.availableActions) {
        if (view.affordableUpgrades.isEmpty()) p { +"No upgrade is affordable right now." }
        view.affordableUpgrades.forEach { into ->
            val label = "Upgrade ${into.upgradesFrom!!.displayName} into ${into.displayName} (1 ${into.upgradeResource!!.displayName})"
            actionButton("$path/upgrades", label, "into" to into.name)
        }
    }
    if (TRADE in view.availableActions) {
        ResourceType.entries.filter { (view.me.hand[it.name] ?: 0) > 0 }.forEach { resource ->
            actionButton("$path/trades", "Trade 1 ${resource.displayName} for 1 Horde", "resource" to resource.name)
        }
    }
    if (READY in view.availableActions) actionButton("$path/ready", "Ready")
    if (COMMIT_ARMY in view.availableActions) armyForm(view, "$path/army")
    if (CLAIM_PRIZE in view.availableActions) claimForm(view, "$path/claim")
}

/** Only the winner sees the drawn artifact; turning it down shuffles it back, unseen by the others. */
private fun FlowContent.claimForm(view: PlayerView, action: String) {
    val drawn = view.me.drawnArtifact ?: return
    p { +"You won the battle and drew the ${drawn.displayName}: ${effect(drawn)}. Take it, or take ${view.pot} VP and shuffle it back." }
    actionButton(action, "Take the ${drawn.displayName}", "take" to Take.ARTIFACT.name)
    actionButton(action, "Take ${view.pot} VP", "take" to Take.VICTORY_POINT.name)
}

private fun effect(artifact: Artifact): String = when (artifact) {
    RESILIENT_BANNER -> "your casualties always round down"
    AMULET_OF_LUCK -> "your first casualty in every battle survives"
    CROWN -> "extra Horde every turn"
}

/** A game without artifacts never mentions them. */
private fun playsWithArtifacts(view: PlayerView): Boolean =
    view.artifactsInStack > 0 || view.me.artifacts.isNotEmpty() || view.others.any { it.artifacts.isNotEmpty() }

private fun artifacts(owned: List<Artifact>): String = owned.joinToString(", ") { it.displayName }.ifEmpty { "none" }

/** A count per creature I hold; all zero commits nothing. */
private fun FlowContent.armyForm(view: PlayerView, action: String) {
    val creatures = view.me.hand.filterKeys { name -> CreatureType.entries.any { it.name == name } }
        .filterValues { it > 0 }.toSortedMap(handOrder)
    form(action = action, method = FormMethod.post) {
        if (creatures.isEmpty()) p { +"You have no creatures, so you can only commit an empty army." }
        creatures.forEach { (name, count) ->
            p {
                label {
                    +"${displayName(name)} (you have $count): "
                    numberInput(name = name) { min = "0"; max = "$count"; value = "0" }
                }
            }
        }
        submitInput { value = "Commit army" }
    }
}

private fun FlowContent.actionButton(action: String, label: String, field: Pair<String, String>? = null) =
    form(action = action, method = FormMethod.post) {
        field?.let { (name, value) -> hiddenInput(name = name) { this.value = value } }
        submitInput { value = label }
    }

private fun waitingFor(view: PlayerView): String = when (view.phase) {
    WAITING_FOR_PLAYERS -> "Waiting for more players to join. Refresh to check."
    BUILD_UP -> "You are ready. Waiting for the others to finish building up. Refresh to check."
    BATTLE -> "Army committed. Waiting for the others to commit. Refresh to check."
    CLAIM -> "Waiting for the battle's winner to take a prize. Refresh to check."
    GAME_OVER -> "The game is over. ${view.winner} won."
}

private fun FlowContent.others(view: PlayerView) {
    h2 { +"Other players" }
    ul {
        view.others.forEach {
            val acted = actedLabel(view.phase, it.acted)?.let { label -> ", $label" }.orEmpty()
            val owned = if (it.artifacts.isEmpty()) "" else ", ${artifacts(it.artifacts)}"
            li { +"${it.name}: ${it.vp} VP, ${it.handSize} cards$owned$acted" }
        }
    }
}

/** Newest battle first, numbered by its position in the log (one battle per turn). */
private fun FlowContent.battleLog(view: PlayerView) {
    h2 { +"Battle log" }
    if (view.battleLog.isEmpty()) p { +"No battles yet." }
    view.battleLog.withIndex().reversed().forEach { (index, battle) ->
        h3 { +"Battle ${index + 1}" }
        p {
            +when {
                battle.winner == null -> "No winner, nobody scores."
                battle.prize == null -> "${battle.winner} won and is choosing a prize."
                battle.prize == "VICTORY_POINT" -> "${battle.winner} won ${battle.pot} VP."
                else -> "${battle.winner} won the ${displayName(battle.prize)}."
            }
        }
        ul {
            battle.armies.forEach { (name, army) ->
                val line = when {
                    army.army.isEmpty() -> "$name sat out"
                    else -> "$name: committed ${creatures(army.army)}; score ${signed(army.score)}; " +
                        "lost ${creatures(army.casualties)}; gained ${army.vpGained} VP"
                }
                li { +line }
            }
        }
        p { +"Duels:" }
        ul {
            battle.duels.forEach { duel ->
                val (first, second) = duel.players
                li { +"$first ${duel.strengths[first]} – ${duel.strengths[second]} $second" }
            }
        }
    }
}

private fun creatures(counts: Map<CreatureType, Int>): String = counts(counts.mapKeys { it.key.name })

private fun signed(n: Int): String = if (n > 0) "+$n" else "$n"

private fun actedLabel(phase: Phase, acted: Boolean): String? = when (phase) {
    BUILD_UP -> if (acted) "ready" else "still building up"
    BATTLE -> if (acted) "committed" else "still choosing an army"
    WAITING_FOR_PLAYERS, CLAIM, GAME_OVER -> null
}

/** `{WOOD=1, HORDE=3}` → "3 Horde, 1 Wood", in [handOrder]. */
private fun counts(counts: Map<String, Int>, empty: String = "nothing"): String =
    counts.filterValues { it > 0 }.toSortedMap(handOrder).entries
        .joinToString(", ") { (name, count) -> "$count ${displayName(name)}" }
        .ifEmpty { empty }

/** Horde first, then the other creatures in [CreatureType] order, then resources alphabetically. */
private val handOrder: Comparator<String> = compareBy(
    { name -> CreatureType.entries.indexOfFirst { it.name == name }.takeIf { it >= 0 } ?: CreatureType.entries.size },
    { it },
)

private fun FlowContent.createGameForm() {
    h2 { +"Create a game" }
    form(action = "/ui/games", method = FormMethod.post) {
        p { label { +"Players (3–5): "; numberInput(name = "players") { min = "3"; max = "5"; value = "3" } } }
        p { label { +"Your name: "; textInput(name = "name") } }
        p { label { +"Seed (optional): "; numberInput(name = "seed") } }
        p { label { checkBoxInput(name = "artifacts") { checked = true }; +" Play with artifacts" } }
        submitInput { value = "Create game" }
    }
}

/** Without a game id, a GET form opens the invite page for an id typed in by hand. */
private fun FlowContent.joinGameForm(gameId: String?) {
    h2 { +"Join a game" }
    if (gameId.isNullOrBlank()) {
        form(action = "/", method = FormMethod.get) {
            p { label { +"Game id: "; textInput(name = "game") } }
            submitInput { value = "Open game" }
        }
    } else {
        p { +"Game $gameId" }
        form(action = "${gamePath(gameId)}/players", method = FormMethod.post) {
            p { label { +"Your name: "; textInput(name = "name") } }
            submitInput { value = "Join game" }
        }
    }
}
