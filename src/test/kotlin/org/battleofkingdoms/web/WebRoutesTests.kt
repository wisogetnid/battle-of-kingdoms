package org.battleofkingdoms.web

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.server.testing.testApplication
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import org.battleofkingdoms.cards.CreatureType.HIGH_MAGE
import org.battleofkingdoms.cards.ResourceType.WOOD
import org.battleofkingdoms.game.Phase
import org.battleofkingdoms.server.MyView
import org.battleofkingdoms.server.PlayerView
import org.battleofkingdoms.server.TestApi
import org.battleofkingdoms.server.module
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val NOT_TEXT_ONLY = listOf("<script", "<style", "<img", "<link", "style=")

/** Text and hyperlinks only: no scripts, styles or images. */
fun assertTextOnly(html: String) {
    NOT_TEXT_ONLY.forEach { assertFalse(html.contains(it, ignoreCase = true), "Page contains $it:\n$html") }
}

/** The app with a client that doesn't follow redirects, so tests see each `303`. */
fun webTest(block: suspend (HttpClient) -> Unit) = testApplication {
    application { module() }
    block(createClient { followRedirects = false })
}

suspend fun page(response: HttpResponse, expected: HttpStatusCode = HttpStatusCode.OK): String {
    val html = response.bodyAsText()
    assertEquals(expected, response.status, html)
    assertTextOnly(html)
    return html
}

/** Follows a `303` by hand and checks where it points. */
fun seeOther(response: HttpResponse): String {
    assertEquals(HttpStatusCode.SeeOther, response.status)
    return response.headers[HttpHeaders.Location]!!
}

suspend fun HttpClient.post(path: String, vararg fields: Pair<String, String>): HttpResponse =
    submitForm(path, parameters { fields.forEach { (name, value) -> append(name, value) } })

/** Creates a game through the home page form and returns its id. */
suspend fun HttpClient.createGame(players: Int, host: String, seed: Long = 42): String {
    val location = seeOther(post("/ui/games", "players" to "$players", "name" to host, "seed" to "$seed"))
    return location.removePrefix("/ui/games/").substringBefore("/")
}

suspend fun HttpClient.startGame(vararg names: String): String =
    createGame(names.size, names.first()).also { id -> names.drop(1).forEach { seeOther(post("/ui/games/$id/players", "name" to it)) } }

suspend fun HttpClient.gamePage(gameId: String, name: String): String = page(get("/ui/games/$gameId/players/$name"))

/** My JSON player view, to check what the UI did against the engine's state. */
suspend fun HttpClient.view(gameId: String, name: String): JsonNode = TestApi.json(get("/games/$gameId?player=$name"))["me"]

private val RESOURCES = listOf("WOOD", "IRON", "ESSENCE")
private val TIER_1 = mapOf("WOOD" to "BOW", "IRON" to "SWORD", "ESSENCE" to "MAGE")

class WebRoutesTests {

    @Test
    fun `the home page offers to create a game and to open one by id`() = webTest { client ->
        val html = page(client.get("/"))

        assertTrue(html.contains("""action="/ui/games" method="post""""), html)
        assertTrue(html.contains("""name="players""""), html)
        assertTrue(html.contains("""action="/" method="get""""), html)
        assertTrue(html.contains("""name="game""""), html)
    }

    @Test
    fun `an invite link opens the join form for that game`() = webTest { client ->
        val html = page(client.get("/?game=abc-123"))

        assertTrue(html.contains("""action="/ui/games/abc-123/players" method="post""""), html)
        assertFalse(html.contains("""name="game""""), html)
    }

    @Test
    fun `the game id in an invite link is escaped`() = webTest { client ->
        val html = page(client.get("/?game=%3Cb%3Ex%3C%2Fb%3E"))

        assertFalse(html.contains("<b>"), html)
        assertTrue(html.contains("&lt;b&gt;x&lt;/b&gt;"), html)
    }

    @Test
    fun `enum names are shown in words`() {
        assertEquals("High Mage", HIGH_MAGE.displayName)
        assertEquals("Wood", WOOD.displayName)
    }

    @Test
    fun `creating a game redirects the host to their game page`() = webTest { client ->
        val location = seeOther(client.post("/ui/games", "players" to "3", "name" to "anna", "seed" to "42"))
        val gameId = location.removePrefix("/ui/games/").substringBefore("/")

        assertEquals("/ui/games/$gameId/players/anna", location)
        UUID.fromString(gameId)
        val html = client.gamePage(gameId, "anna")
        assertTrue(html.contains("You are anna"), html)
        assertTrue(html.contains("Phase: Waiting For Players"), html)
        assertTrue(html.contains("""href="/?game=$gameId""""), html)
        assertTrue(html.contains("Joined so far: anna"), html)
    }

    @Test
    fun `creating a game without a seed works`() = webTest { client ->
        seeOther(client.post("/ui/games", "players" to "3", "name" to "anna", "seed" to ""))
    }

    @Test
    fun `joining redirects to my game page and the game starts when full`() = webTest { client ->
        val gameId = client.createGame(3, "anna")

        assertEquals("/ui/games/$gameId/players/ben", seeOther(client.post("/ui/games/$gameId/players", "name" to "ben")))
        seeOther(client.post("/ui/games/$gameId/players", "name" to "cleo"))

        val html = client.gamePage(gameId, "anna")
        assertTrue(html.contains("Phase: Build Up"), html)
        assertTrue(html.contains("ben: 0 VP"), html)
        assertTrue(html.contains("cleo: 0 VP"), html)
        assertTrue(html.contains("still building up"), html)
        assertFalse(html.contains("invite link"), html)
    }

    @Test
    fun `the game page shows my hand and never another player's`() = webTest { client ->
        val gameId = client.startGame("anna", "ben", "cleo")

        val html = client.gamePage(gameId, "anna")

        assertTrue(html.contains("Hand: "), html)
        assertEquals(1, Regex("Hand:").findAll(html).count(), html)
    }

    @Test
    fun `the hand lists Horde first, then the other creatures, then resources alphabetically`() {
        val view = PlayerView(
            gameId = UUID.randomUUID(), phase = Phase.BUILD_UP, turn = 1, pot = 1, victoryPointsToWin = 3,
            me = MyView("anna", mapOf("WOOD" to 1, "BOW" to 1, "ESSENCE" to 2, "HORDE" to 3, "IRON" to 1, "WARBOW" to 1), emptyMap(), 0, false),
            others = emptyList(), availableActions = emptyList(), battleLog = emptyList(), winner = null,
        )

        val html = createHTML().html { gamePage(view) }

        assertTrue(html.contains("Hand: 3 Horde, 1 Bow, 1 Warbow, 2 Essence, 1 Iron, 1 Wood"), html)
    }

    @Test
    fun `a rejected create shows the home page with the message`() = webTest { client ->
        listOf("6", "2", "x").forEach { players ->
            val html = page(client.post("/ui/games", "players" to players, "name" to "anna"), HttpStatusCode.BadRequest)
            assertTrue(html.contains("Error: "), html)
            assertTrue(html.contains("Create a game"), html)
        }
        page(client.post("/ui/games", "players" to "3", "name" to " "), HttpStatusCode.BadRequest)
        page(client.post("/ui/games", "players" to "3", "name" to "anna", "seed" to "abc"), HttpStatusCode.BadRequest)
    }

    @Test
    fun `a rejected join shows the join form again with the message`() = webTest { client ->
        val gameId = client.createGame(3, "anna")

        val duplicate = page(client.post("/ui/games/$gameId/players", "name" to "anna"), HttpStatusCode.Conflict)
        assertTrue(duplicate.contains("Error: anna has already joined"), duplicate)
        assertTrue(duplicate.contains("""action="/ui/games/$gameId/players""""), duplicate)

        page(client.post("/ui/games/$gameId/players", "name" to ""), HttpStatusCode.BadRequest)
        seeOther(client.post("/ui/games/$gameId/players", "name" to "ben"))
        seeOther(client.post("/ui/games/$gameId/players", "name" to "cleo"))
        page(client.post("/ui/games/$gameId/players", "name" to "dora"), HttpStatusCode.Conflict)
    }

    @Test
    fun `an unknown game or player shows the not-found page`() = webTest { client ->
        val gameId = client.createGame(3, "anna")

        listOf(
            client.get("/ui/games/$gameId/players/zoe"),
            client.get("/ui/games/${UUID.randomUUID()}/players/anna"),
            client.get("/ui/games/not-a-uuid/players/anna"),
            client.post("/ui/games/not-a-uuid/players", "name" to "ben"),
        ).forEach {
            val html = page(it, HttpStatusCode.NotFound)
            assertTrue(html.contains("Not found"), html)
            assertTrue(html.contains("""href="/""""), html)
        }
    }

    @Test
    fun `player names are escaped on the page and encoded in links`() = webTest { client ->
        val gameId = client.createGame(3, "<b>x</b>")
        assertEquals(
            "/ui/games/$gameId/players/anna%20maria",
            seeOther(client.post("/ui/games/$gameId/players", "name" to "anna maria")),
        )

        val html = page(client.get("/ui/games/$gameId/players/anna%20maria"))
        assertTrue(html.contains("&lt;b&gt;x&lt;/b&gt;"), html)
        assertFalse(html.contains("<b>"), html)
        assertTrue(html.contains("""href="/ui/games/$gameId/players/anna%20maria""""), html)
    }

    @Test
    fun `in build-up the page offers every upgrade, a trade per resource held, and ready`() = webTest { client ->
        val gameId = client.startGame("anna", "ben", "cleo")
        val hand = client.view(gameId, "anna")["hand"]

        val html = client.gamePage(gameId, "anna")

        val path = "/ui/games/$gameId/players/anna"
        assertEquals(6, Regex("""action="$path/upgrades"""").findAll(html).count(), html)
        assertTrue(html.contains("Upgrade Horde into Bow (1 Wood)"), html)
        assertTrue(html.contains("Upgrade Mage into High Mage (1 Essence)"), html)
        RESOURCES.forEach { resource ->
            assertEquals(hand.has(resource), html.contains("Trade 1 ${displayName(resource)} for 1 Horde"), html)
        }
        assertEquals(1, Regex("""action="$path/ready"""").findAll(html).count(), html)
    }

    @Test
    fun `an upgrade redirects to my page, which lists it as upgraded this turn`() = webTest { client ->
        val gameId = client.startGame("anna", "ben", "cleo")
        val resource = RESOURCES.first { client.view(gameId, "anna")["hand"].has(it) }
        val into = TIER_1.getValue(resource)

        val location = seeOther(client.post("/ui/games/$gameId/players/anna/upgrades", "into" to into))

        assertEquals("/ui/games/$gameId/players/anna", location)
        assertEquals(1, client.view(gameId, "anna")["hand"][into].asInt())
        assertTrue(client.gamePage(gameId, "anna").contains("Upgraded this turn: 1 ${displayName(into)}"))
    }

    @Test
    fun `a trade swaps a resource for a Horde`() = webTest { client ->
        val gameId = client.startGame("anna", "ben", "cleo")
        val resource = RESOURCES.first { client.view(gameId, "anna")["hand"].has(it) }

        seeOther(client.post("/ui/games/$gameId/players/anna/trades", "resource" to resource))

        assertEquals(2, client.view(gameId, "anna")["hand"]["HORDE"].asInt())
    }

    @Test
    fun `after ready the build-up forms are gone and the others see me as ready`() = webTest { client ->
        val gameId = client.startGame("anna", "ben", "cleo")

        seeOther(client.post("/ui/games/$gameId/players/anna/ready"))

        val mine = client.gamePage(gameId, "anna")
        assertFalse(mine.contains("/upgrades"), mine)
        assertFalse(mine.contains("/trades"), mine)
        assertFalse(mine.contains("/ready"), mine)
        assertTrue(mine.contains("Waiting for the others to finish building up"), mine)
        assertEquals(mine, client.gamePage(gameId, "anna"), "reloading the page changes nothing")
        assertTrue(client.gamePage(gameId, "ben").contains("anna: 0 VP, 3 cards, ready"))
    }

    @Test
    fun `a rejected action shows my page again with the engine's message`() = webTest { client ->
        val gameId = client.createGame(3, "anna")
        val path = "/ui/games/$gameId/players/anna"

        val wrongPhase = page(client.post("$path/ready"), HttpStatusCode.Conflict)
        assertTrue(wrongPhase.contains("Error: Not allowed during WAITING_FOR_PLAYERS"), wrongPhase)
        assertTrue(wrongPhase.contains("You are anna"), wrongPhase)
        assertTrue(wrongPhase.contains("Waiting for more players to join"), wrongPhase)

        listOf("ben", "cleo").forEach { seeOther(client.post("/ui/games/$gameId/players", "name" to it)) }
        val missing = RESOURCES.first { !client.view(gameId, "anna")["hand"].has(it) }
        page(client.post("$path/upgrades", "into" to TIER_1.getValue(missing)), HttpStatusCode.BadRequest)
        page(client.post("$path/upgrades", "into" to "HORDE"), HttpStatusCode.BadRequest)
        page(client.post("$path/trades", "resource" to missing), HttpStatusCode.BadRequest)

        seeOther(client.post("$path/ready"))
        val already = page(client.post("$path/trades", "resource" to "WOOD"), HttpStatusCode.Conflict)
        assertTrue(already.contains("Error: anna has already acted this phase"), already)
    }

    @Test
    fun `an unknown or missing form value is a bad request, not a server error`() = webTest { client ->
        val gameId = client.startGame("anna", "ben", "cleo")
        val path = "/ui/games/$gameId/players/anna"

        val dragon = page(client.post("$path/upgrades", "into" to "DRAGON"), HttpStatusCode.BadRequest)
        assertTrue(dragon.contains("Error: Unknown into 'DRAGON'"), dragon)
        page(client.post("$path/upgrades"), HttpStatusCode.BadRequest)
        page(client.post("$path/trades", "resource" to "HORDE"), HttpStatusCode.BadRequest)
        page(client.post("/ui/games/$gameId/players/zoe/upgrades", "into" to "DRAGON"), HttpStatusCode.NotFound)
    }
}
