package com.ejemplo.hiyako.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ejemplo.hiyako.presentation.details.DetailsScreen
import com.ejemplo.hiyako.presentation.home.HomeScreen
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
                            NavigationBar(containerColor = Color(0xFF15202B)) {
                                NavigationBarItem(icon = { Icon(Icons.Default.Home, contentDescription = null) }, label = { Text("Home") }, selected = currentRoute == "home", onClick = { navController.navigate("home") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFE91E63), selectedTextColor = Color(0xFFE91E63), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray))
                                NavigationBarItem(icon = { Icon(Icons.Default.Search, contentDescription = null) }, label = { Text("Search") }, selected = currentRoute == "search", onClick = { navController.navigate("search") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFE91E63), selectedTextColor = Color(0xFFE91E63), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray))
                                NavigationBarItem(icon = { Icon(Icons.Default.List, contentDescription = null) }, label = { Text("Library") }, selected = currentRoute == "library", onClick = { }, colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray))
                                NavigationBarItem(icon = { Icon(Icons.Default.Star, contentDescription = null) }, label = { Text("AI") }, selected = currentRoute == "ai", onClick = { }, colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray))
                                NavigationBarItem(icon = { Icon(Icons.Default.Person, contentDescription = null) }, label = { Text("Profile") }, selected = currentRoute == "profile", onClick = { }, colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray))
                            }
                        }
                    },
                    containerColor = Color(0xFF0B131D)
                ) { padding ->
                    NavHost(navController = navController, startDestination = "home", modifier = Modifier.padding(padding)) {
                        composable("home") { HomeScreen(navController) }
                        composable("search") { SearchScreen() }
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
