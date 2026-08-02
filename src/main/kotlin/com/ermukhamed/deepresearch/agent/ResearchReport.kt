package com.ermukhamed.deepresearch.agent

import com.ermukhamed.deepresearch.llm.TokenUsage
import java.time.Instant

/**
 * The agent's output for one query, with the provenance a reader needs to judge
 * it: which model produced it, when, and at what token cost.
 */
data class ResearchReport(
    val query: String,
    val content: String,
    val model: String,
    val generatedAt: Instant,
    val usage: TokenUsage?,
)
