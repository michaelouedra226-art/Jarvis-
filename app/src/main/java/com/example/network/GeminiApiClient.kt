package com.example.network

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        modelName: String = "gemini-3.5-flash",
        customApiKey: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = if (!customApiKey.isNullOrEmpty()) customApiKey else BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Erreur : Clé API Gemini manquante. Veuillez la configurer dans l'onglet Configuration (Paramètres de l'application)."
        }

        // Setup fallbacks for models if they hit 503 / 429
        val modelsToTry = mutableListOf<String>()
        modelsToTry.add(modelName)
        if (modelName == "gemini-3.5-flash") {
            modelsToTry.add("gemini-flash-latest")
            modelsToTry.add("gemini-3.1-flash-lite-preview")
        } else if (modelName == "gemini-3.1-pro-preview") {
            modelsToTry.add("gemini-3.5-flash")
            modelsToTry.add("gemini-flash-latest")
        }

        var lastError = ""

        for (currentModel in modelsToTry) {
            var attempt = 0
            val maxAttempts = 3
            while (attempt < maxAttempts) {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$currentModel:generateContent?key=$apiKey"
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
                    Log.d(TAG, "Request (Model: $currentModel, Attempt: $attempt): $requestBodyJson")

                    val request = Request.Builder()
                        .url(url)
                        .post(requestBodyJson.toRequestBody(mediaType))
                        .build()

                    client.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string() ?: ""
                        Log.d(TAG, "Response Code: ${response.code}, Body: $responseBody")

                        if (response.isSuccessful) {
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
                        } else {
                            lastError = "Erreur d'API (${response.code}) : ${response.message}\n$responseBody"
                            Log.w(TAG, "Attempt $attempt failed for model $currentModel with code ${response.code}")
                            
                            // If it's a server overloaded (503), rate limit (429), or standard server issue (5xx), we retry
                            if (response.code == 503 || response.code == 429 || response.code >= 500) {
                                attempt++
                                if (attempt < maxAttempts) {
                                    val delayTime = 1000L * attempt
                                    Log.i(TAG, "Retrying $currentModel in ${delayTime}ms...")
                                    delay(delayTime)
                                    continue
                                }
                            } else {
                                // Non-retryable client error (like 400, 403, 404), break to fallback
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    lastError = "Erreur de connexion : ${e.localizedMessage}"
                    Log.e(TAG, "Exception on attempt $attempt for model $currentModel", e)
                    attempt++
                    if (attempt < maxAttempts) {
                        delay(1000L * attempt)
                    }
                }
            }
            Log.w(TAG, "Model $currentModel failed completely. Trying next fallback model if available.")
        }

        return@withContext "Désolé, Jarvis a rencontré des difficultés de connexion avec l'API après plusieurs essais.\n\nDétails de l'erreur :\n$lastError"
    }
}
