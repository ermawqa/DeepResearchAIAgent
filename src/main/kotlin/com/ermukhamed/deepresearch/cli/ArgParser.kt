package com.ermukhamed.deepresearch.cli

import com.ermukhamed.deepresearch.output.OutputFormat
import java.nio.file.InvalidPathException
import java.nio.file.Path

/**
 * Hand-rolled argument parser.
 *
 * Small enough not to justify a dependency, but strict where a naive parser
 * silently misbehaves: a flag missing its value is an error rather than a null,
 * and `--query --format json` does not quietly research the string "--format".
 */
object ArgParser {

    /** Parses [args], or fails with [CliParseException] describing the problem. */
    fun parse(args: Array<String>): Result<CliOptions> = runCatching { parseOrThrow(args) }

    private fun parseOrThrow(args: Array<String>): CliOptions {
        var options = CliOptions()
        var index = 0

        while (index < args.size) {
            when (val arg = args[index]) {
                "--help", "-h" -> {
                    options = options.copy(showHelp = true)
                    index++
                }

                "--version", "-v" -> {
                    options = options.copy(showVersion = true)
                    index++
                }

                "--query", "-q" -> {
                    options = options.copy(query = requireValue(args, index, arg))
                    index += 2
                }

                "--format", "-f" -> {
                    val raw = requireValue(args, index, arg)
                    val format = OutputFormat.fromCliName(raw)
                        ?: throw CliParseException(
                            "Unknown format '$raw'. Supported formats: ${OutputFormat.supportedNames}.",
                        )
                    options = options.copy(format = format)
                    index += 2
                }

                "--output", "-o" -> {
                    val raw = requireValue(args, index, arg)
                    val path = try {
                        Path.of(raw)
                    } catch (e: InvalidPathException) {
                        throw CliParseException("Invalid output path '$raw': ${e.reason}")
                    }
                    options = options.copy(outputPath = path)
                    index += 2
                }

                "--model", "-m" -> {
                    options = options.copy(model = requireValue(args, index, arg))
                    index += 2
                }

                "--temperature", "-t" -> {
                    val raw = requireValue(args, index, arg)
                    val temperature = raw.toDoubleOrNull()
                        ?: throw CliParseException("--temperature expects a number, got '$raw'.")
                    if (temperature !in 0.0..2.0) {
                        throw CliParseException("--temperature must be between 0.0 and 2.0, got $temperature.")
                    }
                    options = options.copy(temperature = temperature)
                    index += 2
                }

                else -> throw CliParseException(
                    if (arg.startsWith("-")) {
                        "Unknown option '$arg'. Run with --help to see available options."
                    } else {
                        "Unexpected argument '$arg'. Pass the topic with --query \"$arg\"."
                    },
                )
            }
        }

        return options
    }

    /**
     * Reads the value following the flag at [flagIndex].
     *
     * Rejects a missing value, an empty value, and a value that is itself a flag —
     * all three otherwise turn into confusing downstream behaviour.
     */
    private fun requireValue(args: Array<String>, flagIndex: Int, flag: String): String {
        val value = args.getOrNull(flagIndex + 1)
            ?: throw CliParseException("$flag requires a value.")
        if (value.startsWith("-") && value.length > 1) {
            throw CliParseException("$flag requires a value, but was followed by the option '$value'.")
        }
        if (value.isBlank()) {
            throw CliParseException("$flag requires a non-empty value.")
        }
        return value
    }

    /** The `--help` screen. */
    val usage: String = """
        DeepResearchAiAgent — structured research briefings from the command line.

        USAGE
          deep-research [OPTIONS]

        OPTIONS
          -q, --query <text>          Topic to research. Prompted for if omitted.
          -f, --format <format>       Output format: ${OutputFormat.supportedNames} (default: markdown).
          -o, --output <path>         Write to this file instead of the format default.
          -m, --model <name>          Override the model for this run.
          -t, --temperature <0.0-2.0> Sampling temperature (default: 0.3).
          -h, --help                  Show this message.
          -v, --version               Show the version.

        ENVIRONMENT
          OPENAI_API_KEY          Required. Your OpenAI API key.
          OPENAI_MODEL            Default model (default: gpt-4o-mini).
          OPENAI_BASE_URL         API base URL, for proxies and compatible providers.
          OPENAI_TIMEOUT_SECONDS  Per-request timeout (default: 60).

        EXAMPLES
          deep-research --query "Retrieval-augmented generation" --format markdown
          deep-research -q "Kotlin coroutines internals" -f json -o reports/coroutines.json
          deep-research -q "Vector databases" -f raw > notes.txt
    """.trimIndent()
}
