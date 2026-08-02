package com.ermukhamed.deepresearch.output

/** The output shapes the CLI can emit, and where each one lands by default. */
enum class OutputFormat(
    val cliName: String,
    val defaultFileName: String?,
) {
    /** Report plus provenance header, written to a `.md` file. */
    MARKDOWN("markdown", "research_output.md"),

    /** Machine-readable record, written to a `.json` file. */
    JSON("json", "research_output.json"),

    /** The model's text, printed to stdout so it can be piped. */
    RAW("raw", null),
    ;

    companion object {
        /** Case-insensitive lookup by [cliName]; null when [value] matches nothing. */
        fun fromCliName(value: String): OutputFormat? =
            entries.firstOrNull { it.cliName.equals(value.trim(), ignoreCase = true) }

        /** Human-readable list for help text and error messages. */
        val supportedNames: String get() = entries.joinToString(", ") { it.cliName }
    }
}
