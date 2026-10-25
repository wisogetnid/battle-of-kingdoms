package org.battleofkingdoms.web

import io.ktor.http.encodeURLPathPart
import io.ktor.http.encodeURLQueryComponent
import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.HTML
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.label
import kotlinx.html.li
import kotlinx.html.numberInput
import kotlinx.html.p
import kotlinx.html.submitInput
import kotlinx.html.textInput
import kotlinx.html.title
import kotlinx.html.ul
import org.battleofkingdoms.cards.CreatureType
import org.battleofkingdoms.game.Phase
import org.battleofkingdoms.game.Phase.BATTLE
import org.battleofkingdoms.game.Phase.BUILD_UP
import org.battleofkingdoms.game.Phase.GAME_OVER
import org.battleofkingdoms.game.Phase.WAITING_FOR_PLAYERS
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
        others(view)
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
        li { +"Hand: ${counts(view.me.hand)}" }
        li { +"VP: ${view.me.vp}" }
        actedLabel(view.phase, view.me.acted)?.let { li { +"You are $it" } }
    }
}

private fun FlowContent.others(view: PlayerView) {
    h2 { +"Other players" }
    ul {
        view.others.forEach {
            val acted = actedLabel(view.phase, it.acted)?.let { label -> ", $label" }.orEmpty()
            li { +"${it.name}: ${it.vp} VP, ${it.handSize} cards$acted" }
        }
    }
}

private fun actedLabel(phase: Phase, acted: Boolean): String? = when (phase) {
    BUILD_UP -> if (acted) "ready" else "still building up"
    BATTLE -> if (acted) "committed" else "still choosing an army"
    WAITING_FOR_PLAYERS, GAME_OVER -> null
}

/** `{WOOD=1, HORDE=3}` → "3 Horde, 1 Wood", in [handOrder]. */
private fun counts(counts: Map<String, Int>): String =
    counts.filterValues { it > 0 }.toSortedMap(handOrder).entries
        .joinToString(", ") { (name, count) -> "$count ${displayName(name)}" }
        .ifEmpty { "nothing" }

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
