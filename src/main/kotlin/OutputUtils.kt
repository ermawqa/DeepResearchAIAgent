import java.io.File
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun saveToFile(content: String, filename: String) {
    val file = File(filename)
    file.writeText(content)
    println("✅ Saved output to $filename")
}

fun toJsonFormat(query: String, result: String): String {
    val mapper = jacksonObjectMapper()
    val output = mapOf(
        "query" to query,
        "summary" to result,
        "generated_at" to System.currentTimeMillis()
    )
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(output)
}