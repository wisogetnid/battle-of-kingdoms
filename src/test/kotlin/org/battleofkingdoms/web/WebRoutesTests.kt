package org.battleofkingdoms.web

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.battleofkingdoms.cards.CreatureType.HIGH_MAGE
import org.battleofkingdoms.cards.ResourceType.WOOD
import org.battleofkingdoms.server.module
import org.junit.jupiter.api.Test
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
}
