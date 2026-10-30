package com.ejemplo.hiyako.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ejemplo.hiyako.presentation.details.DetailsScreen
import com.ejemplo.hiyako.presentation.home.HomeScreen
import com.ejemplo.hiyako.presentation.library.LibraryScreen
import com.ejemplo.hiyako.presentation.player.PlayerScreen
import com.ejemplo.hiyako.presentation.profile.ProfileScreen
import com.ejemplo.hiyako.presentation.search.SearchScreen
import com.ejemplo.hiyako.presentation.settings.SettingsScreen
import com.ejemplo.hiyako.presentation.settings.SettingsViewModel
import com.ejemplo.hiyako.presentation.splash.SplashScreen
import com.ejemplo.hiyako.presentation.theme.HiyakoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeColor by settingsViewModel.themeColor.collectAsState()

            HiyakoTheme(themeColor = themeColor) {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showBottomBar = currentRoute in listOf("home", "library", "profile")
                val showDrawer = currentRoute != "splash"

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = showDrawer,
                    drawerContent = {
                        ModalDrawerSheet(drawerContainerColor = Color(0xFF0A0A0A), modifier = Modifier.width(300.dp)) {
                            DrawerContent(navController, drawerState, scope)
                        }
                    }
                ) {
                    Scaffold(
                        containerColor = Color(0xFF0F1115),
                        bottomBar = { if (showBottomBar) CustomBottomNav(navController, currentRoute) }
                    ) { padding ->
                        NavHost(navController = navController, startDestination = "splash", modifier = Modifier.padding(padding)) {
                            composable("splash") { SplashScreen(navController) }
                            composable("home") { HomeScreen(navController, onMenuClick = { scope.launch { drawerState.open() } }) }
                            composable("search") { SearchScreen(navController) }
                            composable("library") { LibraryScreen(navController, onMenuClick = { scope.launch { drawerState.open() } }) }
                            composable("profile") { ProfileScreen(navController, onMenuClick = { scope.launch { drawerState.open() } }) }
                            composable("settings") { SettingsScreen(navController) }
                            composable("details/{animeUrl}/{title}/{poster}") { backStackEntry ->
                                val animeUrl = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("animeUrl") ?: "", "UTF-8")
                                val title = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", "UTF-8")
                                val poster = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("poster") ?: "", "UTF-8")
                                DetailsScreen(animeUrl, title, poster, navController)
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
}

@Composable
fun DrawerContent(navController: NavController, drawerState: DrawerState, scope: kotlinx.coroutines.CoroutineScope) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Fake Profile Header
        Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))) {
            Text("Profile Header", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        val items = listOf(
            DrawerItem("Home", Icons.Default.Home, "home"),
            DrawerItem("Search", Icons.Default.Search, "search"),
            DrawerItem("Pick Random Anime", Icons.Default.Refresh, "random"),
            DrawerItem("Recent Releases", Icons.Default.PlayArrow, "recent"),
            DrawerItem("Trending Anime", Icons.Default.Star, "trending"),
            DrawerItem("Schedule", Icons.Default.DateRange, "schedule"),
            DrawerItem("Coming Soon", Icons.Default.Info, "coming"),
            DrawerItem("Library", Icons.Default.List, "library"),
            DrawerItem("Watch History", Icons.Default.History, "history"),
            DrawerItem("Downloads", Icons.Default.CloudDownload, "downloads"),
            DrawerItem("My List", Icons.Default.CheckCircle, "mylist"),
            DrawerItem("Settings", Icons.Default.Settings, "settings")
        )

        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                    scope.launch { drawerState.close() }
                    navController.navigate(item.route) { popUpTo("home") { inclusive = false } }
                }.padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(item.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(item.title, color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

data class DrawerItem(val title: String, val icon: ImageVector, val route: String)

@Composable
fun CustomBottomNav(navController: NavController, currentRoute: String?) {
    NavigationBar(containerColor = Color(0xFF1E1E1E), tonalElevation = 0.dp) {
        NavigationBarItem(icon = { Icon(Icons.Default.Home, contentDescription = null) }, label = { Text("Home") }, selected = currentRoute == "home", onClick = { navController.navigate("home") { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFE50046), selectedTextColor = Color(0xFFE50046), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray))
        
        NavigationBarItem(icon = { Icon(Icons.Default.List, contentDescription = null) }, label = { Text("Library") }, selected = currentRoute == "library", onClick = { navController.navigate("library") { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFE50046), selectedTextColor = Color(0xFFE50046), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray))
        
        NavigationBarItem(icon = { Icon(Icons.Default.Person, contentDescription = null) }, label = { Text("Profile") }, selected = currentRoute == "profile", onClick = { navController.navigate("profile") { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFE50046), selectedTextColor = Color(0xFFE50046), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray))
    }
}
