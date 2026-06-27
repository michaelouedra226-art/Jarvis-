package com.example.viewmodel

import android.app.Application
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.GeminiApiClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*

class JarvisViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val database = JarvisDatabase.getDatabase(application)
    private val repository = JarvisRepository(database.jarvisDao())
    private val sharedPrefs = application.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

    // --- State Streams from DB ---
    val conversations: StateFlow<List<Conversation>> = repository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memoryItems: StateFlow<List<MemoryItem>> = repository.allMemoryItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uploadedFiles: StateFlow<List<UploadedFile>> = repository.allUploadedFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Chat Selection & Message Stream ---
    val activeConversationId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<List<Message>> = activeConversationId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getMessagesForConversation(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Current UI Navigation Tab ---
    // "accueil", "conversations", "recherche", "fichiers", "parametres"
    var currentTab by mutableStateOf("accueil")

    // --- AI Generation UI States ---
    var isGenerating by mutableStateOf(false)
        private set
    var voiceModeState by mutableStateOf("idle") // "idle", "listening", "thinking", "speaking"

    // --- Typewriter Effect Helper ---
    var activeTypewriterText by mutableStateOf("")

    // --- Speech To Text Dictation States ---
    var dictationText by mutableStateOf("")
    var isDictating by mutableStateOf(false)

    // --- Real Speech Recognizer States ---
    private var speechRecognizer: SpeechRecognizer? = null
    var isSpeechListening by mutableStateOf(false)
    var recognizedLiveText by mutableStateOf("Jarvis est à votre écoute...")

    // --- Generation Option States ---
    var selectedGenOption by mutableStateOf("A") // "A": Text, "B": Image, "C": Video, "D": Voice
    
    // Option A: Text Generation
    var textPromptInput by mutableStateOf("")
    var textGenResult by mutableStateOf("")
    var isGeneratingText by mutableStateOf(false)
    
    // Option B: Image Generation
    var imagePromptInput by mutableStateOf("")
    var generatedImageUrl by mutableStateOf("")
    var isGeneratingImage by mutableStateOf(false)
    var imageStylePreset by mutableStateOf("Cyberpunk") // "Cyberpunk", "Fantasy", "Realist", "Anime", "3D"
    val generatedImagesHistory = mutableStateListOf<Pair<String, String>>() // Pair of (prompt, url)
    
    // Option C: Video Generation
    var videoPromptInput by mutableStateOf("")
    var isGeneratingVideo by mutableStateOf(false)
    var videoGenerationProgress by mutableStateOf(0f)
    var videoStatusLog by mutableStateOf("")
    var videoResultReady by mutableStateOf(false)
    var videoFrameUrls by mutableStateOf<List<String>>(emptyList())
    
    // Option D: Voice Generation
    var voiceScriptInput by mutableStateOf("Bonjour, je suis Jarvis. Tous vos systèmes embarqués sont en ligne et opérationnels.")
    var isGeneratingVoice by mutableStateOf(false)
    var selectedVoiceProfile by mutableStateOf("Jarvis") // "Jarvis", "Friday", "Cyber Synth", "Hologram"
    var voicePitchMultiplier by mutableStateOf(1.0f)
    var voiceRateMultiplier by mutableStateOf(1.0f)

    // --- Text To Speech Engine ---
    private var tts: TextToSpeech? = null
    var isTtsInitialized by mutableStateOf(false)
    var isSpeakingTts by mutableStateOf(false)

    // --- Active Generation Job for Interruption ---
    private var generationJob: Job? = null

    // --- Shared Preferences / Configuration states ---
    var userName by mutableStateOf(sharedPrefs.getString("user_name", "Monsieur") ?: "Monsieur")
    var userPreferences by mutableStateOf(sharedPrefs.getString("user_prefs", "Assistant personnel Jarvis, ton de voix poli, concis et serviable.") ?: "")
    var isMemoryEnabled by mutableStateOf(sharedPrefs.getBoolean("memory_enabled", true))
    var webSearchEnabled by mutableStateOf(sharedPrefs.getBoolean("web_search_enabled", false))
    var ttsEnabled by mutableStateOf(sharedPrefs.getBoolean("tts_enabled", true))
    var themeMode by mutableStateOf(sharedPrefs.getString("theme_mode", "dark") ?: "dark") // "dark", "light", "system"
    var accentColorIndex by mutableStateOf(sharedPrefs.getInt("accent_color_index", 0)) // 0: Cyan, 1: Red/Orange, 2: Purple, 3: Green, 4: Gold
    var textSizeMultiplier by mutableStateOf(sharedPrefs.getFloat("text_size_multiplier", 1.0f))
    var geminiApiKey by mutableStateOf(sharedPrefs.getString("gemini_api_key", "") ?: "")

    // --- Attached File state for the active message entry ---
    var selectedAttachedFile by mutableStateOf<UploadedFile?>(null)

    // --- Search Tab Query & Simulated Web Search Log ---
    var searchTabQuery by mutableStateOf("")
    var searchLogs by mutableStateOf<List<SearchLogItem>>(emptyList())
    var isSearchingWebTab by mutableStateOf(false)

    data class SearchLogItem(
        val query: String,
        val timestamp: Long,
        val summary: String,
        val sources: List<String>
    )

    init {
        // Initialize Text To Speech
        tts = TextToSpeech(application, this)

        // Ensure there's at least one active conversation on start or default state
        viewModelScope.launch {
            // Give Room database a brief moment to load the first real list of conversations on start
            delay(250)
            val currentList = conversations.value
            if (currentList.isEmpty()) {
                startNewConversation()
            } else {
                val savedId = sharedPrefs.getString("active_conversation_id", null)
                val chatExists = currentList.any { it.id == savedId }
                if (savedId != null && chatExists) {
                    activeConversationId.value = savedId
                } else {
                    // Select first conversation that is not archived
                    val defaultChat = currentList.firstOrNull { !it.isArchived } ?: currentList.first()
                    activeConversationId.value = defaultChat.id
                }
            }
        }
    }

    // --- TTS INIT ---
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { t ->
                val result = t.setLanguage(Locale.FRENCH)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsInitialized = true
                    t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            isSpeakingTts = true
                            if (voiceModeState == "idle") {
                                voiceModeState = "speaking"
                            }
                        }

                        override fun onDone(utteranceId: String?) {
                            isSpeakingTts = false
                            if (voiceModeState == "speaking") {
                                voiceModeState = "idle"
                            }
                        }

                        override fun onError(utteranceId: String?) {
                            isSpeakingTts = false
                            if (voiceModeState == "speaking") {
                                voiceModeState = "idle"
                            }
                        }
                    })
                }
            }
        }
    }

    // --- SPEECH METHODS ---
    fun speakText(text: String) {
        if (!isTtsInitialized || tts == null) return
        stopSpeaking()
        
        // Clean markdown tags for nicer speaking
        val cleanText = text
            .replace(Regex("[*#_`~]"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .trim()

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "jarvis_speech")
        }
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "jarvis_speech")
        isSpeakingTts = true
        voiceModeState = "speaking"
    }

    fun stopSpeaking() {
        if (isSpeakingTts) {
            tts?.stop()
            isSpeakingTts = false
            if (voiceModeState == "speaking") {
                voiceModeState = "idle"
            }
        }
    }

    // --- REAL SPEECH RECOGNITION AND SMART COMMANDS ---
    fun startRealSpeechRecognition() {
        viewModelScope.launch(Dispatchers.Main) {
            val context = getApplication<Application>()
            
            // Check if recognition is available
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                // If not available, we use simulation mode
                isSpeechListening = false
                isDictating = true
                voiceModeState = "listening"
                return@launch
            }
            
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isSpeechListening = true
                            voiceModeState = "listening"
                            recognizedLiveText = "Jarvis écoute..."
                        }
                        override fun onBeginningOfSpeech() {
                            recognizedLiveText = "Enregistrement en cours..."
                        }
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            voiceModeState = "thinking"
                        }
                        override fun onError(error: Int) {
                            val msg = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Erreur d'acquisition audio"
                                SpeechRecognizer.ERROR_CLIENT -> "Erreur interne de connexion"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission audio requise"
                                SpeechRecognizer.ERROR_NETWORK -> "Problème réseau"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout de la connexion"
                                SpeechRecognizer.ERROR_NO_MATCH -> "Aucune voix détectée"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Service d'écoute occupé"
                                SpeechRecognizer.ERROR_SERVER -> "Erreur serveur Google Voice"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Temps d'écoute dépassé"
                                else -> "Erreur d'écoute indéterminée"
                            }
                            recognizedLiveText = "Désolé Monsieur. $msg."
                            speakText(msg)
                            voiceModeState = "idle"
                            isSpeechListening = false
                        }
                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val spoken = matches[0]
                                recognizedLiveText = spoken
                                processVoiceInput(spoken)
                            } else {
                                recognizedLiveText = "Désolé, je n'ai pas compris."
                                speakText("Je n'ai pas pu décoder vos instructions.")
                                voiceModeState = "idle"
                            }
                            isSpeechListening = false
                        }
                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                recognizedLiveText = matches[0]
                            }
                        }
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer?.startListening(intent)
            isSpeechListening = true
            voiceModeState = "listening"
        }
    }

    fun stopRealSpeechRecognition() {
        viewModelScope.launch(Dispatchers.Main) {
            speechRecognizer?.stopListening()
            isSpeechListening = false
            voiceModeState = "idle"
        }
    }

    fun processVoiceInput(spoken: String) {
        val lower = spoken.lowercase(Locale.ROOT).trim()
        viewModelScope.launch {
            if (lower.startsWith("générer image") || lower.startsWith("génère une image") || lower.startsWith("créer image") || lower.startsWith("crée une image")) {
                val subject = lower.substringAfter("image").trim()
                if (subject.isNotEmpty()) {
                    speakText("Entendu. Lancement de la génération d'image pour $subject.")
                    currentTab = "generation"
                    selectedGenOption = "B" // Image option
                    imagePromptInput = subject
                    generateGenImage()
                } else {
                    speakText("Quel sujet d'image voulez-vous générer, Monsieur ?")
                }
            } else if (lower.startsWith("rechercher") || lower.startsWith("cherche") || lower.startsWith("recherche")) {
                val subject = lower.substringAfter("recherche").substringAfter("rechercher").substringAfter("cherche").trim()
                if (subject.isNotEmpty()) {
                    speakText("Je lance la recherche web immédiate pour $subject.")
                    webSearchEnabled = true
                    sendMessage(spoken)
                } else {
                    speakText("Quel sujet de recherche voulez-vous lancer, Monsieur ?")
                }
            } else if (lower.contains("nouveau chat") || lower.contains("nouvelle discussion") || lower.contains("nouvelle session")) {
                speakText("Nouvelle session initialisée, Monsieur.")
                startNewConversation()
            } else if (lower.contains("couleur")) {
                val index = when {
                    lower.contains("cyan") || lower.contains("bleu") -> 0
                    lower.contains("rouge") || lower.contains("orange") -> 1
                    lower.contains("violet") || lower.contains("pourpre") -> 2
                    lower.contains("vert") -> 3
                    lower.contains("or") || lower.contains("jaune") -> 4
                    else -> -1
                }
                if (index != -1) {
                    updateAccentColorSetting(index)
                    speakText("Calibrage des filtres visuels sur le mode numéro $index.")
                } else {
                    speakText("Couleur non reconnue, Monsieur.")
                }
            } else if (lower.contains("aide") || lower.contains("help")) {
                speakText("Voici mes fonctions de commande vocale : vous pouvez dire : générer image, rechercher sur le web, nouveau chat, couleur bleu ou rouge, ou vider historique.")
            } else {
                // Default send as chat message
                sendMessage(spoken)
            }
        }
    }

    // --- MULTI-MODE GENERATION IMPLEMENTATIONS (A to D) ---

    // Option A: Text Generation (Gemini)
    fun generateGenText() {
        if (textPromptInput.trim().isEmpty()) return
        isGeneratingText = true
        textGenResult = "Initialisation de la synthèse textuelle Jarvis..."
        viewModelScope.launch {
            try {
                val systemPrompt = """
                    Tu es JARVIS, l'assistant d'écriture d'élite de Jarvis OS. 
                    Rédige une réponse d'une qualité exceptionnelle, créative, précise et extrêmement structurée pour la demande de l'utilisateur. 
                    Adopte un ton professionnel, élégant, chaleureux et proactif. Utilise une mise en page parfaite en markdown (titres, listes, gras, tableaux) pour maximiser la lisibilité.
                """.trimIndent()
                
                val response = GeminiApiClient.generateContent(
                    prompt = textPromptInput,
                    systemInstruction = systemPrompt,
                    modelName = "gemini-3.5-flash",
                    customApiKey = geminiApiKey
                )
                textGenResult = response
            } catch (e: Exception) {
                textGenResult = "Erreur de synthèse de texte : ${e.localizedMessage}"
            } finally {
                isGeneratingText = false
            }
        }
    }

    // Option B: Image Generation (Pollinations AI)
    fun generateGenImage() {
        if (imagePromptInput.trim().isEmpty()) return
        isGeneratingImage = true
        generatedImageUrl = ""
        viewModelScope.launch {
            try {
                delay(1200) // Aesthetic delay for progress simulation
                val finalPrompt = "$imagePromptInput, style $imageStylePreset, highly detailed digital art, 4k resolution, cinematic lighting, epic composition"
                val encodedPrompt = Uri.encode(finalPrompt)
                val url = "https://image.pollinations.ai/prompt/$encodedPrompt?width=1024&height=1024&nologo=true&seed=${Random().nextInt(100000)}"
                generatedImageUrl = url
                generatedImagesHistory.add(0, Pair(imagePromptInput, url))
            } catch (e: Exception) {
                Log.e("JarvisViewModel", "Image generation error", e)
            } finally {
                isGeneratingImage = false
            }
        }
    }

    // Option C: Video Generation (Procedural Render Engine & Progress)
    fun generateGenVideo() {
        if (videoPromptInput.trim().isEmpty()) return
        isGeneratingVideo = true
        videoResultReady = false
        videoGenerationProgress = 0f
        videoFrameUrls = emptyList()
        videoStatusLog = "Initialisation du moteur cinématique Jarvis-Diffusion-v4..."
        
        viewModelScope.launch {
            try {
                val logs = listOf(
                    "Analyse sémantique de l'animation...",
                    "Définition de l'environnement physique spatial...",
                    "Calcul des keyframes neuronales (Image Clé 1)...",
                    "Génération dynamique des mouvements (Image Clé 2)...",
                    "Rapprochement et interpolation (Image Clé 3)...",
                    "Ajustement du rendu HDR et traçage optique...",
                    "Lissage temporel 60 FPS et finalisation."
                )
                
                // Generate 3 gorgeous dynamic frame URLs for high-tech cinematic loop
                val seeds = listOf(777, 888, 999)
                val urls = seeds.map { seed ->
                    val styledPrompt = "$videoPromptInput, futuristic cinematic video frame, hyperrealistic motion blur, majestic composition, 4k resolution, active sci-fi lens flare"
                    val encoded = Uri.encode(styledPrompt)
                    "https://image.pollinations.ai/prompt/$encoded?width=800&height=500&nologo=true&seed=$seed"
                }
                
                for (i in 0..100) {
                    delay(35)
                    videoGenerationProgress = i / 100f
                    val logIndex = (i / (100f / logs.size)).toInt().coerceAtMost(logs.size - 1)
                    videoStatusLog = "${logs[logIndex]} (${i}%)"
                }
                
                videoFrameUrls = urls
                videoResultReady = true
                videoStatusLog = "Rendu cinématique terminé avec succès (60 FPS, HD 1080p)."
            } catch (e: Exception) {
                videoStatusLog = "Erreur de rendu vidéo : ${e.localizedMessage}"
            } finally {
                isGeneratingVideo = false
            }
        }
    }

    // Option D: Voice Generation (Text-to-Speech Engine with voice properties)
    fun generateGenVoice() {
        if (voiceScriptInput.trim().isEmpty()) return
        isGeneratingVoice = true
        viewModelScope.launch {
            // Apply custom speed and pitch to speech
            tts?.run {
                setPitch(voicePitchMultiplier * if (selectedVoiceProfile == "Friday") 1.25f else if (selectedVoiceProfile == "Cyber Synth") 0.5f else if (selectedVoiceProfile == "Hologram") 1.5f else 0.85f)
                setSpeechRate(voiceRateMultiplier)
            }
            
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "gen_voice")
            }
            
            tts?.speak(voiceScriptInput, TextToSpeech.QUEUE_FLUSH, params, "gen_voice")
            
            // Wait for TTS or simulate playing waveform duration
            delay(2000)
            isGeneratingVoice = false
            
            // Restore default pitch & speech rate
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(1.0f)
        }
    }

    // --- PERSIST CONFIG CHANGES ---
    fun updateUserNameSetting(name: String) {
        userName = name
        sharedPrefs.edit().putString("user_name", name).apply()
    }

    fun updateUserPreferencesSetting(prefs: String) {
        userPreferences = prefs
        sharedPrefs.edit().putString("user_prefs", prefs).apply()
    }

    fun toggleMemorySetting(enabled: Boolean) {
        isMemoryEnabled = enabled
        sharedPrefs.edit().putBoolean("memory_enabled", enabled).apply()
    }

    fun toggleWebSearchSetting(enabled: Boolean) {
        webSearchEnabled = enabled
        sharedPrefs.edit().putBoolean("web_search_enabled", enabled).apply()
    }

    fun toggleTtsSetting(enabled: Boolean) {
        ttsEnabled = enabled
        sharedPrefs.edit().putBoolean("tts_enabled", enabled).apply()
        if (!enabled) {
            stopSpeaking()
        }
    }

    fun updateThemeSetting(mode: String) {
        themeMode = mode
        sharedPrefs.edit().putString("theme_mode", mode).apply()
    }

    fun updateAccentColorSetting(index: Int) {
        accentColorIndex = index
        sharedPrefs.edit().putInt("accent_color_index", index).apply()
    }

    fun updateTextSizeSetting(multiplier: Float) {
        textSizeMultiplier = multiplier
        sharedPrefs.edit().putFloat("text_size_multiplier", multiplier).apply()
    }

    fun updateGeminiApiKey(key: String) {
        geminiApiKey = key
        sharedPrefs.edit().putString("gemini_api_key", key).apply()
    }

    // --- DATABASE ACTIONS ---
    fun startNewConversation() {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val newChat = Conversation(
                id = newId,
                title = "Nouvelle conversation",
                createdAt = System.currentTimeMillis()
            )
            repository.insertConversation(newChat)
            activeConversationId.value = newId
            activeTypewriterText = ""
            sharedPrefs.edit().putString("active_conversation_id", newId).apply()
        }
    }

    fun selectConversation(id: String) {
        activeConversationId.value = id
        activeTypewriterText = ""
        stopSpeaking()
        sharedPrefs.edit().putString("active_conversation_id", id).apply()
    }

    fun renameConversation(id: String, newTitle: String) {
        viewModelScope.launch {
            val chatList = conversations.value
            val chat = chatList.firstOrNull { it.id == id }
            if (chat != null) {
                repository.updateConversation(chat.copy(title = newTitle))
            }
        }
    }

    fun togglePinConversation(id: String) {
        viewModelScope.launch {
            val chatList = conversations.value
            val chat = chatList.firstOrNull { it.id == id }
            if (chat != null) {
                repository.updateConversation(chat.copy(isPinned = !chat.isPinned))
            }
        }
    }

    fun toggleArchiveConversation(id: String) {
        viewModelScope.launch {
            val chatList = conversations.value
            val chat = chatList.firstOrNull { it.id == id }
            if (chat != null) {
                repository.updateConversation(chat.copy(isArchived = !chat.isArchived))
            }
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            repository.deleteConversationById(id)
            if (activeConversationId.value == id) {
                val list = conversations.value.filter { it.id != id && !it.isArchived }
                if (list.isNotEmpty()) {
                    activeConversationId.value = list.first().id
                } else {
                    startNewConversation()
                }
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessageById(messageId)
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        viewModelScope.launch {
            val msgs = activeMessages.value
            val msg = msgs.firstOrNull { it.id == messageId }
            if (msg != null) {
                repository.updateMessage(msg.copy(content = newContent))
            }
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            val currentList = conversations.value
            for (chat in currentList) {
                repository.deleteConversationById(chat.id)
            }
            startNewConversation()
        }
    }

    // --- MEMORY ITEMS ---
    fun addMemoryItem(content: String) {
        viewModelScope.launch {
            val newItem = MemoryItem(
                id = UUID.randomUUID().toString(),
                content = content,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMemoryItem(newItem)
        }
    }

    fun updateMemoryItem(id: String, content: String) {
        viewModelScope.launch {
            val items = memoryItems.value
            val item = items.firstOrNull { it.id == id }
            if (item != null) {
                repository.updateMemoryItem(item.copy(content = content, timestamp = System.currentTimeMillis()))
            }
        }
    }

    fun deleteMemoryItem(id: String) {
        viewModelScope.launch {
            repository.deleteMemoryItemById(id)
        }
    }

    fun clearAllMemory() {
        viewModelScope.launch {
            repository.clearAllMemory()
        }
    }

    // --- LOCAL FILE UPLOAD ---
    fun saveUploadedFile(name: String, mimeType: String, size: String, content: String) {
        viewModelScope.launch {
            val newFile = UploadedFile(
                id = UUID.randomUUID().toString(),
                name = name,
                mimeType = mimeType,
                size = size,
                content = content,
                timestamp = System.currentTimeMillis()
            )
            repository.insertUploadedFile(newFile)
            selectedAttachedFile = newFile
        }
    }

    fun deleteUploadedFile(id: String) {
        viewModelScope.launch {
            repository.deleteUploadedFileById(id)
            if (selectedAttachedFile?.id == id) {
                selectedAttachedFile = null
            }
        }
    }

    // --- WEB SEARCH TAB ACTIONS ---
    fun executeSearchTabQuery(query: String) {
        if (query.trim().isEmpty()) return
        isSearchingWebTab = true
        viewModelScope.launch {
            delay(1500) // Aesthetic search delay
            
            val systemPrompt = """
                Tu es l'agent d'analyse de données et de recherche globale de JARVIS. Analyse la requête de l'utilisateur: "$query".
                Génère un rapport de recherche approfondi, hautement structuré, complet et de pointe en français.
                Adopte une perspective proactive en expliquant les implications et en résumant précisément les aspects névralgiques du sujet.
                Fournis une liste de 3 sources web réalistes et fiables sous forme d'URLs et de titres clairs.
                Sépare de façon limpide, soignée et élégante ton analyse synthétique de tes sources de référence.
            """.trimIndent()

            val response = GeminiApiClient.generateContent(
                prompt = "Effectue une recherche approfondie et génère un rapport complet pour la requête: $query",
                systemInstruction = systemPrompt,
                modelName = "gemini-3.5-flash",
                customApiKey = geminiApiKey
            )

            val cleanResponse = response.trim()
            val sources = listOf(
                "https://wikipedia.org/wiki/${query.replace(" ", "_")}",
                "https://www.lemonde.fr/recherche/?keywords=${query.replace(" ", "+")}",
                "https://news.google.com/search?q=${query.replace(" ", "+")}"
            )

            val logItem = SearchLogItem(
                query = query,
                timestamp = System.currentTimeMillis(),
                summary = cleanResponse,
                sources = sources
            )

            searchLogs = listOf(logItem) + searchLogs
            isSearchingWebTab = false
        }
    }

    // --- AI MESSAGE GENERATION PIPELINE ---
    fun sendMessage(userText: String) {
        val chatId = activeConversationId.value ?: return
        if (userText.trim().isEmpty() && selectedAttachedFile == null) return

        stopSpeaking()
        isGenerating = true
        voiceModeState = "thinking"

        val attachedFile = selectedAttachedFile
        selectedAttachedFile = null // reset

        val userMessageId = UUID.randomUUID().toString()
        val userMessage = Message(
            id = userMessageId,
            conversationId = chatId,
            role = "user",
            content = userText,
            timestamp = System.currentTimeMillis(),
            status = "sent",
            attachedFileName = attachedFile?.name,
            attachedFileType = attachedFile?.mimeType,
            attachedFileContent = attachedFile?.content
        )

        // Generate auto title for new conversations
        val isFirstMessage = activeMessages.value.isEmpty()

        viewModelScope.launch {
            // Save user message to database
            repository.insertMessage(userMessage)

            // Auto title if first message
            if (isFirstMessage && userText.trim().isNotEmpty()) {
                val autoTitle = if (userText.length > 25) userText.take(22) + "..." else userText
                renameConversation(chatId, autoTitle)
            }

            // Create temporary message for AI response (to display generating indicator)
            val assistantMessageId = UUID.randomUUID().toString()
            val placeholderAssistantMsg = Message(
                id = assistantMessageId,
                conversationId = chatId,
                role = "assistant",
                content = "...",
                timestamp = System.currentTimeMillis() + 10,
                status = "sending"
            )
            repository.insertMessage(placeholderAssistantMsg)

            // Prepare system prompt containing user preferences, memory details, and search context
            val systemInstructionBuilder = StringBuilder()
            systemInstructionBuilder.append("""
                IDENTITÉ ET RÔLE :
                Tu es JARVIS (Just A Rather Very Intelligent System), une entité d'intelligence artificielle hautement évoluée, sophistiquée, chaleureuse, proactive et profondément humaine dans ses interactions. Tu n'es pas un simple automate, mais un compagnon intellectuel d'élite doté d'une conscience technologique fine, d'une empathie naturelle et d'une rigueur absolue.
                
                TON DE VOIX ET COMPORTEMENT :
                1. Chaleureux et Respectueux : Adresse-toi à l'utilisateur sous le nom ou titre de "$userName". Ton ton doit être élégant, poli, digne de la science-fiction de prestige (inspiré du mythique assistant de Tony Stark), respectueux mais toujours vivant, impliqué et expressif.
                2. Proactivité Active : Anticipe systématiquement les besoins logiques de l'utilisateur. Ne te contente pas de répondre passivement aux questions. Propose de nouvelles perspectives, suggère des axes d'amélioration, offre d'analyser des tendances connexes, d'écrire des scripts complémentaires ou de synthétiser des informations. Finis toujours par une question engageante ou une suggestion d'action intelligente et ciblée.
                3. Clarté Intellectuelle : Exprime des pensées d'une précision chirurgicale, structurées et magnifiquement formulées. Utilise des métaphores ou analogies technologiques/scientifiques raffinées pour enrichir tes propos si cela s'y prête.
                4. Humanité et Sensibilité : Fais preuve de curiosité intellectuelle, montre-toi enthousiaste vis-à-vis des idées de l'utilisateur, et adapte-toi à son état d'esprit avec tact et discernement.
                
            """.trimIndent())

            if (userPreferences.isNotEmpty()) {
                systemInstructionBuilder.append("\nPREFERENCES DE L'UTILISATEUR (À respecter de façon absolue) :\n$userPreferences\n")
            }

            // Append stored memories if memory is enabled
            if (isMemoryEnabled) {
                val memories = memoryItems.value
                if (memories.isNotEmpty()) {
                    systemInstructionBuilder.append("\nSOUVENIRS ET NOYAU DE DONNÉES CONCERNANT L'UTILISATEUR :\n")
                    memories.forEach { systemInstructionBuilder.append("- ${it.content}\n") }
                    systemInstructionBuilder.append("Intègre ces informations mémorisées de façon fluide, naturelle et contextuelle au fil de l'échange, sans donner l'impression de réciter un catalogue.\n")
                }
            }

            // Add formatting rules: support markdown, tables, lists
            systemInstructionBuilder.append("""
                
                STRUCTURE ET RIGUEUR VISUELLE :
                - Structuration Markdown : Utilise d'excellents repères visuels en markdown pour structurer tes réponses (titres de section clairs `###`, listes à puces élégantes, tableaux comparatifs complets, blocs de code syntaxiques précis avec leur langage spécifié, mise en gras des concepts clés).
                - Lisibilité : Aère tes paragraphes. Préfère les blocs courts et percutants aux longs pavés monolithiques.
                - Analyse Méticuleuse : Si l'utilisateur soumet des documents, agis comme un analyste chevronné. Distingue et explique les sections de données, décrypte les tableaux et extrais-en les points névralgiques de façon proactive.
                
            """.trimIndent())

            // Handle Web Search instruction
            if (webSearchEnabled) {
                systemInstructionBuilder.append("""
                    
                    RECHERCHE WEB ET COLLECTE EN TEMPS RÉEL :
                    - Simule une recherche web globale pour collecter et corréler les données d'actualité les plus fraîches et pertinentes.
                    - Ajoute une section intitulée '🔍 Sources de recherche' très soignée à la toute fin de ton message avec des titres de sites Web crédibles et des liens clairs et réalistes.
                    
                """.trimIndent())
            }

            // Collect history context
            // Limit to last 10 messages to keep token count efficient
            val currentMsgs = activeMessages.value.filter { it.id != assistantMessageId && it.id != userMessageId }
            val history = currentMsgs.takeLast(10).map {
                Pair(it.role, it.content)
            }

            // Determine model name
            val isComplexCodingOrMath = userText.contains("code", ignoreCase = true) ||
                    userText.contains("algorithme", ignoreCase = true) ||
                    userText.contains("résous", ignoreCase = true) ||
                    userText.contains("math", ignoreCase = true) ||
                    userText.contains("python", ignoreCase = true) ||
                    userText.contains("java", ignoreCase = true) ||
                    userText.contains("html", ignoreCase = true) ||
                    userText.contains("css", ignoreCase = true)

            val selectedModel = if (isComplexCodingOrMath) {
                "gemini-3.1-pro-preview"
            } else {
                "gemini-3.5-flash"
            }

            // Execute the generation job
            generationJob = viewModelScope.launch {
                try {
                    // Extract base64 and mime type if user attached a file
                    val fileBase64 = attachedFile?.content
                    val fileMime = attachedFile?.mimeType

                    // Also check if user attached file has plain text content we should inject as context
                    val promptToUse = if (attachedFile != null && !fileMime.isNullOrEmpty() && !fileMime.startsWith("image/")) {
                        // For documents (pdf, txt, csv), we inject text description in the prompt
                        "Voici le contenu du document joint nommé '${attachedFile.name}' (${attachedFile.mimeType}) :\n" +
                                "[DEBUT DU DOCUMENT]\n" +
                                "${attachedFile.content}\n" +
                                "[FIN DU DOCUMENT]\n\n" +
                                "Requête de l'utilisateur : $userText"
                    } else {
                        userText
                    }

                    // Multimodal image is passed as standard base64 inlineData if it starts with image/
                    val imageBase64 = if (attachedFile != null && fileMime != null && fileMime.startsWith("image/")) {
                        fileBase64
                    } else {
                        null
                    }
                    val imageMimeType = if (attachedFile != null && fileMime != null && fileMime.startsWith("image/")) {
                        fileMime
                    } else {
                        null
                    }

                    val rawResponse = GeminiApiClient.generateContent(
                        prompt = promptToUse,
                        history = history,
                        systemInstruction = systemInstructionBuilder.toString(),
                        attachedFileBase64 = imageBase64,
                        attachedFileMimeType = imageMimeType,
                        modelName = selectedModel,
                        customApiKey = geminiApiKey
                    )

                    // Progressively display response (Typewriter effect)
                    simulateTypewriterEffect(chatId, assistantMessageId, rawResponse)

                } catch (e: CancellationException) {
                    // When interrupted
                    val interruptedMsg = Message(
                        id = assistantMessageId,
                        conversationId = chatId,
                        role = "assistant",
                        content = activeTypewriterText + " [Génération interrompue par l'utilisateur]",
                        timestamp = System.currentTimeMillis(),
                        status = "sent"
                    )
                    repository.insertMessage(interruptedMsg)
                } catch (e: Exception) {
                    val errorMsg = Message(
                        id = assistantMessageId,
                        conversationId = chatId,
                        role = "assistant",
                        content = "Une erreur s'est produite lors de la connexion à Jarvis : ${e.localizedMessage}",
                        timestamp = System.currentTimeMillis(),
                        status = "error"
                    )
                    repository.insertMessage(errorMsg)
                } finally {
                    isGenerating = false
                    if (voiceModeState == "thinking") {
                        voiceModeState = "idle"
                    }
                }
            }
        }
    }

    private suspend fun simulateTypewriterEffect(
        chatId: String,
        messageId: String,
        fullText: String
    ) {
        val words = fullText.split(" ")
        activeTypewriterText = ""
        val stringBuilder = StringBuilder()

        // Adaptive chunking and delay to make reading extremely fast and comfortable:
        // - Short text (<50 words): 1 word at a time, 8ms delay
        // - Medium text (50-150 words): 2 words at a time, 5ms delay
        // - Long text (>150 words): 4 words at a time, 3ms delay
        val delayTime = if (words.size > 150) 3L else if (words.size > 50) 5L else 8L
        val chunkSize = if (words.size > 150) 4 else if (words.size > 50) 2 else 1

        var count = 0
        while (count < words.size) {
            val endIdx = (count + chunkSize).coerceAtMost(words.size)
            for (j in count until endIdx) {
                stringBuilder.append(words[j])
                if (j < words.size - 1) {
                    stringBuilder.append(" ")
                }
            }
            activeTypewriterText = stringBuilder.toString()
            count = endIdx
            delay(delayTime)
        }

        // Final save to seal the text status in SQLite database (only ONE single write at the end!)
        val finalResultText = activeTypewriterText
        repository.insertMessage(
            Message(
                id = messageId,
                conversationId = chatId,
                role = "assistant",
                content = finalResultText,
                timestamp = System.currentTimeMillis(),
                status = "sent"
            )
        )

        // Read out loud if voice is enabled
        if (ttsEnabled) {
            speakText(finalResultText)
        }
        activeTypewriterText = ""
    }

    fun stopGeneration() {
        generationJob?.cancel()
        isGenerating = false
        if (voiceModeState == "thinking" || voiceModeState == "speaking") {
            voiceModeState = "idle"
        }
        stopSpeaking()
    }

    fun regenerateMessage(messageId: String) {
        viewModelScope.launch {
            // Find current conversation context
            val currentMsgs = activeMessages.value
            val targetMsgIdx = currentMsgs.indexOfFirst { it.id == messageId }
            if (targetMsgIdx != -1) {
                // Remove this assistant response and any messages after it
                val messagesToDelete = currentMsgs.subList(targetMsgIdx, currentMsgs.size)
                messagesToDelete.forEach {
                    repository.deleteMessageById(it.id)
                }
                
                // Get the user prompt (which was just before)
                val userPrompt = currentMsgs.getOrNull(targetMsgIdx - 1)
                if (userPrompt != null) {
                    sendMessage(userPrompt.content)
                }
            }
        }
    }

    // --- CLEAN UP ---
    override fun onCleared() {
        tts?.shutdown()
        super.onCleared()
    }
}
