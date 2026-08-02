package com.ermukhamed.deepresearch.agent

import com.ermukhamed.deepresearch.llm.CompletionRequest
import com.ermukhamed.deepresearch.llm.CompletionResponse
import com.ermukhamed.deepresearch.llm.LlmClient
import com.ermukhamed.deepresearch.llm.LlmException
import com.ermukhamed.deepresearch.llm.LlmFailure
import com.ermukhamed.deepresearch.llm.Message
import com.ermukhamed.deepresearch.llm.TokenUsage
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ResearchAgentTest {

    private val fixedInstant: Instant = Instant.parse("2026-08-02T10:15:30Z")
    private val fixedClock: Clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

    /** Captures the request the agent builds and replays a canned response. */
    private class RecordingClient(
        private val result: Result<CompletionResponse>,
    ) : LlmClient {
        var lastRequest: CompletionRequest? = null
        var callCount = 0

        override fun complete(request: CompletionRequest): Result<CompletionResponse> {
            lastRequest = request
            callCount++
            return result
        }
    }

    private fun succeedingClient(content: String = "## Summary\nSome findings.") = RecordingClient(
        Result.success(
            CompletionResponse(content = content, model = "gpt-4o-mini", usage = TokenUsage(10, 20)),
        ),
    )

    private fun agentFor(client: LlmClient, temperature: Double = 0.3) =
        ResearchAgent(client = client, model = "gpt-4o-mini", temperature = temperature, clock = fixedClock)

    @Test
    fun `produces a report carrying query model timestamp and usage`() {
        val client = succeedingClient()

        val report = agentFor(client).research("Retrieval-augmented generation").getOrThrow()

        assertEquals("Retrieval-augmented generation", report.query)
        assertEquals("## Summary\nSome findings.", report.content)
        assertEquals("gpt-4o-mini", report.model)
        assertEquals(fixedInstant, report.generatedAt)
        assertEquals(30, report.usage?.totalTokens)
    }

    @Test
    fun `sends a system instruction ahead of the user turn`() {
        val client = succeedingClient()

        agentFor(client).research("Vector databases").getOrThrow()

        val messages = client.lastRequest!!.messages
        assertEquals(2, messages.size)
        assertEquals(Message.Role.SYSTEM, messages[0].role)
        assertEquals(Message.Role.USER, messages[1].role)
        assertTrue(messages[1].content.contains("Vector databases"))
    }

    @Test
    fun `system instruction pins the section contract the renderers rely on`() {
        val client = succeedingClient()

        agentFor(client).research("Anything").getOrThrow()

        val instruction = client.lastRequest!!.messages.first().content
        listOf(
            "## Summary",
            "## Key Concepts",
            "## Current Research Directions",
            "## Tools & Frameworks",
            "## Challenges & Open Questions",
            "## Further Reading",
        ).forEach { section ->
            assertTrue(instruction.contains(section), "prompt should require the '$section' section")
        }
    }

    @Test
    fun `instructs the model not to fabricate citations`() {
        val client = succeedingClient()

        agentFor(client).research("Anything").getOrThrow()

        val instruction = client.lastRequest!!.messages.first().content.lowercase()
        assertTrue(instruction.contains("do not invent citations"))
    }

    @Test
    fun `forwards the configured model and temperature`() {
        val client = succeedingClient()

        agentFor(client, temperature = 1.25).research("Anything").getOrThrow()

        assertEquals("gpt-4o-mini", client.lastRequest!!.model)
        assertEquals(1.25, client.lastRequest!!.temperature)
    }

    @Test
    fun `trims surrounding whitespace from the query`() {
        val client = succeedingClient()

        val report = agentFor(client).research("  Kotlin coroutines \n").getOrThrow()

        assertEquals("Kotlin coroutines", report.query)
    }

    @Test
    fun `rejects a blank query without calling the model`() {
        val client = succeedingClient()

        val error = runCatching { agentFor(client).research("   ") }.exceptionOrNull()

        assertIs<IllegalArgumentException>(error)
        assertEquals(0, client.callCount, "a blank query should never reach the API")
    }

    @Test
    fun `propagates a client failure unchanged`() {
        val failure = LlmFailure.RateLimited("quota exceeded", retryAfterSeconds = 3)
        val client = RecordingClient(Result.failure(LlmException(failure)))

        val error = agentFor(client).research("Anything").exceptionOrNull()

        assertIs<LlmException>(error)
        assertEquals(failure, error.failure)
    }

    @Test
    fun `reports the model the provider actually served, not the one requested`() {
        // Providers resolve aliases to dated snapshots; the report should record the truth.
        val client = RecordingClient(
            Result.success(CompletionResponse("text", model = "gpt-4o-mini-2024-07-18", usage = null)),
        )

        val report = agentFor(client).research("Anything").getOrThrow()

        assertEquals("gpt-4o-mini-2024-07-18", report.model)
    }
}
