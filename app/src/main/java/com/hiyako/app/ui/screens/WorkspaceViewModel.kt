package com.hiyako.app.ui.screens

import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiyako.app.data.GitHubApi
import com.hiyako.app.data.Repo
import com.hiyako.app.data.WorkspaceManager
import com.hiyako.app.security.KeyVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkspaceViewModel : ViewModel() {
    private val _repos = MutableStateFlow<List<Repo>>(emptyList())
    val repos = _repos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    private val _loadingMessage = MutableStateFlow("")
    val loadingMessage = _loadingMessage.asStateFlow()

    private val _token = MutableStateFlow("")
    val token = _token.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun loadToken(context: Context) {
        val vault = KeyVault.prefs(context)
        val legacy = context.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE)
        val oldToken = legacy.getString("github_token", "").orEmpty()
        if (oldToken.isNotBlank() && vault.getString("github_token_enc", null) == null) {
            vault.edit().putString("github_token_enc", KeyVault.encrypt(oldToken)).apply()
        }
        if (legacy.contains("github_token") && vault.getString("github_token_enc", null) != null) {
            legacy.edit().remove("github_token").apply()
        }
        val enc = KeyVault.prefs(context).getString("github_token_enc", null)
        _token.value = if (enc != null) KeyVault.decrypt(enc).orEmpty() else ""
        if (_token.value.isNotBlank()) fetchRepos()
    }

    fun saveToken(context: Context, token: String) {
        val clean = token.trim()
        if (clean.isNotEmpty()) {
            val enc = KeyVault.encrypt(clean)
            KeyVault.prefs(context).edit().putString("github_token_enc", enc).apply()
        } else {
            KeyVault.prefs(context).edit().remove("github_token_enc").apply()
        }
        _token.value = clean
        fetchRepos()
    }

    fun disconnect(context: Context) {
        KeyVault.prefs(context).edit().remove("github_token_enc").apply()
        context.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE).edit().remove("github_token").apply()
        WorkspaceManager.activeRepo = ""
        WorkspaceManager.projectFiles = emptyMap()
        WorkspaceManager.isLoaded = false
        _token.value = ""
        _repos.value = emptyList()
        _error.value = null
    }

    fun fetchRepos() {
        if (_token.value.isBlank()) return
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            val res = GitHubApi.getUserRepos(_token.value)
            _repos.value = res
            if (res.isEmpty()) _error.value = "Could not load repositories. Check your token."
            _isLoading.value = false
        }
    }

    fun selectRepoAndLoadFiles(repo: Repo, onComplete: () -> Unit) {
        _isLoading.value = true
        _error.value = null
        _progress.value = 0f
        _loadingMessage.value = "Fetching file tree..."
        viewModelScope.launch {
            val owner = repo.fullName.split("/")[0]
            val filesList = GitHubApi.getRepoFiles(_token.value, owner, repo.name, repo.defaultBranch)
            if (filesList.isEmpty()) {
                _isLoading.value = false
                _error.value = "No supported files found in repo."
                return@launch
            }
            
            val loadedFiles = java.util.concurrent.ConcurrentHashMap<String, String>()
            val finished = java.util.concurrent.atomic.AtomicInteger(0)
            val gate = Semaphore(8)
            val tokenValue = _token.value
            coroutineScope {
                filesList.map { path ->
                    async(Dispatchers.IO) {
                        gate.withPermit {
                            val content = GitHubApi.getFileContent(tokenValue, owner, repo.name, path)
                            if (content != null) loadedFiles[path] = content
                            val count = finished.incrementAndGet()
                            _progress.value = count.toFloat() / filesList.size
                            _loadingMessage.value = "$count / ${filesList.size}"
                        }
                    }
                }.awaitAll()
            }
            
            WorkspaceManager.activeRepo = repo.fullName
            WorkspaceManager.projectFiles = loadedFiles.toMap()
            WorkspaceManager.isLoaded = loadedFiles.isNotEmpty()
            
            _isLoading.value = false
            _loadingMessage.value = ""
            if (loadedFiles.isNotEmpty()) onComplete()
            else _error.value = "Failed to download file contents."
        }
    }
}
