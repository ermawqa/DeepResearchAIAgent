package com.ermukhamed.deepresearch.llm

/**
 * Every way a completion call can fail, enumerated.
 *
 * Modelling failures as data (rather than returning `"❌ error: ..."` strings)
 * lets callers branch on the cause — the retry policy keys off [isRetryable],
 * and the CLI maps each case to a distinct exit code.
 */
sealed interface LlmFailure {
    val message: String

    /** The request never completed: DNS, TLS, connection reset, timeout. */
    data class Network(val cause: Throwable) : LlmFailure {
        override val message: String get() = "Network error: ${cause.message ?: cause::class.simpleName}"
    }

    /** 401/403 — the key is missing, revoked, or lacks access to the model. */
    data class Authentication(val detail: String) : LlmFailure {
        override val message: String get() = "Authentication failed: $detail"
    }

    /** 429 — quota or rate limit exhausted. */
    data class RateLimited(val detail: String, val retryAfterSeconds: Long?) : LlmFailure {
        override val message: String
            get() = buildString {
                append("Rate limited: ").append(detail)
                retryAfterSeconds?.let { append(" (retry after ${it}s)") }
            }
    }

    /** Any other non-2xx response, carrying the provider's own error text. */
    data class Api(val statusCode: Int, val detail: String) : LlmFailure {
        override val message: String get() = "API error $statusCode: $detail"
    }

    /** 2xx, but the payload was not the shape we expect. */
    data class MalformedResponse(val detail: String) : LlmFailure {
        override val message: String get() = "Malformed response: $detail"
    }

    /** 2xx with a well-formed body that contained no usable text. */
    data object EmptyResponse : LlmFailure {
        override val message: String get() = "The model returned an empty response."
    }

    /** Whether retrying the identical request could plausibly succeed. */
    val isRetryable: Boolean
        get() = when (this) {
            is Network -> true
            is RateLimited -> true
            is Api -> statusCode >= 500
            is Authentication, is MalformedResponse, EmptyResponse -> false
        }
}

/** Carries an [LlmFailure] through `Result.failure`. */
class LlmException(val failure: LlmFailure) : Exception(failure.message)
