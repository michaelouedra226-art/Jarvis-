package com.example.network

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateContent(
        prompt: String,
        history: List<Pair<String, String>> = emptyList(), // Pair of (role, content)
        systemInstruction: String? = null,
        attachedFileBase64: String? = null,
        attachedFileMimeType: String? = null,
        modelName: String = "gemini-2.5-flash",
        customApiKey: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = if (!customApiKey.isNullOrEmpty()) customApiKey else BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Erreur : Clé API Gemini manquante. Veuillez la configurer dans l'onglet Configuration (Paramètres de l'application)."
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        try {
            val root = JSONObject()

            // Contents array
            val contentsArray = JSONArray()

            // 1. History
            for ((role, text) in history) {
                val contentObj = JSONObject()
                contentObj.put("role", if (role == "user") "user" else "model")
                
                val partsArray = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", text)
                partsArray.put(partObj)
                
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
            }

            // 2. Current User Message (prompt + file if any)
            val currentUserContentObj = JSONObject()
            currentUserContentObj.put("role", "user")
            val partsArray = JSONArray()

            // File part if any
            if (attachedFileBase64 != null && attachedFileMimeType != null) {
                val filePart = JSONObject()
                val inlineData = JSONObject()
                inlineData.put("mimeType", attachedFileMimeType)
                inlineData.put("data", attachedFileBase64)
                filePart.put("inlineData", inlineData)
                partsArray.put(filePart)
            }

            // Text part
            val textPart = JSONObject()
            textPart.put("text", prompt)
            partsArray.put(textPart)

            currentUserContentObj.put("parts", partsArray)
            contentsArray.put(currentUserContentObj)

            root.put("contents", contentsArray)

            // System Instruction
            if (systemInstruction != null) {
                val sysInstObj = JSONObject()
                val sysInstParts = JSONArray()
                val sysInstPart = JSONObject()
                sysInstPart.put("text", systemInstruction)
                sysInstParts.put(sysInstPart)
                sysInstObj.put("parts", sysInstParts)
                root.put("systemInstruction", sysInstObj)
            }

            // Generation Config
            val generationConfig = JSONObject()
            generationConfig.put("temperature", 0.7)
            root.put("generationConfig", generationConfig)

            val requestBodyJson = root.toString()
            Log.d(TAG, "Request: $requestBodyJson")

            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "Response Code: ${response.code}, Body: $responseBody")

                if (!response.isSuccessful) {
                    return@withContext "Erreur d'API (${response.code}) : ${response.message}\n$responseBody"
                }

                val responseJson = JSONObject(responseBody)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text")
                            return@withContext text
                        }
                    }
                }
                return@withContext "Désolé, aucune réponse n'a été générée."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating content", e)
            return@withContext "Erreur de connexion : ${e.localizedMessage}"
        }
    }
}
