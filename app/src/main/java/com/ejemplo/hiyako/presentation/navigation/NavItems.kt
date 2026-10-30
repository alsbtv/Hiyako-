package com.ejemplo.hiyako.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class NavItem(val title: String, val icon: ImageVector, val route: String)

val bottomNavItems = listOf(
    NavItem("Home", Icons.Default.Home, Routes.HOME),
    NavItem("Library", Icons.Default.List, Routes.LIBRARY),
    NavItem("Profile", Icons.Default.Person, Routes.PROFILE)
)

val drawerNavItems = listOf(
    NavItem("Home", Icons.Default.Home, Routes.HOME),
    NavItem("Search", Icons.Default.Search, Routes.SEARCH),
    NavItem("Pick Random Anime", Icons.Default.Refresh, "random"),
    NavItem("Recent Releases", Icons.Default.PlayArrow, "recent"),
    NavItem("Trending Anime", Icons.Default.Star, "trending"),
    NavItem("Schedule", Icons.Default.DateRange, "schedule"),
    NavItem("Coming Soon", Icons.Default.Info, "coming_soon"),
    NavItem("Library", Icons.Default.List, Routes.LIBRARY),
    NavItem("Watch History", Icons.Default.History, "history"),
    NavItem("Downloads", Icons.Default.CloudDownload, "downloads"),
    NavItem("My List", Icons.Default.CheckCircle, "my_list"),
    NavItem("Settings", Icons.Default.Settings, Routes.SETTINGS)
)
