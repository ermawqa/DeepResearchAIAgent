package com.ermukhamed.deepresearch.config

import java.time.Duration

/**
 * Runtime configuration, resolved from the environment.
 *
 * Everything except the API key has a sensible default, so the only thing a user
 * strictly has to set is `OPENAI_API_KEY`.
 */
data class AppConfig(
    val apiKey: String,
    val model: String = DEFAULT_MODEL,
    val baseUrl: String = DEFAULT_BASE_URL,
    val temperature: Double = DEFAULT_TEMPERATURE,
    val requestTimeout: Duration = DEFAULT_TIMEOUT,
    val maxRetries: Int = DEFAULT_MAX_RETRIES,
) {
    init {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }
        require(temperature in 0.0..2.0) { "temperature must be in [0.0, 2.0], was $temperature" }
        require(maxRetries >= 0) { "maxRetries must not be negative, was $maxRetries" }
    }

    companion object {
        const val DEFAULT_MODEL = "gpt-4o-mini"
        const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        const val DEFAULT_TEMPERATURE = 0.3
        const val DEFAULT_MAX_RETRIES = 2
        val DEFAULT_TIMEOUT: Duration = Duration.ofSeconds(60)

        const val ENV_API_KEY = "OPENAI_API_KEY"
        const val ENV_MODEL = "OPENAI_MODEL"
        const val ENV_BASE_URL = "OPENAI_BASE_URL"
        const val ENV_TIMEOUT_SECONDS = "OPENAI_TIMEOUT_SECONDS"

        /**
         * Builds a config from environment variables.
         *
         * The [env] lookup is injected so tests can exercise this without mutating
         * the real process environment.
         */
        fun fromEnvironment(env: (String) -> String? = System::getenv): Result<AppConfig> {
            val apiKey = env(ENV_API_KEY)?.trim()
            if (apiKey.isNullOrBlank()) {
                return Result.failure(
                    MissingConfigurationException(
                        "$ENV_API_KEY is not set. Export it first:\n" +
                            "  export $ENV_API_KEY=\"sk-...\"",
                    ),
                )
            }

            val timeout = env(ENV_TIMEOUT_SECONDS)?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
                val seconds = raw.toLongOrNull()
                if (seconds == null || seconds <= 0) {
                    return Result.failure(
                        MissingConfigurationException("$ENV_TIMEOUT_SECONDS must be a positive integer, was '$raw'"),
                    )
                }
                Duration.ofSeconds(seconds)
            } ?: DEFAULT_TIMEOUT

            return runCatching {
                AppConfig(
                    apiKey = apiKey,
                    model = env(ENV_MODEL)?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_MODEL,
                    baseUrl = env(ENV_BASE_URL)?.trim()?.takeIf { it.isNotEmpty() }?.trimEnd('/') ?: DEFAULT_BASE_URL,
                    requestTimeout = timeout,
                )
            }
        }
    }
}

/** Thrown when required configuration is absent or malformed. */
class MissingConfigurationException(message: String) : Exception(message)
