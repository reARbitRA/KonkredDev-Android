package com.example.data.api

import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiPairProgrammer {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun askGemini(prompt: String, systemPrompt: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "AI Pairing Response:\nTo connect to a live Google Gemini Instance, secure your GEMINI_API_KEY inside the Secrets Panel of AI Studio. Running local parsing instead:\n\n```js\n// Offline AI suggestion\nconsole.log(\"Happy coding!\");\n```"
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        // Build request body using Android native JSON
        val requestJson = JSONObject()
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        
        partsArray.put(JSONObject().put("text", prompt))
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        requestJson.put("contents", contentsArray)

        if (systemPrompt != null) {
            val systemInstructionObj = JSONObject()
            val systemPartsArray = JSONArray()
            systemPartsArray.put(JSONObject().put("text", systemPrompt))
            systemInstructionObj.put("parts", systemPartsArray)
            requestJson.put("systemInstruction", systemInstructionObj)
        }

        val requestBodyString = requestJson.toString()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestBodyString.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        JSONObject(bodyString).optJSONObject("error")?.optString("message") ?: "HTTP error code: ${response.code}"
                    } catch (e: Exception) {
                        "HTTP error code: ${response.code}"
                    }
                    return@withContext "Gemini Error: $errorMsg"
                }

                val responseJson = JSONObject(bodyString)
                val candidatesArray = responseJson.optJSONArray("candidates")
                if (candidatesArray != null && candidatesArray.length() > 0) {
                    val firstCandidate = candidatesArray.optJSONObject(0)
                    val contentObjResponse = firstCandidate?.optJSONObject("content")
                    val responsePartsArray = contentObjResponse?.optJSONArray("parts")
                    if (responsePartsArray != null && responsePartsArray.length() > 0) {
                        return@withContext responsePartsArray.optJSONObject(0)?.optString("text") ?: "No valid text"
                    }
                }
                "Response format was unreadable."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Network error connecting to Gemini API: ${e.localizedMessage}. Verify your network connection."
        }
    }
}
