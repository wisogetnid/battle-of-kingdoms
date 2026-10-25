package org.battleofkingdoms.web

import io.ktor.http.encodeURLPathPart
import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.label
import kotlinx.html.numberInput
import kotlinx.html.p
import kotlinx.html.submitInput
import kotlinx.html.textInput
import kotlinx.html.title

/** `HIGH_MAGE` → "High Mage", `WOOD` → "Wood". */
fun displayName(constant: String): String =
    constant.split('_').joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }

val Enum<*>.displayName: String get() = displayName(name)

fun gamePath(gameId: String): String = "/ui/games/${gameId.encodeURLPathPart()}"

/** Create a game, or join one: [gameId] comes from an invite link `/?game={id}`. */
fun HTML.homePage(gameId: String? = null) {
    head { title("Battle of Kingdoms") }
    body {
        h1 { +"Battle of Kingdoms" }
        createGameForm()
        joinGameForm(gameId)
    }
}

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
