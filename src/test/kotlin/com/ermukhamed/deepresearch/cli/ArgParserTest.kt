package com.ermukhamed.deepresearch.cli

import com.ermukhamed.deepresearch.output.OutputFormat
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArgParserTest {

    @Test
    fun `defaults to markdown with no query when given no arguments`() {
        val options = ArgParser.parse(emptyArray()).getOrThrow()

        assertNull(options.query)
        assertEquals(OutputFormat.MARKDOWN, options.format)
        assertNull(options.outputPath)
    }

    @Test
    fun `parses long form options`() {
        val options = ArgParser.parse(
            arrayOf("--query", "Vector databases", "--format", "json", "--output", "out/report.json"),
        ).getOrThrow()

        assertEquals("Vector databases", options.query)
        assertEquals(OutputFormat.JSON, options.format)
        assertEquals(Path.of("out/report.json"), options.outputPath)
    }

    @Test
    fun `parses short form options`() {
        val options = ArgParser.parse(
            arrayOf("-q", "Kotlin coroutines", "-f", "raw", "-m", "gpt-4o", "-t", "0.9"),
        ).getOrThrow()

        assertEquals("Kotlin coroutines", options.query)
        assertEquals(OutputFormat.RAW, options.format)
        assertEquals("gpt-4o", options.model)
        assertEquals(0.9, options.temperature)
    }

    @Test
    fun `format matching is case insensitive`() {
        val options = ArgParser.parse(arrayOf("-f", "JSON")).getOrThrow()

        assertEquals(OutputFormat.JSON, options.format)
    }

    @Test
    fun `rejects a flag whose value is missing`() {
        val error = ArgParser.parse(arrayOf("--query")).exceptionOrNull()

        assertIs<CliParseException>(error)
        assertTrue(error.message!!.contains("requires a value"))
    }

    @Test
    fun `rejects a flag followed by another flag instead of a value`() {
        // The naive `args[index + 1]` parser would research the string "--format".
        val error = ArgParser.parse(arrayOf("--query", "--format", "json")).exceptionOrNull()

        assertIs<CliParseException>(error)
        assertTrue(error.message!!.contains("option '--format'"))
    }

    @Test
    fun `rejects an unknown format and names the supported ones`() {
        val error = ArgParser.parse(arrayOf("--format", "pdf")).exceptionOrNull()

        assertIs<CliParseException>(error)
        assertTrue(error.message!!.contains("markdown"))
        assertTrue(error.message!!.contains("json"))
    }

    @Test
    fun `rejects an unknown option`() {
        val error = ArgParser.parse(arrayOf("--verbose")).exceptionOrNull()

        assertIs<CliParseException>(error)
        assertTrue(error.message!!.contains("Unknown option"))
    }

    @Test
    fun `rejects a bare positional argument and suggests the flag`() {
        val error = ArgParser.parse(arrayOf("quantum computing")).exceptionOrNull()

        assertIs<CliParseException>(error)
        assertTrue(error.message!!.contains("--query"))
    }

    @Test
    fun `rejects a non numeric temperature`() {
        val error = ArgParser.parse(arrayOf("-t", "warm")).exceptionOrNull()

        assertIs<CliParseException>(error)
        assertTrue(error.message!!.contains("expects a number"))
    }

    @Test
    fun `rejects a temperature outside the valid range`() {
        val error = ArgParser.parse(arrayOf("--temperature", "3.5")).exceptionOrNull()

        assertIs<CliParseException>(error)
        assertTrue(error.message!!.contains("between 0.0 and 2.0"))
    }

    @Test
    fun `recognises help and version flags`() {
        assertTrue(ArgParser.parse(arrayOf("--help")).getOrThrow().showHelp)
        assertTrue(ArgParser.parse(arrayOf("-h")).getOrThrow().showHelp)
        assertTrue(ArgParser.parse(arrayOf("--version")).getOrThrow().showVersion)
        assertTrue(ArgParser.parse(arrayOf("-v")).getOrThrow().showVersion)
    }

    @Test
    fun `later occurrences of a flag win`() {
        val options = ArgParser.parse(arrayOf("-f", "json", "-f", "raw")).getOrThrow()

        assertEquals(OutputFormat.RAW, options.format)
    }

    @Test
    fun `usage text lists every supported format`() {
        OutputFormat.entries.forEach { format ->
            assertTrue(
                ArgParser.usage.contains(format.cliName),
                "usage text should mention '${format.cliName}'",
            )
        }
    }
}
