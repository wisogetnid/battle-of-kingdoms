package org.battleofkingdoms.web

import io.ktor.client.request.get
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val MAX_TURNS = 20
private val PLAYERS = listOf("anna", "ben", "cleo")

/**
 * A full 3-player game played only through the UI's forms and pages. Every turn everyone marks
 * ready, anna commits every creature she holds and the others commit nothing, so anna wins each
 * pot as the only committer. It asserts what the script forces, not seeded numbers or the VP
 * target, so changes to the rules' defaults don't break it.
 */
class WebGameTest {

    @Test
    fun `a seeded 3-player game is played through the forms to game over`() = webTest { client ->
        val gameId = client.createGame(PLAYERS.size, "anna")
        PLAYERS.drop(1).forEach { name ->
            val invite = page(client.get("/?game=$gameId"))
            assertTrue(invite.contains("""action="/ui/games/$gameId/players""""), invite)
            seeOther(client.post("/ui/games/$gameId/players", "name" to name))
        }

        var turn = 1
        while (!client.gamePage(gameId, "anna").contains("Phase: Game Over")) {
            check(turn <= MAX_TURNS) { "Game didn't end within $MAX_TURNS turns" }
            assertTrue(client.gamePage(gameId, "anna").contains("Phase: Build Up"))
            client.allReady(gameId, *PLAYERS.toTypedArray())

            assertTrue(client.gamePage(gameId, "anna").contains("Phase: Battle"))
            val hand = client.view(gameId, "anna")["hand"]
            val army = CREATURES.filter { hand.has(it) }.map { it to hand[it].asText() }
            seeOther(client.post("/ui/games/$gameId/players/anna/army", *army.toTypedArray()))
            PLAYERS.drop(1).forEach { seeOther(client.post("/ui/games/$gameId/players/$it/army")) }

            val html = client.gamePage(gameId, "ben")
            assertTrue(html.contains("Battle $turn"), "battle $turn should be in the log:\n$html")
            assertTrue(html.contains("anna won 1 VP."), html)
            turn++
        }

        PLAYERS.forEach { name ->
            val html = client.gamePage(gameId, name)
            assertTrue(html.contains("Winner: anna"), html)
            assertTrue(html.contains("The game is over. anna won."), html)
            assertFalse(html.contains("<form"), "no actions after the game is over:\n$html")
        }
        assertEquals(turn - 1, Regex("<h3>Battle ").findAll(client.gamePage(gameId, "anna")).count())
    }
}

private val CREATURES = listOf("HORDE", "BOW", "SWORD", "MAGE", "WARBOW", "KNIGHT", "HIGH_MAGE")
