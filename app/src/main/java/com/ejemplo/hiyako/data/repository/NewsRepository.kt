package com.ejemplo.hiyako.data.repository

import com.ejemplo.hiyako.data.api.TranslateApi
import com.ejemplo.hiyako.data.model.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor(
    private val translateApi: TranslateApi
) {
    private val client = OkHttpClient()

    suspend fun fetchNews(): List<NewsArticle> = withContext(Dispatchers.IO) {
        val articles = mutableListOf<NewsArticle>()

        // 1. Fetch from AniNewsAPI
        try {
            val request = Request.Builder()
                .url("https://aninews.vercel.app/api/news")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val jsonArray = JSONArray(body)
                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.optJSONObject(i) ?: continue
                            val title = item.optString("title", "")
                            val desc = item.optString("description", item.optString("snippet", ""))
                            val img = item.optString("image", item.optString("imageUrl", ""))
                            val source = item.optString("source", "AniNews")
                            val date = item.optString("date", item.optString("publishedAt", ""))

                            if (title.isNotBlank()) {
                                articles.add(
                                    NewsArticle(
                                        id = UUID.randomUUID().toString(),
                                        title = title,
                                        description = desc,
                                        imageUrl = img,
                                        source = source,
                                        publishedAt = date
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        // 2. Fetch from ANN RSS
        try {
            val request = Request.Builder()
                .url("https://www.animenewsnetwork.com/news/rss.xml")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val xmlString = response.body?.string()
                    if (xmlString != null) {
                        val factory = XmlPullParserFactory.newInstance()
                        val parser = factory.newPullParser()
                        parser.setInput(StringReader(xmlString))

                        var eventType = parser.eventType
                        var title = ""
                        var link = ""
                        var desc = ""
                        var date = ""
                        var inItem = false

                        while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                            val name = parser.name
                            when (eventType) {
                                org.xmlpull.v1.XmlPullParser.START_TAG -> {
                                    if (name.equals("item", ignoreCase = true)) {
                                        inItem = true
                                    }
                                }
                                org.xmlpull.v1.XmlPullParser.TEXT -> {
                                    if (inItem) {
                                        when {
                                            name.equals("title", ignoreCase = true) -> title = parser.text
                                            name.equals("link", ignoreCase = true) -> link = parser.text
                                            name.equals("description", ignoreCase = true) -> desc = parser.text
                                            name.equals("pubDate", ignoreCase = true) -> date = parser.text
                                        }
                                    }
                                }
                                org.xmlpull.v1.XmlPullParser.END_TAG -> {
                                    if (name.equals("item", ignoreCase = true)) {
                                        if (title.isNotBlank()) {
                                            articles.add(
                                                NewsArticle(
                                                    id = UUID.randomUUID().toString(),
                                                    title = title,
                                                    description = desc.replace(Regex("<.*?>"), ""),
                                                    imageUrl = "",
                                                    source = "Anime News Network",
                                                    publishedAt = date
                                                )
                                            )
                                        }
                                        title = ""
                                        link = ""
                                        desc = ""
                                        date = ""
                                        inItem = false
                                    }
                                }
                            }
                            eventType = parser.next()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        val deduplicated = articles.distinctBy { it.title.lowercase().trim() }

        // Translate the first 10 articles
        val translatedList = mutableListOf<NewsArticle>()
        for ((index, article) in deduplicated.withIndex()) {
            if (index < 10) {
                val tTitle = translateApi.translateToArabic(article.title)
                val tDesc = translateApi.translateToArabic(article.description)
                translatedList.add(article.copy(translatedTitle = tTitle, translatedDescription = tDesc))
            } else {
                translatedList.add(article)
            }
        }

        return@withContext translatedList
    }
}
