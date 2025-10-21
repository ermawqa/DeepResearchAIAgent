class ResearchAgent(private val client: OpenAIClient) {
    fun runResearch(query: String): String {
//       val prompt = PromptBuilder.build(query)
//       return client.askLLM(prompt)
        return client.askLLM(query)
    }
}