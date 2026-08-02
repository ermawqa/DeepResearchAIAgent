package com.ermukhamed.deepresearch.output

import com.ermukhamed.deepresearch.agent.ResearchReport
import com.ermukhamed.deepresearch.llm.TokenUsage
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReportRendererTest {

    private val mapper = jacksonObjectMapper()

    private val report = ResearchReport(
        query = "Retrieval-augmented generation",
        content = "## Summary\nRAG grounds an LLM in retrieved documents.",
        model = "gpt-4o-mini",
        generatedAt = Instant.parse("2026-08-02T10:15:30Z"),
        usage = TokenUsage(promptTokens = 42, completionTokens = 108),
    )

    @Test
    fun `raw renderer emits the model text untouched`() {
        assertEquals(report.content, RawRenderer.render(report))
    }

    @Test
    fun `markdown renderer adds a title body and provenance footer`() {
        val rendered = MarkdownRenderer.render(report)

        assertTrue(rendered.startsWith("# Research: Retrieval-augmented generation"))
        assertTrue(rendered.contains("RAG grounds an LLM in retrieved documents."))
        assertTrue(rendered.contains("gpt-4o-mini"))
        assertTrue(rendered.contains("2026-08-02T10:15:30Z"))
        assertTrue(rendered.contains("150 total"))
    }

    @Test
    fun `markdown renderer omits the token line when usage is unknown`() {
        val rendered = MarkdownRenderer.render(report.copy(usage = null))

        assertFalse(rendered.contains("Tokens:"))
        assertTrue(rendered.contains("gpt-4o-mini"))
    }

    @Test
    fun `json renderer emits a stable parseable record`() {
        val parsed = mapper.readTree(JsonRenderer.render(report))

        assertEquals("Retrieval-augmented generation", parsed["query"].asText())
        assertEquals("gpt-4o-mini", parsed["model"].asText())
        assertEquals("2026-08-02T10:15:30Z", parsed["generated_at"].asText())
        assertEquals(report.content, parsed["content"].asText())
        assertEquals(42, parsed["usage"]["prompt_tokens"].asInt())
        assertEquals(150, parsed["usage"]["total_tokens"].asInt())
    }

    @Test
    fun `json renderer emits a null usage rather than dropping the key`() {
        val parsed = mapper.readTree(JsonRenderer.render(report.copy(usage = null)))

        assertTrue(parsed.has("usage"))
        assertTrue(parsed["usage"].isNull)
    }

    @Test
    fun `json renderer escapes content that would otherwise break the document`() {
        val awkward = report.copy(content = "He said \"hello\"\nand a backslash \\ too")

        val parsed = mapper.readTree(JsonRenderer.render(awkward))

        assertEquals(awkward.content, parsed["content"].asText())
    }

    @Test
    fun `every format resolves to a renderer`() {
        OutputFormat.entries.forEach { format ->
            assertTrue(ReportRenderer.forFormat(format).render(report).isNotEmpty())
        }
    }
}
