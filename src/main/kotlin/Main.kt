fun main(args: Array<String>) {
    val client = OpenAIClient()
    val agent = ResearchAgent(client)

    var query: String? = null
    var format = "markdown"

    // simple manual CLI argument parsingg
    args.forEachIndexed { index, arg ->
        when (arg) {
            "--query" -> query = args.getOrNull(index + 1)
            "--format" -> format = args.getOrNull(index + 1) ?: "markdown"
        }
    }

    if (query == null) {
        println("❓ No query provided. Please enter your research question:")
        query = readlnOrNull()
    }

    if (query.isNullOrBlank()) {
        println("⚠️ No input received. Exiting.")
        return
    }

    println("🔍 Running research on: \"$query\" (format: $format)")

    val result = agent.runResearch(query!!)

    when (format.lowercase()) {
        "markdown" -> saveToFile(result, "research_output.md")
        "json" -> saveToFile(toJsonFormat(query!!, result), "research_output.json")
        "raw" -> println(result)
        else -> {
            println("⚠️ Unknown format '$format'. Showing raw output:")
            println(result)
        }
    }
}