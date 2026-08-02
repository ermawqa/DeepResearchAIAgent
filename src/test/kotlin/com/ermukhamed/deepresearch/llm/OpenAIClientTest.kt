package com.ermukhamed.deepresearch.llm

import com.ermukhamed.deepresearch.config.AppConfig
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.time.Duration
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises the HTTP layer against a local [MockWebServer]; no network, no API key,
 * no cost — so these run in CI on every push.
 */
class OpenAIClientTest {

    private lateinit var server: MockWebServer
    private val mapper = jacksonObjectMapper()

    /** Recorded backoff durations, so retry timing is asserted without real sleeping. */
    private val sleeps = mutableListOf<Duration>()

    @BeforeTest
    fun setUp() {
        server = MockWebServer().apply { start() }
        sleeps.clear()
    }

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    private fun client(maxRetries: Int = 0) = OpenAIClient(
        config = AppConfig(
            apiKey = "sk-test",
            model = "gpt-4o-mini",
            baseUrl = server.url("/v1").toString().trimEnd('/'),
            maxRetries = maxRetries,
        ),
        sleeper = { sleeps += it },
    )

    private fun request() = CompletionRequest(
        model = "gpt-4o-mini",
        messages = listOf(Message.system("be helpful"), Message.user("Explain RAG")),
        temperature = 0.3,
    )

    private fun successBody(content: String = "## Summary\nRAG grounds an LLM in retrieved documents.") = """
        {
          "id": "chatcmpl-123",
          "object": "chat.completion",
          "model": "gpt-4o-mini-2024-07-18",
          "choices": [
            { "index": 0, "message": { "role": "assistant", "content": ${mapper.writeValueAsString(content)} },
              "finish_reason": "stop" }
          ],
          "usage": { "prompt_tokens": 42, "completion_tokens": 108, "total_tokens": 150 }
        }
    """.trimIndent()

    @Test
    fun `parses a successful completion including usage`() {
        server.enqueue(MockResponse().setBody(successBody()).setHeader("Content-Type", "application/json"))

        val response = client().complete(request()).getOrThrow()

        assertEquals("## Summary\nRAG grounds an LLM in retrieved documents.", response.content)
        assertEquals("gpt-4o-mini-2024-07-18", response.model)
        assertEquals(42, response.usage?.promptTokens)
        assertEquals(108, response.usage?.completionTokens)
        assertEquals(150, response.usage?.totalTokens)
    }

    @Test
    fun `sends a well formed authenticated request`() {
        server.enqueue(MockResponse().setBody(successBody()))

        client().complete(request()).getOrThrow()

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/chat/completions", recorded.path)
        assertEquals("Bearer sk-test", recorded.getHeader("Authorization"))
        assertTrue(recorded.getHeader("Content-Type")!!.startsWith("application/json"))

        val body: JsonNode = mapper.readTree(recorded.body.readUtf8())
        assertEquals("gpt-4o-mini", body["model"].asText())
        assertEquals(0.3, body["temperature"].asDouble())
        assertEquals(2, body["messages"].size())
        assertEquals("system", body["messages"][0]["role"].asText())
        assertEquals("user", body["messages"][1]["role"].asText())
        assertEquals("Explain RAG", body["messages"][1]["content"].asText())
    }

    @Test
    fun `tolerates unknown fields the provider may add`() {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "model": "gpt-4o-mini",
                  "service_tier": "default",
                  "system_fingerprint": "fp_abc",
                  "choices": [{ "index": 0, "message": { "role": "assistant", "content": "hello" },
                               "logprobs": null, "finish_reason": "stop" }]
                }
                """.trimIndent(),
            ),
        )

        val response = client().complete(request()).getOrThrow()

        assertEquals("hello", response.content)
        assertNull(response.usage)
    }

    @Test
    fun `maps 401 to an authentication failure and does not retry it`() {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("""{"error":{"message":"Incorrect API key provided","type":"invalid_request_error"}}"""),
        )

        val failure = failureOf(client(maxRetries = 3).complete(request()))

        assertIs<LlmFailure.Authentication>(failure)
        assertTrue(failure.message.contains("Incorrect API key provided"))
        assertEquals(1, server.requestCount, "authentication failures must not be retried")
        assertTrue(sleeps.isEmpty())
    }

    @Test
    fun `maps 429 to a rate limit failure carrying retry-after`() {
        server.enqueue(
            MockResponse().setResponseCode(429)
                .setHeader("Retry-After", "7")
                .setBody("""{"error":{"message":"Rate limit reached"}}"""),
        )

        val failure = failureOf(client().complete(request()))

        assertIs<LlmFailure.RateLimited>(failure)
        assertEquals(7L, failure.retryAfterSeconds)
        assertTrue(failure.isRetryable)
    }

    @Test
    fun `retries a 500 and succeeds on the follow-up attempt`() {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":{"message":"server error"}}"""))
        server.enqueue(MockResponse().setBody(successBody("recovered")))

        val response = client(maxRetries = 2).complete(request()).getOrThrow()

        assertEquals("recovered", response.content)
        assertEquals(2, server.requestCount)
        assertEquals(1, sleeps.size, "should back off exactly once between the two attempts")
    }

    @Test
    fun `gives up after exhausting retries and reports the last failure`() {
        repeat(3) {
            server.enqueue(MockResponse().setResponseCode(503).setBody("""{"error":{"message":"unavailable"}}"""))
        }

        val failure = failureOf(client(maxRetries = 2).complete(request()))

        assertIs<LlmFailure.Api>(failure)
        assertEquals(503, failure.statusCode)
        assertEquals(3, server.requestCount, "one initial attempt plus two retries")
    }

    @Test
    fun `honours the server retry-after hint when backing off`() {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "5").setBody("{}"))
        server.enqueue(MockResponse().setBody(successBody()))

        client(maxRetries = 1).complete(request()).getOrThrow()

        assertEquals(listOf(Duration.ofSeconds(5)), sleeps)
    }

    @Test
    fun `maps an empty choices array to an empty response failure`() {
        server.enqueue(MockResponse().setBody("""{"model":"gpt-4o-mini","choices":[]}"""))

        val failure = failureOf(client().complete(request()))

        assertEquals(LlmFailure.EmptyResponse, failure)
    }

    @Test
    fun `maps whitespace-only content to an empty response failure`() {
        server.enqueue(MockResponse().setBody(successBody("   \n  ")))

        val failure = failureOf(client().complete(request()))

        assertEquals(LlmFailure.EmptyResponse, failure)
    }

    @Test
    fun `maps unparseable json to a malformed response failure`() {
        server.enqueue(MockResponse().setBody("this is not json"))

        val failure = failureOf(client().complete(request()))

        assertIs<LlmFailure.MalformedResponse>(failure)
    }

    @Test
    fun `falls back to a body snippet when an error response has no error object`() {
        server.enqueue(MockResponse().setResponseCode(502).setBody("<html>Bad Gateway</html>"))

        val failure = failureOf(client().complete(request()))

        assertIs<LlmFailure.Api>(failure)
        assertTrue(failure.detail.contains("Bad Gateway"))
    }

    @Test
    fun `maps a dropped connection to a network failure`() {
        server.shutdown()

        val failure = failureOf(client().complete(request()))

        assertIs<LlmFailure.Network>(failure)
        assertTrue(failure.isRetryable)
    }

    private fun failureOf(result: Result<CompletionResponse>): LlmFailure {
        val error = result.exceptionOrNull()
        assertIs<LlmException>(error, "expected the call to fail")
        return error.failure
    }
}
