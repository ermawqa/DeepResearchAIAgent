package com.ermukhamed.deepresearch

import java.util.Properties

/** Version stamped into `version.properties` by the Gradle build. */
object BuildInfo {
    const val NAME = "DeepResearchAiAgent"

    val version: String by lazy {
        runCatching {
            BuildInfo::class.java.getResourceAsStream("/version.properties")?.use { stream ->
                Properties().apply { load(stream) }.getProperty("version")
            }
        }.getOrNull()?.takeIf { it.isNotBlank() && !it.startsWith("\${") } ?: "dev"
    }
}
