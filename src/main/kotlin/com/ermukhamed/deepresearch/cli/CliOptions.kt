package com.ermukhamed.deepresearch.cli

import com.ermukhamed.deepresearch.output.OutputFormat
import java.nio.file.Path

/** Parsed command line, after validation. */
data class CliOptions(
    val query: String? = null,
    val format: OutputFormat = OutputFormat.MARKDOWN,
    val outputPath: Path? = null,
    val model: String? = null,
    val temperature: Double? = null,
    val showHelp: Boolean = false,
    val showVersion: Boolean = false,
)

/** A command line that could not be understood. */
class CliParseException(message: String) : Exception(message)
