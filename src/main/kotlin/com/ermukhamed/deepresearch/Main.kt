package com.ermukhamed.deepresearch

import com.ermukhamed.deepresearch.agent.ResearchAgent
import com.ermukhamed.deepresearch.cli.ArgParser
import com.ermukhamed.deepresearch.cli.CliParseException
import com.ermukhamed.deepresearch.cli.ExitCode
import com.ermukhamed.deepresearch.config.AppConfig
import com.ermukhamed.deepresearch.config.MissingConfigurationException
import com.ermukhamed.deepresearch.llm.LlmException
import com.ermukhamed.deepresearch.llm.OpenAIClient
import com.ermukhamed.deepresearch.output.ReportWriter
import java.io.IOException
import kotlin.system.exitProcess

/**
 * CLI entry point.
 *
 * Wires the pieces together and translates outcomes into exit codes. All progress
 * chatter goes to stderr so that `--format raw` can be redirected cleanly:
 *
 *     deep-research -q "Vector databases" -f raw > notes.txt
 */
fun main(args: Array<String>) {
    exitProcess(run(args).code)
}

internal fun run(args: Array<String>): ExitCode {
    val options = ArgParser.parse(args).getOrElse { error ->
        return when (error) {
            is CliParseException -> fail(error.message, ExitCode.USAGE, showHelpHint = true)
            else -> fail(error.message, ExitCode.USAGE)
        }
    }

    if (options.showHelp) {
        println(ArgParser.usage)
        return ExitCode.SUCCESS
    }

    if (options.showVersion) {
        println("${BuildInfo.NAME} ${BuildInfo.version}")
        return ExitCode.SUCCESS
    }

    val config = AppConfig.fromEnvironment().getOrElse { error ->
        val code = if (error is MissingConfigurationException) ExitCode.CONFIGURATION else ExitCode.USAGE
        return fail(error.message, code)
    }

    val query = options.query ?: promptForQuery() ?: return fail("No query provided.", ExitCode.USAGE)

    val model = options.model ?: config.model
    val temperature = options.temperature ?: config.temperature

    System.err.println("Researching \"$query\" with $model…")

    val agent = ResearchAgent(
        client = OpenAIClient(config),
        model = model,
        temperature = temperature,
    )

    val report = agent.research(query).getOrElse { error ->
        return when (error) {
            is LlmException -> fail(error.failure.message, ExitCode.forFailure(error.failure))
            else -> fail(error.message, ExitCode.API)
        }
    }

    return try {
        when (val destination = ReportWriter().write(report, options.format, options.outputPath)) {
            is ReportWriter.Destination.File ->
                System.err.println("Saved ${options.format.cliName} report to ${destination.path}")
            ReportWriter.Destination.Stdout -> Unit
        }
        report.usage?.let { System.err.println("Used ${it.totalTokens} tokens.") }
        ExitCode.SUCCESS
    } catch (e: IOException) {
        fail("Could not write output: ${e.message}", ExitCode.IO)
    }
}

/** Reads a query interactively; null when stdin is closed or the input is blank. */
private fun promptForQuery(): String? {
    System.err.print("Enter your research question: ")
    System.err.flush()
    return readlnOrNull()?.trim()?.takeIf { it.isNotEmpty() }
}

private fun fail(message: String?, code: ExitCode, showHelpHint: Boolean = false): ExitCode {
    System.err.println("Error: ${message ?: "unexpected failure"}")
    if (showHelpHint) System.err.println("Run with --help for usage.")
    return code
}
