package com.hiyako.app.ui.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hiyako.app.NexusService
import com.hiyako.app.data.db.AppDatabase
import com.hiyako.app.data.db.ChatEntity
import com.hiyako.app.data.db.ConversationEntity
import com.hiyako.app.data.db.MemoryEntity
import com.hiyako.app.data.db.ReportEntity
import com.hiyako.app.engine.PuterEngine
import com.hiyako.app.engine.PuterListener
import com.hiyako.app.util.isRtlText
import com.hiyako.app.util.queryDisplayName
import com.hiyako.app.providers.AccountStore
import com.hiyako.app.providers.OpenAiCompatClient
import com.hiyako.app.providers.ProviderCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class Message(
    val id: Int,
    val content: String,
    val isUser: Boolean,
    val type: String = "TEXT",
    val attachmentUri: String? = null,
    val senderName: String? = null
)

data class AiModel(val name: String, val id: String, val accountId: String = "")

data class AuthInfo(val signedIn: Boolean, val username: String)

@OptIn(ExperimentalCoroutinesApi::class)
class AiViewModel(application: Application) :
    AndroidViewModel(application), TextToSpeech.OnInitListener, PuterListener {

    private val appCtx: Context = application
    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatDao()
    private val conversationDao = db.conversationDao()
    private val memoryDao = db.memoryDao()
    private val prefs = application.getSharedPreferences("nexus_settings", Context.MODE_PRIVATE)
    private var tts: TextToSpeech? = null

    private class Pending(val convoId: Int) {
        private val sb = StringBuilder()

        @Volatile
        var lastActivity: Long = System.currentTimeMillis()

        @Synchronized
        fun append(t: String): String {
            sb.append(t)
            lastActivity = System.currentTimeMillis()
            return sb.toString()
        }

        @Synchronized
        fun text(): String = sb.toString()
    }

    private class SavedAttachment(val file: File, val mime: String, val name: String)
    private class BuiltRequest(val messagesJson: String, val imageDataUrl: String?)

    private val pending = ConcurrentHashMap<String, Pending>()
    private val pendingImages = ConcurrentHashMap<String, String>()
    // Request payloads kept only for the Puter-to-account fallback on credit errors.
    private val pendingPayloads = ConcurrentHashMap<String, Pair<String, String?>>()
    private val titleFallbacks = ConcurrentHashMap<Int, String>()
    private var requestCounter = 0L

    private val _conversations = MutableStateFlow<List<ConversationEntity>>(emptyList())
    val conversations: StateFlow<List<ConversationEntity>> = _conversations.asStateFlow()

    private val _activeConversationId = MutableStateFlow<Int?>(null)
    val activeConversationId: StateFlow<Int?> = _activeConversationId.asStateFlow()

    val messages: StateFlow<List<Message>> = _activeConversationId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList<ChatEntity>()) else chatDao.getMessagesForChat(id)
        }
        .map { list ->
            list.filterNot { it.role == "ai" && it.content == "Thinking..." }
                .map { e ->
                    Message(
                        id = e.id,
                        content = e.content,
                        isUser = e.role == "user",
                        type = e.messageType,
                        attachmentUri = e.attachmentUri,
                        senderName = e.senderName
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val chatTitle: StateFlow<String> = combine(_conversations, _activeConversationId) { convos, id ->
        convos.firstOrNull { it.id == id }?.title ?: DEFAULT_TITLE
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_TITLE)

    // conversationId -> text streamed so far ("" means waiting for the first token)
    private val _generating = MutableStateFlow<Map<Int, String>>(emptyMap())
    val generating: StateFlow<Map<Int, String>> = _generating.asStateFlow()

    private val _errors = MutableStateFlow<Map<Int, String>>(emptyMap())
    val errors: StateFlow<Map<Int, String>> = _errors.asStateFlow()

    private val _selectedModel = MutableStateFlow(
        AiModel(
            prefs.getString("model_name", "Default (Auto)") ?: "Default (Auto)",
            prefs.getString("model_id", "") ?: "",
            prefs.getString("model_account", "") ?: ""
        )
    )
    val selectedModel: StateFlow<AiModel> = _selectedModel.asStateFlow()

    private val _availableModels = MutableStateFlow<List<AiModel>>(accountModelsOf(appCtx))
    val availableModels: StateFlow<List<AiModel>> = _availableModels.asStateFlow()

    private val _memories = MutableStateFlow<List<MemoryEntity>>(emptyList())
    val memories: StateFlow<List<MemoryEntity>> = _memories.asStateFlow()

    private val _auth = MutableStateFlow(AuthInfo(false, ""))
    val auth: StateFlow<AuthInfo> = _auth.asStateFlow()

    var isMultiSelectMode by mutableStateOf(false)
    val selectedChatIds = mutableStateListOf<Int>()
    var isThinkingMode by mutableStateOf(false)
    var isSearchMode by mutableStateOf(false)
    var themeMode by mutableStateOf(prefs.getString("theme", "Dark") ?: "Dark")
    var fontSize by mutableStateOf(prefs.getFloat("fontSize", 1f))

    private val engineHolder = lazy { PuterEngine(appCtx, this) }
    val engine: PuterEngine get() = engineHolder.value

    init {
        tts = TextToSpeech(application, this)

        viewModelScope.launch {
            conversationDao.getAllConversations().collect { convos ->
                _conversations.value = convos
                val active = _activeConversationId.value
                if (convos.isEmpty()) {
                    val newId = conversationDao.insertConversation(ConversationEntity(title = DEFAULT_TITLE)).toInt()
                    _activeConversationId.value = newId
                } else if (active == null || convos.none { it.id == active }) {
                    _activeConversationId.value = convos.first().id
                }
            }
        }

        viewModelScope.launch {
            memoryDao.getAllMemories().collectLatest { _memories.value = it }
        }

        // Watchdog: fail requests that stopped producing output.
        viewModelScope.launch {
            while (true) {
                delay(5000)
                val now = System.currentTimeMillis()
                pending.entries.toList().forEach { entry ->
                    if (now - entry.value.lastActivity > REQUEST_TIMEOUT_MS) {
                        failRequest(entry.key, "The model stopped responding (timeout).")
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ TTS

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault()
    }

    fun speak(text: String) {
        val clean = text
            .replace(Regex("```[\\s\\S]*?```"), " ")
            .replace("*", "")
            .replace("#", "")
            .replace("`", "")
            .trim()
            .take(3500)
        if (clean.isEmpty()) return
        tts?.language = if (isRtlText(clean)) Locale("ar") else Locale.getDefault()
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "nexus_tts")
    }

    // ------------------------------------------------------------------ Simple state

    fun toggleThink() { isThinkingMode = !isThinkingMode }
    fun toggleSearch() { isSearchMode = !isSearchMode }

    fun selectModel(model: AiModel) {
        _selectedModel.value = model
        prefs.edit()
            .putString("model_id", model.id)
            .putString("model_name", model.name)
            .putString("model_account", model.accountId)
            .apply()
    }

    fun updateTheme(mode: String) {
        themeMode = mode
        prefs.edit().putString("theme", mode).apply()
    }

    fun updateFontSize(size: Float) {
        fontSize = size
        prefs.edit().putFloat("fontSize", size).apply()
    }

    fun addMemory(content: String) {
        viewModelScope.launch { memoryDao.insertMemory(MemoryEntity(content = content)) }
    }

    fun clearMemories() {
        viewModelScope.launch { memoryDao.clearMemories() }
    }

    fun canSend(): Boolean = engine.ready.value

    fun refreshModels() = engine.refreshModels()

    fun signIn() = engine.signIn()

    // ------------------------------------------------------------------ Conversations

    fun createNewConversation() {
        val active = _activeConversationId.value
        if (active != null && messages.value.isEmpty() && !_generating.value.containsKey(active)) return
        viewModelScope.launch {
            _activeConversationId.value =
                conversationDao.insertConversation(ConversationEntity(title = DEFAULT_TITLE)).toInt()
        }
    }

    fun selectConversation(id: Int) {
        _activeConversationId.value = id
    }

    fun deleteChat(id: Int) {
        cancelRequestsFor(id)
        viewModelScope.launch {
            chatDao.clearChat(id)
            conversationDao.deleteConversation(id)
        }
    }

    fun renameChat(id: Int, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { conversationDao.updateTitle(id, clean) }
    }

    fun togglePin(id: Int, isPinned: Boolean) {
        viewModelScope.launch { conversationDao.updatePin(id, isPinned) }
    }

    /** Saves a report for an AI reply. Reports stay on this device until a moderation backend exists. */
    fun reportMessage(messageId: Int, content: String) {
        val conversationId = _activeConversationId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            db.reportDao().insert(
                ReportEntity(messageId = messageId, conversationId = conversationId, content = content)
            )
        }
    }

    fun clearAllChats() {
        _conversations.value.forEach { cancelRequestsFor(it.id) }
        viewModelScope.launch {
            chatDao.clearAllMessages()
            conversationDao.deleteAllConversations()
            withContext(Dispatchers.IO) { File(appCtx.filesDir, "attachments").deleteRecursively() }
        }
    }

    private fun cancelRequestsFor(convoId: Int) {
        pending.entries.toList().forEach { entry ->
            if (entry.value.convoId == convoId) {
                pending.remove(entry.key)
                pendingImages.remove(entry.key)
            }
        }
        _generating.update { it - convoId }
        _errors.update { it - convoId }
        stopServiceIfIdle()
    }

    // ------------------------------------------------------------------ Sending

    private fun markGenerating(convoId: Int): Boolean {
        if (_generating.value.containsKey(convoId)) return false
        _generating.update { it + (convoId to "") }
        _errors.update { it - convoId }
        return true
    }

    fun sendMessage(text: String, attachmentUri: Uri?) {
        val convoId = _activeConversationId.value ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty() && attachmentUri == null) return
        if (!markGenerating(convoId)) return

        val currentTitle = _conversations.value.firstOrNull { it.id == convoId }?.title
        val needsTitle = currentTitle == null || currentTitle == DEFAULT_TITLE || currentTitle == "New Group"

        viewModelScope.launch {
            try {
                var type = "TEXT"
                var storedUri: String? = null
                var displayName: String? = null
                if (attachmentUri != null) {
                    val saved = withContext(Dispatchers.IO) { copyAttachment(attachmentUri) }
                    type = if (saved.mime.startsWith("image/")) "IMAGE" else "FILE"
                    storedUri = Uri.fromFile(saved.file).toString()
                    displayName = saved.name
                }
                chatDao.insertMessage(
                    ChatEntity(
                        conversationId = convoId,
                        role = "user",
                        content = trimmed,
                        messageType = type,
                        attachmentUri = storedUri,
                        senderName = displayName
                    )
                )
                conversationDao.touch(convoId, System.currentTimeMillis())

                if (needsTitle) {
                    val seed = if (trimmed.isNotEmpty()) trimmed else (displayName ?: "Attachment")
                    val fallback = seed.lines().first().trim().take(32).ifBlank { DEFAULT_TITLE }
                    titleFallbacks[convoId] = fallback
                    conversationDao.updateTitle(convoId, fallback)
                    engine.makeTitle(convoId, seed)
                }
            } catch (e: Exception) {
                _generating.update { it - convoId }
                _errors.update { it + (convoId to ("Could not attach the file: " + (e.message ?: "unknown error"))) }
                return@launch
            }
            runRequest(convoId)
        }
    }

    fun regenerate() {
        val convoId = _activeConversationId.value ?: return
        if (!markGenerating(convoId)) return
        viewModelScope.launch {
            try {
                val last = chatDao.getMessagesOnce(convoId).lastOrNull()
                if (last != null && last.role != "user") chatDao.deleteMessage(last.id)
            } catch (e: Exception) {
                _generating.update { it - convoId }
                return@launch
            }
            runRequest(convoId)
        }
    }

    fun stopGeneration() {
        val convoId = _activeConversationId.value ?: return
        val partials = mutableListOf<String>()
        pending.entries.toList().forEach { entry ->
            if (entry.value.convoId == convoId) {
                pending.remove(entry.key)
                pendingImages.remove(entry.key)
                partials.add(entry.value.text())
            }
        }
        val partial = partials.joinToString("").trim()
        viewModelScope.launch {
            if (partial.isNotEmpty()) {
                chatDao.insertMessage(ChatEntity(conversationId = convoId, role = "ai", content = partial))
            }
            _generating.update { it - convoId }
            stopServiceIfIdle()
        }
    }

    private suspend fun runRequest(convoId: Int) {
        requestCounter += 1
        val reqId = "r$requestCounter"
        pending[reqId] = Pending(convoId)
        startServiceSafely()
        try {
            val entities = chatDao.getMessagesOnce(convoId)
            val built = withContext(Dispatchers.IO) { buildRequest(entities) }
            if (!pending.containsKey(reqId)) return
            val image = built.imageDataUrl
            if (image != null) pendingImages[reqId] = image
            val selected = _selectedModel.value
            if (selected.accountId.isNotEmpty()) {
                streamViaAccount(reqId, selected.accountId, built.messagesJson, image)
            } else {
                pendingPayloads[reqId] = built.messagesJson to image
                engine.ask(reqId, built.messagesJson, selected.id, image != null)
            }
        } catch (e: Exception) {
            failRequest(reqId, e.message ?: "Unexpected error")
        }
    }

    /** Refreshes the model picker after accounts are added or removed in Settings. */
    fun reloadAccountModels() {
        _availableModels.update { list -> list.filter { it.accountId.isEmpty() } + accountModelsOf(appCtx) }
    }

    /** Sends the request to a saved account. On quota or credit errors it moves on to the next saved account. */
    private fun streamViaAccount(reqId: String, accountId: String, messagesJson: String, imageDataUrl: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val accounts = AccountStore.load(appCtx)
            val start = accounts.indexOfFirst { it.id == accountId }
            if (start < 0) {
                failRequest(reqId, "This account was removed. Pick another model.")
                return@launch
            }
            val messages = try {
                buildAccountMessages(messagesJson, imageDataUrl)
            } catch (e: Exception) {
                failRequest(reqId, "Could not prepare the message.")
                return@launch
            }
            val order = accounts.subList(start, accounts.size) + accounts.subList(0, start)
            var lastError: Throwable? = null
            for (account in order) {
                if (!pending.containsKey(reqId)) return@launch
                val spec = ProviderCatalog.find(account.providerId) ?: continue
                if (spec.id == ProviderCatalog.PUTER) continue
                val result = OpenAiCompatClient.stream(
                    baseUrl = account.baseUrl,
                    apiKey = AccountStore.apiKey(account),
                    model = account.model,
                    messages = messages
                ) { delta ->
                    if (!pending.containsKey(reqId)) throw CancellationException("Stopped by user")
                    onChunk(reqId, delta)
                }
                if (result.isSuccess) {
                    onDone(reqId)
                    return@launch
                }
                lastError = result.exceptionOrNull()
                if (lastError == null || !OpenAiCompatClient.isQuotaError(lastError)) break
            }
            failRequest(reqId, lastError?.message ?: "No saved account could answer this request.")
        }
    }

    /** Converts the request into OpenAI format; the image becomes an image_url part on the last user message. */
    private fun buildAccountMessages(messagesJson: String, imageDataUrl: String?): JSONArray {
        val array = JSONArray(messagesJson)
        if (imageDataUrl != null) {
            for (i in array.length() - 1 downTo 0) {
                val message = array.getJSONObject(i)
                if (message.optString("role") == "user") {
                    val parts = JSONArray()
                        .put(JSONObject().put("type", "text").put("text", message.optString("content")))
                        .put(
                            JSONObject()
                                .put("type", "image_url")
                                .put("image_url", JSONObject().put("url", imageDataUrl))
                        )
                    message.put("content", parts)
                    break
                }
            }
        }
        return array
    }

    private fun buildRequest(entities: List<ChatEntity>): BuiltRequest {
        val system = StringBuilder()
        system.append("You are Nexus AI, a helpful assistant. Reply in the same language as the user's last message. ")
        system.append("Use Markdown for formatting and fenced code blocks for code.")

        val mem = _memories.value.take(20)
        if (mem.isNotEmpty()) {
            system.append("\n\nKnown facts about the user:\n")
            mem.forEach { system.append("- ").append(it.content.take(300)).append('\n') }
        }
        if (isThinkingMode) {
            system.append("\n\nThink through the problem carefully and step by step before giving the final answer.")
        }
        if (isSearchMode) {
            system.append("\n\nThe user turned on Search mode, but you cannot browse the web in this app. ")
            system.append("If the answer depends on recent information, say so clearly instead of guessing.")
        }

        val arr = JSONArray()
        arr.put(JSONObject().put("role", "system").put("content", system.toString()))

        val recent = entities
            .filterNot { it.role == "ai" && it.content == "Thinking..." }
            .takeLast(MAX_HISTORY)

        for (e in recent) {
            val role = if (e.role == "user") "user" else "assistant"
            var content = e.content.take(12000)
            if (role == "user" && e.messageType == "FILE") {
                val name = e.senderName ?: "file"
                val fileText = readTextAttachment(e.attachmentUri)
                content += if (fileText != null) {
                    "\n\n[Attached file: $name]\n```\n$fileText\n```"
                } else {
                    "\n\n[Attached file: $name - this file type cannot be read in this version of the app]"
                }
            }
            arr.put(JSONObject().put("role", role).put("content", content))
        }

        var imageDataUrl: String? = null
        val last = recent.lastOrNull()
        if (last != null && last.role == "user" && last.messageType == "IMAGE") {
            val path = last.attachmentUri?.let { Uri.parse(it).path }
            if (path != null) imageDataUrl = imageToDataUrl(File(path))
            if (imageDataUrl != null && last.content.isBlank()) {
                arr.remove(arr.length() - 1)
                arr.put(JSONObject().put("role", "user").put("content", "Describe this image in detail."))
            }
        }
        return BuiltRequest(arr.toString(), imageDataUrl)
    }

    private fun copyAttachment(uri: Uri): SavedAttachment {
        val name = queryDisplayName(appCtx, uri)
        val mime = appCtx.contentResolver.getType(uri) ?: "application/octet-stream"
        val dir = File(appCtx.filesDir, "attachments")
        dir.mkdirs()
        val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val out = File(dir, System.currentTimeMillis().toString() + "_" + safe)
        val input = appCtx.contentResolver.openInputStream(uri) ?: throw IllegalStateException("Cannot open the selected file")
        input.use { src -> out.outputStream().use { dst -> src.copyTo(dst) } }
        return SavedAttachment(out, mime, name)
    }

    private fun readTextAttachment(uriString: String?): String? {
        if (uriString == null) return null
        val path = Uri.parse(uriString).path ?: return null
        val file = File(path)
        if (!file.exists() || file.length() > 5_000_000L) return null
        val bytes = file.readBytes()
        for (b in bytes) {
            if (b.toInt() == 0) return null
        }
        return String(bytes, Charsets.UTF_8).take(40000)
    }

    private fun imageToDataUrl(file: File): String? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options()
        bounds.inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
        val opts = BitmapFactory.Options()
        opts.inSampleSize = sample
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        bitmap.recycle()
        return "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    // ------------------------------------------------------------------ Foreground service

    private fun startServiceSafely() {
        try {
            ContextCompat.startForegroundService(appCtx, Intent(appCtx, NexusService::class.java))
        } catch (e: Exception) {
            // The service only keeps the process alive in the background; chat works without it.
        }
    }

    private fun stopServiceIfIdle() {
        viewModelScope.launch {
            delay(1500)
            if (pending.isEmpty()) {
                try {
                    appCtx.stopService(Intent(appCtx, NexusService::class.java))
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    private suspend fun conversationExists(id: Int): Boolean = _conversations.value.any { it.id == id }

    private fun failRequest(reqId: String, message: String) {
        val p = pending.remove(reqId) ?: return
        pendingImages.remove(reqId)
        val partial = p.text().trim()
        viewModelScope.launch {
            if (partial.isNotEmpty() && conversationExists(p.convoId)) {
                chatDao.insertMessage(ChatEntity(conversationId = p.convoId, role = "ai", content = partial))
            }
            _errors.update { it + (p.convoId to message) }
            _generating.update { it - p.convoId }
            stopServiceIfIdle()
        }
    }

    // ------------------------------------------------------------------ PuterListener (called from the WebView bridge thread)

    override fun onEngineReady() {}

    override fun onModelsReceived(json: String) {
        try {
            val arr = JSONArray(json)
            val list = mutableListOf<AiModel>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optString("id")
                if (id.isNotBlank()) {
                    val name = o.optString("name", id).ifBlank { id }
                    list.add(AiModel(name, id))
                }
            }
            val sorted = list.distinctBy { it.id }.sortedBy { it.name.lowercase() }
            _availableModels.value = listOf(AiModel("Default (Auto)", "")) + sorted + accountModelsOf(appCtx)
        } catch (e: Exception) {
            // ignore malformed payload
        }
    }

    override fun onChunk(reqId: String, text: String) {
        val p = pending[reqId] ?: return
        val snapshot = p.append(text)
        _generating.update { it + (p.convoId to snapshot) }
    }

    override fun onDone(reqId: String) {
        val p = pending.remove(reqId) ?: return
        pendingImages.remove(reqId)
        pendingPayloads.remove(reqId)
        val finalText = p.text().trim()
        viewModelScope.launch {
            if (conversationExists(p.convoId)) {
                if (finalText.isNotEmpty()) {
                    chatDao.insertMessage(ChatEntity(conversationId = p.convoId, role = "ai", content = finalText))
                    conversationDao.touch(p.convoId, System.currentTimeMillis())
                } else {
                    _errors.update { it + (p.convoId to "The model returned an empty reply.") }
                }
            }
            _generating.update { it - p.convoId }
            stopServiceIfIdle()
        }
    }

    override fun onError(reqId: String, message: String) {
        val payload = pendingPayloads.remove(reqId)
        val fallback = AccountStore.load(appCtx).firstOrNull { it.model.isNotBlank() }
        val isCredit = message.contains("credit", ignoreCase = true)
        if (payload != null && fallback != null && isCredit && pending.containsKey(reqId)) {
            streamViaAccount(reqId, fallback.id, payload.first, payload.second)
        } else {
            failRequest(reqId, message)
        }
    }

    override fun onTitle(convoId: Int, title: String) {
        val clean = title.trim().trim('"', '\'', '«', '»').trim().take(60)
        if (clean.isEmpty()) return
        val fallback = titleFallbacks[convoId] ?: return
        val current = _conversations.value.firstOrNull { it.id == convoId }?.title
        if (current == fallback) {
            titleFallbacks.remove(convoId)
            viewModelScope.launch { conversationDao.updateTitle(convoId, clean) }
        }
    }

    override fun onAuth(signedIn: Boolean, username: String) {
        _auth.value = AuthInfo(signedIn, username)
    }

    override fun provideImage(reqId: String): String = pendingImages[reqId] ?: ""

    override fun onCleared() {
        tts?.shutdown()
        if (engineHolder.isInitialized()) engine.destroy()
        super.onCleared()
    }

    companion object {
        private const val DEFAULT_TITLE = "New Chat"
        private const val REQUEST_TIMEOUT_MS = 120_000L
        private const val MAX_HISTORY = 30
    }
}

/** Model picker entries for every saved account that has a model name. */
private fun accountModelsOf(context: Context): List<AiModel> =
    AccountStore.load(context)
        .filter { it.model.isNotBlank() }
        .map { AiModel(name = "${it.label} · ${it.model}", id = it.model, accountId = it.id) }
