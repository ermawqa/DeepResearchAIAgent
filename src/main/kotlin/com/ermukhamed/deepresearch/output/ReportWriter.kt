package com.ermukhamed.deepresearch.output

import com.ermukhamed.deepresearch.agent.ResearchReport
import java.nio.file.Files
import java.nio.file.Path

/**
 * Routes a rendered report to its destination: a file for the file-backed
 * formats, stdout for [OutputFormat.RAW] or when no destination is resolvable.
 */
class ReportWriter(private val stdout: Appendable = System.out) {

    /** Where a rendered report ended up, for reporting back to the user. */
    sealed interface Destination {
        data class File(val path: Path) : Destination
        data object Stdout : Destination
    }

    /**
     * Renders [report] as [format] and writes it out.
     *
     * [explicitPath] overrides the format's default file name; passing null for a
     * format with no default sends the output to stdout.
     */
    fun write(
        report: ResearchReport,
        format: OutputFormat,
        explicitPath: Path? = null,
    ): Destination {
        val rendered = ReportRenderer.forFormat(format).render(report)
        val target = explicitPath ?: format.defaultFileName?.let(Path::of)

        if (target == null) {
            stdout.append(rendered).append(System.lineSeparator())
            return Destination.Stdout
        }

        target.toAbsolutePath().parent?.let(Files::createDirectories)
        Files.writeString(target, rendered)
        return Destination.File(target)
    }
}
