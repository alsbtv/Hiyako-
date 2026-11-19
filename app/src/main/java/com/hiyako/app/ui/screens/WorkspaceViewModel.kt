package com.hiyako.app.ui.screens

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
    
    private val _loadingMessage = MutableStateFlow("")
    val loadingMessage = _loadingMessage.asStateFlow()

    private val _token = MutableStateFlow("")
    val token = _token.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun loadToken(context: Context) {
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
        _loadingMessage.value = "Fetching file tree..."
        viewModelScope.launch {
            val owner = repo.fullName.split("/")[0]
            val filesList = GitHubApi.getRepoFiles(_token.value, owner, repo.name, repo.defaultBranch)
            if (filesList.isEmpty()) {
                _isLoading.value = false
                _error.value = "No supported files found in repo."
                return@launch
            }
            
            val loadedFiles = mutableMapOf<String, String>()
            withContext(Dispatchers.IO) {
                filesList.forEachIndexed { index, path -> 
                    _loadingMessage.value = "Loading files: ${index + 1}/${filesList.size}..."
                    val content = GitHubApi.getFileContent(_token.value, owner, repo.name, path)
                    if (content != null) loadedFiles[path] = content
                }
            }
            
            WorkspaceManager.activeRepo = repo.fullName
            WorkspaceManager.projectFiles = loadedFiles
            WorkspaceManager.isLoaded = loadedFiles.isNotEmpty()
            
            _isLoading.value = false
            _loadingMessage.value = ""
            if (loadedFiles.isNotEmpty()) onComplete()
            else _error.value = "Failed to download file contents."
        }
    }
}
