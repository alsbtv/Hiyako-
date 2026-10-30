package com.ejemplo.hiyako.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ejemplo.hiyako.presentation.details.DetailsScreen
import com.ejemplo.hiyako.presentation.home.HomeScreen
import com.ejemplo.hiyako.presentation.home.HomeViewModel
import com.ejemplo.hiyako.presentation.player.PlayerScreen
import com.ejemplo.hiyako.presentation.theme.HiyakoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HiyakoTheme {
                val navController = rememberNavController()
                val homeViewModel: HomeViewModel = hiltViewModel()
                Scaffold(containerColor = Color(0xFF0F1115)) { padding ->
                    NavHost(navController = navController, startDestination = "home", modifier = androidx.compose.ui.Modifier.padding(padding)) {
                        composable("home") { HomeScreen(navController, homeViewModel) }
                        composable("details/{url}/{title}/{poster}") { backStackEntry ->
                            val url = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", "UTF-8")
                            val title = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", "UTF-8")
                            val poster = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("poster") ?: "", "UTF-8")
                            DetailsScreen(url, title, poster, navController)
                        }
                        composable("player/{videoUrl}") { backStackEntry ->
                            val videoUrl = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("videoUrl") ?: "", "UTF-8")
                            PlayerScreen(videoUrl, navController)
                        }
                    }
                }
            }
        }
    }
}
