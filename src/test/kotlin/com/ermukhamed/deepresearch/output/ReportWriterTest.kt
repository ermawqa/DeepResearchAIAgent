package com.ermukhamed.deepresearch.output

import com.ermukhamed.deepresearch.agent.ResearchReport
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReportWriterTest {

    private val report = ResearchReport(
        query = "Vector databases",
        content = "## Summary\nStores embeddings for similarity search.",
        model = "gpt-4o-mini",
        generatedAt = Instant.parse("2026-08-02T10:15:30Z"),
        usage = null,
    )

    private fun tempFile(name: String): Path = createTempDirectory("report-writer").resolve(name)

    @Test
    fun `raw format goes to stdout rather than a file`() {
        val stdout = StringBuilder()

        val destination = ReportWriter(stdout).write(report, OutputFormat.RAW)

        assertEquals(ReportWriter.Destination.Stdout, destination)
        assertTrue(stdout.toString().contains("Stores embeddings for similarity search."))
    }

    @Test
    fun `writes to the explicit path when one is given`() {
        val target = tempFile("custom.md")

        val destination = ReportWriter(StringBuilder()).write(report, OutputFormat.MARKDOWN, target)

        assertIs<ReportWriter.Destination.File>(destination)
        assertEquals(target, destination.path)
        assertTrue(target.readText().contains("# Research: Vector databases"))
    }

    @Test
    fun `an explicit path overrides the raw format's stdout default`() {
        val target = tempFile("piped.txt")

        ReportWriter(StringBuilder()).write(report, OutputFormat.RAW, target)

        assertEquals(report.content, target.readText())
    }

    @Test
    fun `creates missing parent directories`() {
        val target = createTempDirectory("report-writer").resolve("nested/deeper/report.json")

        ReportWriter(StringBuilder()).write(report, OutputFormat.JSON, target)

        assertTrue(Files.exists(target))
        assertTrue(target.readText().contains("\"query\""))
    }

    @Test
    fun `overwrites an existing file instead of appending`() {
        val target = tempFile("existing.md")
        Files.createDirectories(target.parent)
        Files.writeString(target, "stale content that must not survive")

        ReportWriter(StringBuilder()).write(report, OutputFormat.MARKDOWN, target)

        assertTrue(!target.readText().contains("stale content"))
    }

    @Test
    fun `file backed formats declare a default file name`() {
        assertEquals("research_output.md", OutputFormat.MARKDOWN.defaultFileName)
        assertEquals("research_output.json", OutputFormat.JSON.defaultFileName)
        assertEquals(null, OutputFormat.RAW.defaultFileName)
    }
}
