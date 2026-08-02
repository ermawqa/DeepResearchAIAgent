package com.ermukhamed.deepresearch.llm.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Wire format for the OpenAI `/chat/completions` endpoint.
 *
 * These types are deliberately kept separate from the domain types in
 * `llm/LlmClient.kt`: the domain model should not change shape just because a
 * provider adds a field.
 */
internal data class ChatCompletionRequestDto(
    val model: String,
    val messages: List<ChatMessageDto>,
    val temperature: Double,
)

internal data class ChatMessageDto(
    val role: String,
    val content: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ChatCompletionResponseDto(
    val model: String? = null,
    val choices: List<ChoiceDto> = emptyList(),
    val usage: UsageDto? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ChoiceDto(
    val index: Int = 0,
    val message: ChatMessageDto? = null,
    @JsonProperty("finish_reason") val finishReason: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class UsageDto(
    @JsonProperty("prompt_tokens") val promptTokens: Int = 0,
    @JsonProperty("completion_tokens") val completionTokens: Int = 0,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ErrorEnvelopeDto(
    val error: ErrorDto? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ErrorDto(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null,
)
