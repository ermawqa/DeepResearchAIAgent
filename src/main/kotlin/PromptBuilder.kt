object PromptBuilder {
    fun build(query: String): String {
        return """
            You are a research assistant. Summarize the current academic and technical knowledge about:
            "$query"
            
            Your output should include:
            - Key concepts
            - Main research directions
            - Relevant tools or frameworks
            - Potential challenges and open questions
            Return the result in Markdown format.
        """.trimIndent()
    }
}