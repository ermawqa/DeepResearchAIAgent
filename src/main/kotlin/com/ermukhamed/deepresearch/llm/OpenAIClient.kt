package com.ermukhamed.deepresearch.llm

import com.ermukhamed.deepresearch.config.AppConfig
import com.ermukhamed.deepresearch.llm.dto.ChatCompletionRequestDto
import com.ermukhamed.deepresearch.llm.dto.ChatCompletionResponseDto
import com.ermukhamed.deepresearch.llm.dto.ChatMessageDto
import com.ermukhamed.deepresearch.llm.dto.ErrorEnvelopeDto
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.time.Duration
import kotlin.math.pow

/**
 * OpenAI Chat Completions implementation of [LlmClient].
 *
 * Retries retryable failures ([LlmFailure.isRetryable]) with exponential backoff,
 * honouring a `Retry-After` header when the server sends one.
 */
class OpenAIClient(
    private val config: AppConfig,
    private val httpClient: OkHttpClient = defaultHttpClient(config),
    private val mapper: ObjectMapper = jacksonObjectMapper(),
    private val sleeper: (Duration) -> Unit = { Thread.sleep(it.toMillis()) },
) : LlmClient {

    override fun complete(request: CompletionRequest): Result<CompletionResponse> {
        var lastFailure: LlmFailure? = null

        // maxRetries is the number of *extra* attempts after the first.
        for (attempt in 0..config.maxRetries) {
            if (attempt > 0) sleeper(backoffFor(attempt, lastFailure))

            when (val outcome = attempt(request)) {
                is Outcome.Success -> return Result.success(outcome.response)
                is Outcome.Failure -> {
                    lastFailure = outcome.failure
                    if (!outcome.failure.isRetryable) break
                }
            }
        }

        return Result.failure(LlmException(lastFailure ?: LlmFailure.EmptyResponse))
    }

    private fun attempt(request: CompletionRequest): Outcome {
        val httpRequest = buildHttpRequest(request)

        val response = try {
            httpClient.newCall(httpRequest).execute()
        } catch (e: IOException) {
            return Outcome.Failure(LlmFailure.Network(e))
        }

        return response.use { it.toOutcome() }
    }

    private fun buildHttpRequest(request: CompletionRequest): Request {
        val payload = ChatCompletionRequestDto(
            model = request.model,
            messages = request.messages.map { ChatMessageDto(it.role.wireName, it.content) },
            temperature = request.temperature,
        )

        return Request.Builder()
            .url("${config.baseUrl}/chat/completions")
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Accept", "application/json")
            .post(mapper.writeValueAsString(payload).toRequestBody(JSON_MEDIA_TYPE))
            .build()
    }

    private fun Response.toOutcome(): Outcome {
        val rawBody = try {
            body?.string().orEmpty()
        } catch (e: IOException) {
            return Outcome.Failure(LlmFailure.Network(e))
        }

        if (!isSuccessful) {
            return Outcome.Failure(toFailure(rawBody))
        }

        val parsed = try {
            mapper.readValue<ChatCompletionResponseDto>(rawBody)
        } catch (e: Exception) {
            return Outcome.Failure(
                LlmFailure.MalformedResponse(e.message ?: "could not parse response body as JSON"),
            )
        }

        val content = parsed.choices.firstOrNull()?.message?.content?.trim()
        if (content.isNullOrEmpty()) {
            return Outcome.Failure(LlmFailure.EmptyResponse)
        }

        return Outcome.Success(
            CompletionResponse(
                content = content,
                model = parsed.model ?: config.model,
                usage = parsed.usage?.let { TokenUsage(it.promptTokens, it.completionTokens) },
            ),
        )
    }

    private fun Response.toFailure(rawBody: String): LlmFailure {
        val detail = extractErrorMessage(rawBody) ?: message.ifBlank { "no detail provided" }

        return when (code) {
            401, 403 -> LlmFailure.Authentication(detail)
            429 -> LlmFailure.RateLimited(detail, header("Retry-After")?.toLongOrNull())
            else -> LlmFailure.Api(code, detail)
        }
    }

    private fun extractErrorMessage(rawBody: String): String? =
        try {
            mapper.readValue<ErrorEnvelopeDto>(rawBody).error?.message?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            rawBody.take(MAX_ERROR_SNIPPET).takeIf { it.isNotBlank() }
        }

    private fun backoffFor(attempt: Int, lastFailure: LlmFailure?): Duration {
        val serverHint = (lastFailure as? LlmFailure.RateLimited)?.retryAfterSeconds
        if (serverHint != null) return Duration.ofSeconds(serverHint.coerceAtMost(MAX_BACKOFF_SECONDS))

        val seconds = BASE_BACKOFF_SECONDS * 2.0.pow(attempt - 1)
        return Duration.ofMillis((seconds.coerceAtMost(MAX_BACKOFF_SECONDS.toDouble()) * 1000).toLong())
    }

    private sealed interface Outcome {
        data class Success(val response: CompletionResponse) : Outcome
        data class Failure(val failure: LlmFailure) : Outcome
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val MAX_ERROR_SNIPPET = 300
        private const val BASE_BACKOFF_SECONDS = 1.0
        private const val MAX_BACKOFF_SECONDS = 30L

        /** One shared client per process: OkHttp pools connections and threads internally. */
        fun defaultHttpClient(config: AppConfig): OkHttpClient = OkHttpClient.Builder()
            .callTimeout(config.requestTimeout)
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(config.requestTimeout)
            .build()
    }
}
