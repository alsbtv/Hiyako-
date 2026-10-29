package com.ejemplo.hiyako.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ejemplo.hiyako.presentation.details.DetailsScreen
import com.ejemplo.hiyako.presentation.home.HomeScreen
import com.ejemplo.hiyako.presentation.news.NewsScreen
import com.ejemplo.hiyako.presentation.search.SearchScreen
import com.ejemplo.hiyako.presentation.theme.HiyakoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HiyakoTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        if (currentRoute != "details") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(0.9f).clip(androidx.compose.foundation.shape.RoundedCornerShape(30.dp)),
                                    color = Color(0xEE1E1B26), // Semi-transparent dark
                                    tonalElevation = 8.dp
                                ) {
                                    NavigationBar(
                                        containerColor = Color.Transparent,
                                        tonalElevation = 0.dp
                                    ) {
                                        NavigationBarItem(icon = { Icon(Icons.Default.Home, contentDescription = null) }, label = { Text("Home") }, selected = currentRoute == "home", onClick = { navController.navigate("home") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFB388FF), selectedTextColor = Color(0xFFB388FF), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray))
                                        NavigationBarItem(icon = { Icon(Icons.Default.Search, contentDescription = null) }, label = { Text("Search") }, selected = currentRoute == "search", onClick = { navController.navigate("search") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFB388FF), selectedTextColor = Color(0xFFB388FF), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray))
                                        NavigationBarItem(icon = { Icon(Icons.Default.List, contentDescription = null) }, label = { Text("News") }, selected = currentRoute == "news", onClick = { navController.navigate("news") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFB388FF), selectedTextColor = Color(0xFFB388FF), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray))
                                        NavigationBarItem(icon = { Icon(Icons.Default.Star, contentDescription = null) }, label = { Text("AI") }, selected = false, onClick = { }, colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray))
                                    }
                                }
                            }
                        }
                    },
                    containerColor = Color(0xFF0B131D)
                ) { padding ->
                    NavHost(navController = navController, startDestination = "home", modifier = Modifier.padding(padding)) {
                        composable("home") { HomeScreen(navController) }
                        composable("search") { SearchScreen() }
                        composable("news") { NewsScreen() }
                        composable("details/{animeUrl}") { backStackEntry ->
                            val url = backStackEntry.arguments?.getString("animeUrl") ?: ""
                            DetailsScreen(url, navController)
                        }
                    }
                }
            }
        }
    }
}
