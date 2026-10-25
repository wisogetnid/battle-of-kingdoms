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
import org.battleofkingdoms.cards.Artifact.TRANSMUTER_STONE
import org.battleofkingdoms.cards.CreatureType.HIGH_MAGE
import org.battleofkingdoms.cards.ResourceType.WOOD
import org.battleofkingdoms.game.GameConfig
import org.battleofkingdoms.game.Phase
import org.battleofkingdoms.server.MyView
import org.battleofkingdoms.server.PlayerView
import org.battleofkingdoms.server.TestApi
import org.battleofkingdoms.server.GameServer
import org.battleofkingdoms.server.gameModule
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
fun webTest(config: GameConfig = GameConfig(), block: suspend (HttpClient) -> Unit) = testApplication {
    application { gameModule(GameServer(config)) }
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
suspend fun HttpClient.createGame(players: Int, host: String, seed: Long = 42, artifacts: Boolean = false): String {
    val fields = listOf("players" to "$players", "name" to host, "seed" to "$seed") + listOfNotNull(("artifacts" to "on").takeIf { artifacts })
    val location = seeOther(post("/ui/games", *fields.toTypedArray()))
    return location.removePrefix("/ui/games/").substringBefore("/")
}

/** Without [artifacts] unless asked, like an unticked box. */
suspend fun HttpClient.startGame(vararg names: String, artifacts: Boolean = false): String =
    createGame(names.size, names.first(), artifacts = artifacts).also { id -> names.drop(1).forEach { seeOther(post("/ui/games/$id/players", "name" to it)) } }

suspend fun HttpClient.gamePage(gameId: String, name: String): String = page(get("/ui/games/$gameId/players/$name"))

/** My JSON player view, to check what the UI did against the engine's state. */
suspend fun HttpClient.view(gameId: String, name: String): JsonNode = TestApi.json(get("/games/$gameId?player=$name"))["me"]

private val RESOURCES = listOf("WOOD", "IRON", "ESSENCE")
private val TIER_1 = mapOf("WOOD" to "BOW", "IRON" to "SWORD", "ESSENCE" to "MAGE")

/** Trades all of [name]'s resources for Horde through the UI. */
suspend fun HttpClient.tradeAll(gameId: String, name: String) {
    val hand = view(gameId, name)["hand"]
    RESOURCES.forEach { resource ->
        repeat(hand.path(resource).asInt(0)) { seeOther(post("/ui/games/$gameId/players/$name/trades", "resource" to resource)) }
    }
}

suspend fun HttpClient.allReady(gameId: String, vararg names: String) =
    names.forEach { seeOther(post("/ui/games/$gameId/players/$it/ready")) }

class WebRoutesTests {

    @Test
    fun `the home page offers to create a game and to open one by id`() = webTest { client ->
        val html = page(client.get("/"))

        assertTrue(html.contains("""action="/ui/games" method="post""""), html)
        assertTrue(html.contains("""name="players""""), html)
        assertTrue(html.contains("""<input type="checkbox" name="artifacts" checked="checked">"""), html)
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

        assertTrue(html.contains("Hand:"), html)
        assertEquals(1, Regex("Hand:").findAll(html).count(), html)
    }

    @Test
    fun `the hand is broken down into tier 2, tier 1, Horde and resources`() {
        val view = PlayerView(
            gameId = UUID.randomUUID(), phase = Phase.BUILD_UP, turn = 1, pot = 1, victoryPointsToWin = 3,
            me = MyView("anna", mapOf("WOOD" to 1, "BOW" to 1, "ESSENCE" to 2, "HORDE" to 3, "IRON" to 1, "WARBOW" to 1), emptyMap(), 0, false),
            others = emptyList(), availableActions = emptyList(), battleLog = emptyList(), winner = null,
        )

        val html = createHTML().html { gamePage(view) }

        val hand = Regex("<li>(Tier 2|Tier 1|Horde|Resources): [^<]*</li>").findAll(html).map { it.value }.toList()
        assertEquals(
            listOf(
                "<li>Tier 2: 1 Warbow</li>",
                "<li>Tier 1: 1 Bow</li>",
                "<li>Horde: 3</li>",
                "<li>Resources: 2 Essence, 1 Iron, 1 Wood</li>",
            ),
            hand,
        )
    }

    @Test
    fun `tier lines appear only when I hold such units, Horde and resources always`() {
        val view = PlayerView(
            gameId = UUID.randomUUID(), phase = Phase.BUILD_UP, turn = 1, pot = 1, victoryPointsToWin = 3,
            me = MyView("anna", mapOf("BOW" to 1), emptyMap(), 0, false),
            others = emptyList(), availableActions = emptyList(), battleLog = emptyList(), winner = null,
        )

        val html = createHTML().html { gamePage(view) }

        assertFalse(html.contains("Tier 2:"), html)
        assertTrue(html.contains("<li>Tier 1: 1 Bow</li>"), html)
        assertTrue(html.contains("<li>Horde: 0</li>"), html)
        assertTrue(html.contains("<li>Resources: none</li>"), html)
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
    fun `in build-up the page offers only affordable upgrades, a trade per resource held, and ready`() = webTest { client ->
        val gameId = client.startGame("anna", "ben", "cleo")
        val hand = client.view(gameId, "anna")["hand"]

        val html = client.gamePage(gameId, "anna")

        val path = "/ui/games/$gameId/players/anna"
        val held = RESOURCES.filter { hand.has(it) }
        assertEquals(held.size, Regex("""action="$path/upgrades"""").findAll(html).count(), html)
        RESOURCES.forEach { resource ->
            val upgrade = "Upgrade Horde into ${displayName(TIER_1.getValue(resource))} (1 ${displayName(resource)})"
            assertEquals(hand.has(resource), html.contains(upgrade), html)
            assertEquals(hand.has(resource), html.contains("Trade 1 ${displayName(resource)} for 1 Horde"), html)
        }
        assertFalse(html.contains("into Warbow"), html)
        assertEquals(1, Regex("""action="$path/ready"""").findAll(html).count(), html)
    }

    @Test
    fun `with nothing affordable the page says so instead of offering upgrades`() = webTest { client ->
        val gameId = client.startGame("anna", "ben", "cleo")
        client.tradeAll(gameId, "anna")

        val html = client.gamePage(gameId, "anna")

        assertFalse(html.contains("/upgrades"), html)
        assertTrue(html.contains("No upgrade is affordable right now."), html)
        assertTrue(html.contains("/ready"), html)
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

    @Test
    fun `in battle the page offers a count field per creature I hold`() = webTest { client ->
        val gameId = client.startGame("anna", "ben", "cleo")
        client.tradeAll(gameId, "anna")
        client.allReady(gameId, "anna", "ben", "cleo")

        val html = client.gamePage(gameId, "anna")

        assertTrue(html.contains("""action="/ui/games/$gameId/players/anna/army" method="post""""), html)
        assertTrue(html.contains("Horde (you have 3)"), html)
        assertTrue(html.contains("""name="HORDE" min="0" max="3" value="0""""), html)
        assertFalse(html.contains("""name="WOOD""""), html)
        assertFalse(html.contains("/upgrades"), html)
    }

    @Test
    fun `committing an army redirects, and the others see it committed but not what it is`() = webTest { client ->
        val gameId = client.startGame("anna", "ben", "cleo")
        client.allReady(gameId, "anna", "ben", "cleo")

        assertEquals(
            "/ui/games/$gameId/players/anna",
            seeOther(client.post("/ui/games/$gameId/players/anna/army", "HORDE" to "1")),
        )

        val mine = client.gamePage(gameId, "anna")
        assertTrue(mine.contains("Waiting for the others to commit"), mine)
        assertFalse(mine.contains("/army"), mine)
        val bens = client.gamePage(gameId, "ben")
        assertTrue(bens.contains("anna: 0 VP, 3 cards, committed"), bens)
        assertTrue(bens.contains("No battles yet."), bens)
        assertFalse(bens.contains("committed 1 Horde"), bens)
    }

    @Test
    fun `blank and zero counts commit nothing`() = webTest { client ->
        val gameId = client.startGame("anna", "ben", "cleo")
        client.allReady(gameId, "anna", "ben", "cleo")

        seeOther(client.post("/ui/games/$gameId/players/anna/army", "HORDE" to "0"))
        seeOther(client.post("/ui/games/$gameId/players/ben/army", "HORDE" to ""))
        seeOther(client.post("/ui/games/$gameId/players/cleo/army"))

        val html = client.gamePage(gameId, "anna")
        assertTrue(html.contains("No winner, nobody scores."), html)
        assertTrue(html.contains("anna sat out"), html)
    }

    @Test
    fun `a malformed or unaffordable army is a bad request`() = webTest { client ->
        val gameId = client.startGame("anna", "ben", "cleo")
        client.allReady(gameId, "anna", "ben", "cleo")
        val path = "/ui/games/$gameId/players/anna/army"

        val word = page(client.post(path, "HORDE" to "two"), HttpStatusCode.BadRequest)
        assertTrue(word.contains("Error: 'HORDE' must be a whole number"), word)
        page(client.post(path, "HORDE" to "-1"), HttpStatusCode.BadRequest)
        page(client.post(path, "HORDE" to "2"), HttpStatusCode.BadRequest)
        page(client.post(path, "KNIGHT" to "1"), HttpStatusCode.BadRequest)
        assertTrue(client.gamePage(gameId, "anna").contains("/army"), "anna can still commit")
    }

    @Test
    fun `a resolved battle shows in every player's log, newest first`() = webTest { client ->
        val gameId = client.startGame("anna", "ben", "cleo")
        client.tradeAll(gameId, "anna") // 1 Horde + 2 traded
        client.allReady(gameId, "anna", "ben", "cleo")
        seeOther(client.post("/ui/games/$gameId/players/anna/army", "HORDE" to "3"))
        seeOther(client.post("/ui/games/$gameId/players/ben/army", "HORDE" to "1"))
        seeOther(client.post("/ui/games/$gameId/players/cleo/army"))

        listOf("anna", "ben", "cleo").forEach { name ->
            val html = client.gamePage(gameId, name)
            assertTrue(html.contains("Battle 1"), html)
            assertTrue(html.contains("anna won 1 VP."), html)
            assertTrue(html.contains("anna: committed 3 Horde; score +2; lost 1 Horde; gained 1 VP"), html)
            assertTrue(html.contains("ben: committed 1 Horde; score -2; lost 1 Horde; gained 0 VP"), html)
            assertTrue(html.contains("cleo sat out"), html)
            assertTrue(html.contains("anna 3 – 1 ben"), html)
            assertFalse(html.contains("cleo</li>"), "cleo sat out, so nobody duels her:\n$html")
        }

        client.allReady(gameId, "anna", "ben", "cleo")
        listOf("anna", "ben", "cleo").forEach { seeOther(client.post("/ui/games/$gameId/players/$it/army")) }
        val html = client.gamePage(gameId, "anna")
        assertTrue(html.indexOf("Battle 2") < html.indexOf("Battle 1"), html)
    }

    @Test
    fun `without artifacts a win is 1 VP and artifacts are never mentioned`() = webTest { client ->
        val gameId = annaWins(client, artifacts = false)

        listOf("anna", "ben", "cleo").forEach { name ->
            val html = client.gamePage(gameId, name)
            assertTrue(html.contains("anna won 1 VP."), html)
            assertFalse(html.contains("rtifact"), html)
        }
    }

    @Test
    fun `with artifacts only the winner sees the drawn artifact and may claim`() = webTest { client ->
        val gameId = annaWins(client, artifacts = true)
        val drawn = displayName(client.view(gameId, "anna")["drawnArtifact"].asText())

        val mine = client.gamePage(gameId, "anna")
        assertTrue(mine.contains("You won the battle and drew the $drawn"), mine)
        assertTrue(mine.contains("Take the $drawn"), mine)
        assertTrue(mine.contains("Take 1 VP"), mine)
        listOf("ben", "cleo").forEach { name ->
            val html = client.gamePage(gameId, name)
            assertTrue(html.contains("anna won and is choosing a prize."), html)
            assertTrue(html.contains("Waiting for the battle's winner to take a prize."), html)
            assertTrue(html.contains("Artifacts left in the stack: 4"), html)
            assertFalse(html.contains(drawn), "$name must not see the drawn $drawn:\n$html")
        }
    }

    @Test
    fun `a taken artifact shows for everyone`() = webTest { client ->
        val gameId = annaWins(client, artifacts = true)
        val drawn = displayName(client.view(gameId, "anna")["drawnArtifact"].asText())

        seeOther(client.post("/ui/games/$gameId/players/anna/claim", "take" to "ARTIFACT"))

        assertTrue(client.gamePage(gameId, "anna").contains("Artifacts: $drawn"))
        val html = client.gamePage(gameId, "ben")
        assertTrue(html.contains("anna won the $drawn."), html)
        assertTrue(html.contains("anna: 0 VP"), html)
        assertTrue(html.contains(", $drawn"), html)
        assertTrue(html.contains("Artifacts left in the stack: 3"), html)
    }

    @Test
    fun `taking the VP keeps the declined artifact hidden`() = webTest { client ->
        val gameId = annaWins(client, artifacts = true)
        val drawn = displayName(client.view(gameId, "anna")["drawnArtifact"].asText())

        seeOther(client.post("/ui/games/$gameId/players/anna/claim", "take" to "VICTORY_POINT"))

        assertEquals(1, client.view(gameId, "anna")["vp"].asInt())
        listOf("anna", "ben", "cleo").forEach { name ->
            val html = client.gamePage(gameId, name)
            assertTrue(html.contains("anna won 1 VP."), html)
            assertTrue(html.contains("Artifacts left in the stack: 4"), html)
            assertFalse(html.contains(drawn), "$name's page names the declined $drawn:\n$html")
        }
    }

    @Test
    fun `only the winner can claim`() = webTest { client ->
        val gameId = annaWins(client, artifacts = true)

        page(client.post("/ui/games/$gameId/players/ben/claim", "take" to "VICTORY_POINT"), HttpStatusCode.Conflict)
        page(client.post("/ui/games/$gameId/players/anna/claim", "take" to "FAMOUS_CROWN"), HttpStatusCode.BadRequest)
    }

    @Test
    fun `the Transmuter Stone's owner gets a transmute button per resource`() = webTest(GameConfig(artifacts = listOf(TRANSMUTER_STONE))) { client ->
        val gameId = annaWins(client, artifacts = true)
        seeOther(client.post("/ui/games/$gameId/players/anna/claim", "take" to "ARTIFACT"))

        val held = RESOURCES.filter { client.view(gameId, "anna")["hand"].path(it).asInt(0) > 0 }
        val html = client.gamePage(gameId, "anna")
        held.forEach { assertTrue(html.contains("Transmute 1 ${displayName(it)} into a random other resource"), html) }
        assertFalse(client.gamePage(gameId, "ben").contains("Transmute 1 "))

        val before = client.view(gameId, "anna")["hand"]
        seeOther(client.post("/ui/games/$gameId/players/anna/transmute", "resource" to held.first()))
        val after = client.view(gameId, "anna")["hand"]
        assertEquals(before.path(held.first()).asInt() - 1, after.path(held.first()).asInt(0))
        assertFalse(client.gamePage(gameId, "anna").contains("Transmute 1 "), "once per turn")
    }

    /** Everyone readies; anna commits 1 Horde and the others nothing, so she wins uncontested. */
    private suspend fun annaWins(client: HttpClient, artifacts: Boolean): String {
        val gameId = client.startGame("anna", "ben", "cleo", artifacts = artifacts)
        client.allReady(gameId, "anna", "ben", "cleo")
        seeOther(client.post("/ui/games/$gameId/players/ben/army"))
        seeOther(client.post("/ui/games/$gameId/players/cleo/army"))
        seeOther(client.post("/ui/games/$gameId/players/anna/army", "HORDE" to "1"))
        return gameId
    }
}
