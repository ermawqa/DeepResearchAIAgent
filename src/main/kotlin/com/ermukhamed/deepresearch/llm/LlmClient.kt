package com.ermukhamed.deepresearch.llm

/**
 * Provider-agnostic chat completion port.
 *
 * The agent depends on this interface rather than on [OpenAIClient] directly, so
 * swapping providers (or substituting a fake in tests) touches no agent code.
 */
fun interface LlmClient {
    /** Runs a chat completion, returning the assistant reply or an [LlmException]. */
    fun complete(request: CompletionRequest): Result<CompletionResponse>
}

/** A single turn in a chat conversation. */
data class Message(val role: Role, val content: String) {
    enum class Role(val wireName: String) {
        SYSTEM("system"),
        USER("user"),
        ASSISTANT("assistant"),
    }

    companion object {
        fun system(content: String) = Message(Role.SYSTEM, content)
        fun user(content: String) = Message(Role.USER, content)
    }
}

/** What the caller wants generated. */
data class CompletionRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double,
) {
    init {
        require(messages.isNotEmpty()) { "messages must not be empty" }
    }
}

/** What the provider produced, plus the token accounting it reported. */
data class CompletionResponse(
    val content: String,
    val model: String,
    val usage: TokenUsage?,
)

/** Token accounting for a single completion. */
data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
) {
    val totalTokens: Int get() = promptTokens + completionTokens
}
