package com.github.andreyasadchy.xtra.repository

import com.github.andreyasadchy.xtra.model.ui.ViewerPoints
import com.github.andreyasadchy.xtra.util.C
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

class ChannelPointsRequestTest {
    private val response = """{"data":{"community":{"channel":{"self":{"communityPoints":{"balance":120,"availableClaim":{"id":"bonus-fixture"}}},"communityPointsSettings":{"customRewards":[]}}}}}"""

    private fun withRepository(block: suspend (GraphQLRepository, List<Request>) -> Unit) = runBlocking {
        val requests = mutableListOf<Request>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            requests += chain.request()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(response.toResponseBody()).build()
        }.build()
        val repository = GraphQLRepository(lazy { null }, lazy { null },
            lazy { error("Unused network engine") }, lazy { client }, Json { ignoreUnknownKeys = true })
        try {
            block(repository, requests)
        } finally {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        }
    }

    private fun assertCurrentRequest(request: Request) {
        assertEquals("https://gql.twitch.tv/gql", request.url.toString())
        assertEquals("POST", request.method)
        val buffer = Buffer()
        request.body!!.writeTo(buffer)
        val body = Json.parseToJsonElement(buffer.readUtf8()).jsonObject
        assertEquals("ChannelPointsContext", body.getValue("operationName").jsonPrimitive.content)
        val persisted = body.getValue("extensions").jsonObject.getValue("persistedQuery").jsonObject
        assertEquals("33884dcfeab64eb44c2d61255145643cf69ea993e0c6c2ecb0fa46238586a111",
            persisted.getValue("sha256Hash").jsonPrimitive.content)
        assertEquals(1, persisted.getValue("version").jsonPrimitive.int)
        val variables = body.getValue("variables").jsonObject
        assertEquals("fixture_channel", variables.getValue("channelLogin").jsonPrimitive.content)
        assertEquals(listOf("CREATOR", "BOOST"), variables.getValue("includeGoalTypes").jsonArray.map { it.jsonPrimitive.content })
    }

    @Test fun rewardsPanelUsesUpdatedRequestAndPreservesBalanceAndBonusParsing() = withRepository { repository, requests ->
        val result = repository.loadViewerPoints(C.OKHTTP, mapOf("X-Test-Account" to "active"), "fixture_channel")
        assertCurrentRequest(requests.single())
        assertEquals("active", requests.single().header("X-Test-Account"))
        val points = ViewerPoints.parse(result)
        assertEquals(120, points.balance)
        assertEquals("bonus-fixture", points.claimId)
        assertTrue(points.rewards.isEmpty())
    }

    @Test fun legacyBonusContextUsesTheSameUpdatedRequest() = withRepository { repository, requests ->
        val result = repository.loadChannelPointsContext(C.OKHTTP, emptyMap(), "fixture_channel")
        assertCurrentRequest(requests.single())
        val points = result.data!!.community.channel.self.communityPoints
        assertEquals(120, points.balance)
        assertEquals("bonus-fixture", points.availableClaim!!.id)
    }
}
