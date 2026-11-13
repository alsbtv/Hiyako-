package com.hiyako.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiyako.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigateToWorkspace: () -> Unit, onNavigateToUpdater: () -> Unit, onSignIn: () -> Unit, onClearChat: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings", color = TextPrimary, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = OledBlack)) },
        containerColor = OledBlack
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item { SettingsGroup(title = "Developer") { SettingsItem(icon = Icons.Default.Folder, title = "Workspace (GitHub)", onClick = onNavigateToWorkspace) } }
            item {
                SettingsGroup(title = "App") {
                    SettingsItem(icon = Icons.Default.Update, title = "Update App", onClick = onNavigateToUpdater)
                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(icon = Icons.Default.LightMode, title = "Appearance", value = "Dark", onClick = {})
                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(icon = Icons.Default.Language, title = "Language", value = "English", onClick = {})
                }
            }
            item {
                SettingsGroup(title = "Data") {
                    SettingsItem(icon = Icons.Default.Delete, title = "Clear Chat History", titleColor = Color.Red, iconColor = Color.Red, onClick = onClearChat)
                }
            }
            item { SettingsGroup(title = "Account") { SettingsItem(icon = Icons.Default.AccountCircle, title = "Puter Account / Sign In", onClick = onSignIn) } }
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 16.dp, bottom = 6.dp))
        Surface(color = CardDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) { Column { content() } }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, value: String? = null, titleColor: Color = TextPrimary, iconColor: Color = TextSecondary, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 14.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, color = titleColor, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (value != null) { Text(value, color = TextSecondary, fontSize = 14.sp); Spacer(Modifier.width(8.dp)) }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}
