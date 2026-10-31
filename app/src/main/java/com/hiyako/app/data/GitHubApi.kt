package com.hiyako.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

data class Repo(val name: String, val fullName: String, val defaultBranch: String)

object GitHubApi {
    private val client = OkHttpClient.Builder().build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun authRequest(url: String, token: String) = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer $token")
        .header("Accept", "application/vnd.github.v3+json")
        .build()

    suspend fun getUserRepos(token: String): List<Repo> = withContext(Dispatchers.IO) {
        try {
            client.newCall(authRequest("https://api.github.com/user/repos", token)).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val jsonArray = JSONArray(response.body?.string() ?: "[]")
                val repos = mutableListOf<Repo>()
                for (i in 0 until jsonArray.length()) {
                    val repo = jsonArray.getJSONObject(i)
                    repos.add(Repo(repo.getString("name"), repo.getString("full_name"), repo.getString("default_branch")))
                }
                return@withContext repos
            }
        } catch (e: Exception) { return@withContext emptyList() }
    }

    suspend fun getRepoFiles(token: String, owner: String, repo: String, branch: String): List<String> = withContext(Dispatchers.IO) {
        val files = mutableListOf<String>()
        try {
            val url = "https://api.github.com/repos/$owner/$repo/git/trees/$branch?recursive=1"
            client.newCall(authRequest(url, token)).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val tree = JSONObject(response.body?.string() ?: "{}").optJSONArray("tree") ?: return@withContext emptyList()
                for (i in 0 until tree.length()) {
                    val item = tree.getJSONObject(i)
                    if (item.getString("type") == "blob") {
                        val path = item.getString("path")
                        if (!path.contains("build/") && !path.contains(".git/") && (path.endsWith(".kt") || path.endsWith(".xml") || path.endsWith(".kts") || path.endsWith(".gradle"))) {
                            files.add(path)
                        }
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext files
    }

    suspend fun getFileContent(token: String, owner: String, repo: String, path: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$owner/$repo/contents/$path"
            client.newCall(authRequest(url, token)).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val contentBase64 = JSONObject(response.body?.string() ?: "{}").optString("content").replace("\n", "")
                if (contentBase64.isBlank()) return@withContext null
                String(Base64.getDecoder().decode(contentBase64))
            }
        } catch (e: Exception) { return@withContext null }
    }

    // جلب SHA للملف لتحديثه
    private suspend fun getFileSha(token: String, owner: String, repo: String, path: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$owner/$repo/contents/$path"
            client.newCall(authRequest(url, token)).execute().use { response ->
                if (response.isSuccessful) {
                    return@withContext JSONObject(response.body?.string() ?: "{}").optString("sha")
                }
            }
        } catch (e: Exception) {}
        return@withContext null
    }

    // رفع الملف المعدل
    suspend fun updateFile(token: String, owner: String, repo: String, path: String, newContent: String, commitMessage: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val sha = getFileSha(token, owner, repo, path)
            val payload = JSONObject().apply {
                put("message", commitMessage)
                put("content", Base64.getEncoder().encodeToString(newContent.toByteArray()))
                if (sha != null) put("sha", sha)
            }
            
            val request = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repo/contents/$path")
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github.v3+json")
                .put(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response -> return@withContext response.isSuccessful }
        } catch (e: Exception) { return@withContext false }
    }
}

object WorkspaceManager {
    var activeRepo: String = ""
    var projectFiles: Map<String, String> = emptyMap()
    var isLoaded: Boolean = false
}
