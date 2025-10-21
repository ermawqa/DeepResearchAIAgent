object Secrets {
    val OPENAI_API_KEY = System.getenv("OPENAI_API_KEY") ?: "MISSING_KEY"
}