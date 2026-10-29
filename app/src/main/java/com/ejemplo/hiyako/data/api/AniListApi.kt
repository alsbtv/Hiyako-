package com.ejemplo.hiyako.data.api

import com.ejemplo.hiyako.data.model.Character
import com.ejemplo.hiyako.data.model.Episode
import com.ejemplo.hiyako.data.model.ScrapedAnime
import com.ejemplo.hiyako.utils.RateLimiter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AniListApi @Inject constructor() {
    private val client = OkHttpClient()

    suspend fun getAnimeDetails(title: String): ScrapedAnime? = withContext(Dispatchers.IO) {
        RateLimiter.throttleAnilist()
        try {
            val query = """
                query (${'$'}search: String) {
                  Media (search: ${'$'}search, type: ANIME) {
                    title {
                      romaji
                      english
                    }
                    bannerImage
                    coverImage {
                      extraLarge
                    }
                    description
                    averageScore
                    studios(isMain: true) {
                      nodes {
                        name
                      }
                    }
                    genres
                    characters(page: 1, perEnd: 10, sort: [ROLE, FAVOURITES_DESC]) {
                      edges {
                        role
                        node {
                          name {
                            full
                          }
                          image {
                            large
                          }
                        }
                      }
                    }
                    streamingEpisodes {
                      title
                      thumbnail
                      url
                    }
                  }
                }
            """.trimIndent()

            val variables = JSONObject().put("search", title)
            val jsonBody = JSONObject()
                .put("query", query)
                .put("variables", variables)

            val body = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val workerUrl = "https://late-morning-434d.wesaamalsubhi.workers.dev/?url=https://graphql.anilist.co"
            val request = Request.Builder()
                .url(workerUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val responseBody = response.body?.string() ?: return@withContext null
                val json = JSONObject(responseBody)
                val media = json.optJSONObject("data")?.optJSONObject("Media") ?: return@withContext null

                val bannerImage = media.optString("bannerImage", "")
                val coverImage = media.optJSONObject("coverImage")?.optString("extraLarge", "") ?: ""
                val description = media.optString("description", "").replace(Regex("<.*?>"), "")
                val score = media.optDouble("averageScore", 0.0).let { if (it > 0) it / 10.0 else null }

                val studiosList = mutableListOf<String>()
                val studiosObj = media.optJSONObject("studios")?.optJSONArray("nodes")
                if (studiosObj != null) {
                    for (i in 0 until studiosObj.length()) {
                        val studio = studiosObj.optJSONObject(i)
                        studio?.optString("name")?.let { if (it.isNotBlank()) studiosList.add(it) }
                    }
                }

                val genresList = mutableListOf<String>()
                val genresObj = media.optJSONArray("genres")
                if (genresObj != null) {
                    for (i in 0 until genresObj.length()) {
                        genresObj.optString(i)?.let { if (it.isNotBlank()) genresList.add(it) }
                    }
                }

                val charactersList = mutableListOf<Character>()
                val charEdges = media.optJSONObject("characters")?.optJSONArray("edges")
                if (charEdges != null) {
                    for (i in 0 until charEdges.length()) {
                        val edge = charEdges.optJSONObject(i) ?: continue
                        val role = edge.optString("role", "")
                        val node = edge.optJSONObject("node") ?: continue
                        val charName = node.optJSONObject("name")?.optString("full", "") ?: ""
                        val charImage = node.optJSONObject("image")?.optString("large", "") ?: ""
                        if (charName.isNotBlank()) {
                            charactersList.add(Character(name = charName, imageUrl = charImage, role = role))
                        }
                    }
                }

                val episodesList = mutableListOf<Episode>()
                val streamEp = media.optJSONArray("streamingEpisodes")
                if (streamEp != null) {
                    for (i in 0 until streamEp.length()) {
                        val ep = streamEp.optJSONObject(i) ?: continue
                        val epTitle = ep.optString("title", "Episode ${i+1}")
                        val epThumb = ep.optString("thumbnail", "")
                        val epUrl = ep.optString("url", "")
                        episodesList.add(Episode(number = "${i+1}", title = epTitle, thumbnailUrl = epThumb, url = epUrl))
                    }
                }

                ScrapedAnime(
                    title = title,
                    url = "",
                    posterUrl = coverImage.ifEmpty { bannerImage },
                    bannerUrl = bannerImage.ifEmpty { coverImage },
                    synopsis = description,
                    score = score,
                    studios = studiosList,
                    genres = genresList,
                    episodesList = episodesList,
                    characters = charactersList
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}
