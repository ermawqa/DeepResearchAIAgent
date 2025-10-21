class ResearchAgent(private val client: OpenAIClient) {
    fun runResearch(query: String): String {
        return client.askLLM(query)
    }
}