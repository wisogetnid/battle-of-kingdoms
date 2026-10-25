package org.battleofkingdoms.server

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.assertEquals

private val mapper = jacksonObjectMapper()

/** Thin JSON client for the game API, used by the API and end-to-end tests. */
class TestApi(private val client: HttpClient) {

    suspend fun post(path: String, body: Any? = null): HttpResponse = client.post(path) {
        if (body != null) {
            contentType(ContentType.Application.Json)
            setBody(mapper.writeValueAsString(body))
        }
    }

    suspend fun createGame(players: Int, host: String, seed: Long? = 42): String {
        val response = post("/games", mapOf("players" to players, "host" to host, "seed" to seed))
        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        return json(response)["gameId"].asText()
    }

    suspend fun join(gameId: String, name: String) {
        val response = post("/games/$gameId/players", mapOf("name" to name))
        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
    }

    suspend fun startGame(vararg names: String): String =
        createGame(names.size, names.first()).also { id -> names.drop(1).forEach { join(id, it) } }

    suspend fun viewResponse(gameId: String, player: String): HttpResponse =
        client.get("/games/$gameId") { parameter("player", player) }

    suspend fun view(gameId: String, player: String): JsonNode {
        val response = viewResponse(gameId, player)
        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        return json(response)
    }

    companion object {
        suspend fun json(response: HttpResponse): JsonNode = mapper.readTree(response.bodyAsText())
    }
}

fun apiTest(block: suspend ApplicationTestBuilder.(TestApi) -> Unit) = testApplication {
    application { module() }
    block(TestApi(client))
}

suspend fun assertError(expected: HttpStatusCode, response: HttpResponse) {
    assertEquals(expected, response.status, response.bodyAsText())
    assert(TestApi.json(response)["error"].asText().isNotBlank()) { "Missing error message" }
}
