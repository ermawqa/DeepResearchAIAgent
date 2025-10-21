import java.io.File

fun saveToMarkdown(content: String, filename: String = "research_output.md") {
    val file = File(filename)
    file.writeText(content)
}