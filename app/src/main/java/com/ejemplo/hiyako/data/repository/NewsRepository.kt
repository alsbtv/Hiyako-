package com.ejemplo.hiyako.data.repository

import com.ejemplo.hiyako.data.model.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor() {
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).build()

    suspend fun fetchNews(): List<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("https://aninews.vercel.app/api/news").build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val jsonString = response.body?.string() ?: "[]"
            val jsonArray = JSONArray(jsonString)
            val articles = mutableListOf<NewsArticle>()

            // Limit to first 15 to avoid translation rate limits
            for (i in 0 until minOf(jsonArray.length(), 15)) {
                val item = jsonArray.getJSONObject(i)
                val originalTitle = item.optString("title")
                val originalDesc = item.optString("description")
                
                // Translate
                val translatedTitle = translateText(originalTitle)
                val translatedDesc = translateText(originalDesc)

                articles.add(NewsArticle(
                    id = item.optString("id", i.toString()),
                    title = originalTitle,
                    description = originalDesc,
                    imageUrl = item.optString("image"),
                    source = item.optString("source"),
                    publishedAt = item.optString("publishedAt"),
                    translatedTitle = translatedTitle,
                    translatedDescription = translatedDesc
                ))
            }
            return@withContext articles.distinctBy { it.title.lowercase().trim() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun translateText(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext text
        try {
            val encodedText = java.net.URLEncoder.encode(text, "UTF-8")
            val targetUrl = "https://lingva.ml/api/v1/en/ar/$encodedText"
            val workerUrl = "https://late-morning-434d.wesaamalsubhi.workers.dev/?url=$targetUrl"
            
            val request = Request.Builder().url(workerUrl).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonString = response.body?.string() ?: ""
                val jsonObject = JSONObject(jsonString)
                return@withContext jsonObject.optString("translation", text)
            }
            text
        } catch (e: Exception) {
            text
        }
    }
}
