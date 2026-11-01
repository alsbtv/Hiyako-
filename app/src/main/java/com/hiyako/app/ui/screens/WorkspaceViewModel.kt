package com.hiyako.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiyako.app.data.GitHubApi
import com.hiyako.app.data.Repo
import com.hiyako.app.data.WorkspaceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkspaceViewModel : ViewModel() {
    private val _repos = MutableStateFlow<List<Repo>>(emptyList())
    val repos = _repos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _loadingMessage = MutableStateFlow("")
    val loadingMessage = _loadingMessage.asStateFlow()

    private val _token = MutableStateFlow("")
    val token = _token.asStateFlow()

    fun loadToken(context: Context) {
        val prefs = context.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE)
        _token.value = prefs.getString("github_token", "") ?: ""
        if (_token.value.isNotBlank()) fetchRepos()
    }

    fun saveToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("github_token", token).apply()
        _token.value = token
        fetchRepos()
    }

    fun fetchRepos() {
        if (_token.value.isBlank()) return
        _isLoading.value = true
        viewModelScope.launch {
            _repos.value = GitHubApi.getUserRepos(_token.value)
            _isLoading.value = false
        }
    }

    fun selectRepoAndLoadFiles(repo: Repo, onComplete: () -> Unit) {
        _isLoading.value = true
        _loadingMessage.value = "Fetching file tree..."
        viewModelScope.launch {
            val owner = repo.fullName.split("/")[0]
            val filesList = GitHubApi.getRepoFiles(_token.value, owner, repo.name, repo.defaultBranch)
            val loadedFiles = mutableMapOf<String, String>()
            
            // تم إزالة الحد الأقصى لقراءة كل الملفات
            filesList.forEachIndexed { index, path -> 
                _loadingMessage.value = "Loading files: ${index + 1}/${filesList.size}..."
                val content = GitHubApi.getFileContent(_token.value, owner, repo.name, path)
                if (content != null) {
                    loadedFiles[path] = content
                }
            }
            
            WorkspaceManager.activeRepo = repo.fullName
            WorkspaceManager.projectFiles = loadedFiles
            WorkspaceManager.isLoaded = loadedFiles.isNotEmpty()
            
            _isLoading.value = false
            _loadingMessage.value = ""
            onComplete()
        }
    }
}
