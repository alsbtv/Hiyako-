package com.ejemplo.hiyako.presentation.navigation

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val DETAILS = "details/{animeUrl}/{title}/{poster}"
    const val PLAYER = "player/{videoUrl}"

    fun detailsRoute(animeUrl: String, title: String, poster: String): String {
        return "details/${android.net.Uri.encode(animeUrl)}/${android.net.Uri.encode(title)}/${android.net.Uri.encode(poster)}"
    }

    fun playerRoute(videoUrl: String): String {
        return "player/${android.net.Uri.encode(videoUrl)}"
    }
}
