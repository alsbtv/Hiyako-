package com.ejemplo.hiyako.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.activity.compose.setContent
import com.ejemplo.hiyako.presentation.home.HomeScreen
import com.ejemplo.hiyako.presentation.theme.HiyakoTheme
import dagger.hilt.android.AndroidEntryPoint

data class BottomNavItem(val name: String, val icon: ImageVector)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HiyakoTheme {
                val items = listOf(
                    BottomNavItem("الرئيسية", Icons.Default.Home),
                    BottomNavItem("البحث", Icons.Default.Search),
                    BottomNavItem("المكتبة", Icons.Default.List),
                    BottomNavItem("AI", Icons.Default.Star),
                    BottomNavItem("الملف", Icons.Default.Person)
                )
                var selectedItem by remember { mutableIntStateOf(0) }

                Scaffold(
                    bottomBar = {
                        NavigationBar(containerColor = Color(0xFF0B131D)) {
                            items.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    selected = selectedItem == index,
                                    onClick = { selectedItem = index },
                                    label = { Text(item.name) },
                                    icon = { Icon(item.icon, contentDescription = item.name) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFFE91E63),
                                        unselectedIconColor = Color.Gray
                                    )
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    when (selectedItem) {
                        0 -> HomeScreen()
                        else -> Text("Section ${items[selectedItem].name}", modifier = Modifier.padding(paddingValues))
                    }
                }
            }
        }
    }
}
