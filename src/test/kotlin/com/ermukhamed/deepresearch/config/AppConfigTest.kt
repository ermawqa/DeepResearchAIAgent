package com.ermukhamed.deepresearch.config

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AppConfigTest {

    private fun env(vararg pairs: Pair<String, String>): (String) -> String? {
        val map = pairs.toMap()
        return { map[it] }
    }

    @Test
    fun `fails when the api key is absent`() {
        val error = AppConfig.fromEnvironment(env()).exceptionOrNull()

        assertIs<MissingConfigurationException>(error)
        assertTrue(error.message!!.contains("OPENAI_API_KEY"))
    }

    @Test
    fun `fails when the api key is blank`() {
        val error = AppConfig.fromEnvironment(env("OPENAI_API_KEY" to "   ")).exceptionOrNull()

        assertIs<MissingConfigurationException>(error)
    }

    @Test
    fun `applies defaults when only the api key is set`() {
        val config = AppConfig.fromEnvironment(env("OPENAI_API_KEY" to "sk-test")).getOrThrow()

        assertEquals("sk-test", config.apiKey)
        assertEquals(AppConfig.DEFAULT_MODEL, config.model)
        assertEquals(AppConfig.DEFAULT_BASE_URL, config.baseUrl)
        assertEquals(AppConfig.DEFAULT_TIMEOUT, config.requestTimeout)
    }

    @Test
    fun `overrides defaults from the environment`() {
        val config = AppConfig.fromEnvironment(
            env(
                "OPENAI_API_KEY" to "sk-test",
                "OPENAI_MODEL" to "gpt-4o",
                "OPENAI_BASE_URL" to "https://proxy.internal/v1",
                "OPENAI_TIMEOUT_SECONDS" to "15",
            ),
        ).getOrThrow()

        assertEquals("gpt-4o", config.model)
        assertEquals("https://proxy.internal/v1", config.baseUrl)
        assertEquals(Duration.ofSeconds(15), config.requestTimeout)
    }

    @Test
    fun `trims a trailing slash from the base url so paths do not double up`() {
        val config = AppConfig.fromEnvironment(
            env("OPENAI_API_KEY" to "sk-test", "OPENAI_BASE_URL" to "https://proxy.internal/v1/"),
        ).getOrThrow()

        assertEquals("https://proxy.internal/v1", config.baseUrl)
    }

    @Test
    fun `rejects a non numeric timeout`() {
        val error = AppConfig.fromEnvironment(
            env("OPENAI_API_KEY" to "sk-test", "OPENAI_TIMEOUT_SECONDS" to "soon"),
        ).exceptionOrNull()

        assertIs<MissingConfigurationException>(error)
        assertTrue(error.message!!.contains("positive integer"))
    }

    @Test
    fun `rejects a non positive timeout`() {
        val error = AppConfig.fromEnvironment(
            env("OPENAI_API_KEY" to "sk-test", "OPENAI_TIMEOUT_SECONDS" to "0"),
        ).exceptionOrNull()

        assertIs<MissingConfigurationException>(error)
    }

    @Test
    fun `rejects an out of range temperature`() {
        val error = runCatching { AppConfig(apiKey = "sk-test", temperature = 2.5) }.exceptionOrNull()

        assertIs<IllegalArgumentException>(error)
    }
}
