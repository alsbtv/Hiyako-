package com.ejemplo.hiyako.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ejemplo.hiyako.presentation.settings.SettingsViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, viewModel: SettingsViewModel = hiltViewModel()) {
    val themeColor by viewModel.themeColor.collectAsState()
    val titleLanguage by viewModel.titleLanguage.collectAsState()
    val seekDuration by viewModel.seekDuration.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("Settings", color = Color.White) },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            item {
                Text("App Theme", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    listOf("Red", "Green", "Purple", "Blue").forEach { color ->
                        Button(onClick = { viewModel.setTheme(color) }, colors = ButtonDefaults.buttonColors(containerColor = if (themeColor == color) MaterialTheme.colorScheme.primary else Color.DarkGray)) {
                            Text(color, color = Color.White)
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Anime Titles Language", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    listOf("Japanese", "English").forEach { lang ->
                        Button(onClick = { viewModel.setLanguage(lang) }, colors = ButtonDefaults.buttonColors(containerColor = if (titleLanguage == lang) MaterialTheme.colorScheme.primary else Color.DarkGray)) {
                            Text(lang, color = Color.White)
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Seek Duration (Seconds)", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    listOf("5", "10", "15", "30", "90").forEach { duration ->
                        Button(onClick = { viewModel.setSeek(duration) }, colors = ButtonDefaults.buttonColors(containerColor = if (seekDuration == duration) MaterialTheme.colorScheme.primary else Color.DarkGray), contentPadding = PaddingValues(0.dp), modifier = Modifier.size(45.dp)) {
                            Text(duration, color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
