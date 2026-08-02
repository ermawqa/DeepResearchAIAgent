package com.ermukhamed.deepresearch.agent

import com.ermukhamed.deepresearch.llm.CompletionRequest
import com.ermukhamed.deepresearch.llm.LlmClient
import java.time.Clock
import java.time.Instant

/**
 * Orchestrates a single research run: prompt construction, model invocation, and
 * packaging of the result.
 *
 * Holds no transport concerns of its own — it talks to [LlmClient], so the same
 * agent works against OpenAI, a local model, or a test double.
 */
class ResearchAgent(
    private val client: LlmClient,
    private val model: String,
    private val temperature: Double,
    private val clock: Clock = Clock.systemUTC(),
) {

    /**
     * Researches [query] and returns a [ResearchReport].
     *
     * Failures surface as `Result.failure` carrying an
     * `LlmException`; the caller decides how to report them.
     */
    fun research(query: String): Result<ResearchReport> {
        val trimmed = query.trim()
        require(trimmed.isNotEmpty()) { "query must not be blank" }

        val request = CompletionRequest(
            model = model,
            messages = ResearchPrompt.buildMessages(trimmed),
            temperature = temperature,
        )

        return client.complete(request).map { completion ->
            ResearchReport(
                query = trimmed,
                content = completion.content,
                model = completion.model,
                generatedAt = Instant.now(clock),
                usage = completion.usage,
            )
        }
    }
}
