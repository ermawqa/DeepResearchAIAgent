import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

import Secrets.OPENAI_API_KEY

class OpenAIClient {
    fun askLLM(prompt: String): String {
        val client = OkHttpClient()

        val mapper = jacksonObjectMapper()
        val requestJson = mapper.writeValueAsString(
            mapOf(
                "model" to "gpt-3.5-turbo",
                "messages" to listOf(
                    mapOf("role" to "user", "content" to prompt)
                )
            )
        )

        val body = requestJson.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $OPENAI_API_KEY")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: return "No response"
//          println("🔴 Raw response: $responseBody")

            val json = mapper.readTree(responseBody)
            val choices = json["choices"]

            if (choices != null && choices.isArray && choices.size() > 0) {
                val content = choices[0]["message"]?.get("content")?.asText()
                return content ?: "⚠️ No content in response."
            } else if (json.has("error")) {
                val error = json["error"]["message"].asText()
                return "❌ OpenAI API error: $error"
            } else {
                return "❌ Unexpected response format."
            }
        }
    }
}