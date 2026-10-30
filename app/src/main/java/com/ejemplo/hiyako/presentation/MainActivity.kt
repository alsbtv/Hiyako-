package com.ejemplo.hiyako.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.ejemplo.hiyako.presentation.details.DetailsScreen
import com.ejemplo.hiyako.presentation.home.HomeScreen
import com.ejemplo.hiyako.presentation.player.PlayerScreen
import com.ejemplo.hiyako.presentation.news.NewsScreen
import com.ejemplo.hiyako.presentation.search.SearchScreen
import com.ejemplo.hiyako.presentation.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HiyakoTheme {
                MainContainer()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DarkBackground,
                drawerContentColor = TextPrimary,
                modifier = Modifier.width(300.dp)
            ) {
                DrawerContent(
                    onClose = { scope.launch { drawerState.close() } },
                    onNavigate = { route ->
                        navController.navigate(route)
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                if (currentRoute != "details/{animeUrl}") {
                    FloatingBottomNav(navController, currentRoute)
                }
            },
            containerColor = DarkBackground
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(padding)
            ) {
                composable("home") { HomeScreen(navController, onOpenDrawer = { scope.launch { drawerState.open() } }) }
                composable("search") { SearchScreen() }
                composable("news") { NewsScreen() }
                composable("details/{animeUrl}") { backStackEntry ->
                        composable("player/{videoUrl}") { backEntry ->
                            val videoUrl = backEntry.arguments?.getString("videoUrl") ?: ""
                            PlayerScreen(videoUrl, navController)
                        }
                    val url = backStackEntry.arguments?.getString("animeUrl") ?: ""
                    DetailsScreen(url, navController)
                }
            }
        }
    }
}

@Composable
fun DrawerContent(onClose: () -> Unit, onNavigate: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                // Header Cover
                AsyncImage(
                    model = "https://images.alphacoders.com/132/1322232.jpeg",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
                
                // Close button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(0.4f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                // Profile Info
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    AsyncImage(
                        model = "https://i.pinimg.com/736x/8f/90/1c/8f901c9a4441584c6c245942738a9e0a.jpg",
                        contentDescription = null,
                        modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.Gray),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("alsb_tv", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("@alsb_tv", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }

        val items = listOf(
            DrawerItemData("Home", "home", Icons.Default.Home),
            DrawerItemData("Search", "search", Icons.Default.Search),
            DrawerItemData("Pick Random Anime", "home", Icons.Default.Shuffle),
            DrawerItemData("Competitions & Prizes", "home", Icons.Default.EmojiEvents),
            DrawerItemData("Referral & Rewards", "home", Icons.Default.Redeem),
            DrawerItemData("Recent Releases", "home", Icons.Default.NewReleases),
            DrawerItemData("Recent Dub Releases", "home", Icons.Default.Mic),
            DrawerItemData("Trending Anime", "home", Icons.Default.TrendingUp),
            DrawerItemData("Schedule", "home", Icons.Default.CalendarToday),
            DrawerItemData("Coming Soon", "home", Icons.Default.Upcoming),
            DrawerItemData("Library", "home", Icons.Default.VideoLibrary),
            DrawerItemData("Seasons", "home", Icons.Default.ViewCarousel),
            DrawerItemData("Watch History", "home", Icons.Default.History),
            DrawerItemData("Downloads/Offline", "home", Icons.Default.Download),
            DrawerItemData("My List", "home", Icons.Default.Bookmark),
            DrawerItemData("Custom Lists", "home", Icons.Default.FormatListBulleted),
            DrawerItemData("Notified Anime", "home", Icons.Default.Notifications),
            DrawerItemData("Popular Characters", "home", Icons.Default.People),
            DrawerItemData("Favorite Characters", "home", Icons.Default.Favorite),
            DrawerItemData("Our Other Apps", "home", Icons.Default.Apps),
            DrawerItemData("Settings", "home", Icons.Default.Settings)
        )

        items(items) { item ->
            DrawerItem(item) { onNavigate(item.route) }
        }
    }
}

data class DrawerItemData(val label: String, val route: String, val icon: ImageVector)

@Composable
fun DrawerItem(item: DrawerItemData, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(item.icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(item.label, color = TextPrimary, fontSize = 16.sp)
    }
}

@Composable
fun FloatingBottomNav(navController: NavController, currentRoute: String?) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.wrapContentSize().clip(RoundedCornerShape(40.dp)),
            color = Color.Black.copy(alpha = 0.8f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                BottomNavItem(Icons.Default.Home, selected = currentRoute == "home") { navController.navigate("home") }
                BottomNavItem(Icons.Default.Bookmark, selected = currentRoute == "mylist") { }
                BottomNavItem(Icons.Default.VideoLibrary, selected = currentRoute == "library") { }
                // Profile circular image
                AsyncImage(
                    model = "https://i.pinimg.com/736x/8f/90/1c/8f901c9a4441584c6c245942738a9e0a.jpg",
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(CircleShape).clickable { },
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun BottomNavItem(icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = null, tint = if (selected) AccentColor else Color.Gray)
    }
}
