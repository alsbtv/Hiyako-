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
import androidx.compose.ui.window.Dialog
import com.hiyako.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit, onNavigateToWorkspace: () -> Unit, onNavigateToUpdater: () -> Unit, onNavigateToMemory: () -> Unit,
    onSignIn: () -> Unit, onClearChat: () -> Unit, vm: AiViewModel
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }

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
                    SettingsItem(icon = Icons.Default.Psychology, title = "Memory Manager", onClick = onNavigateToMemory)
                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(icon = Icons.Default.LightMode, title = "Appearance", value = vm.themeMode, onClick = { showThemeDialog = true })
                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(icon = Icons.Default.TextFields, title = "Font size", onClick = { showFontSizeDialog = true })
                }
            }
            item { SettingsGroup(title = "Data") { SettingsItem(icon = Icons.Default.Delete, title = "Clear Chat History", titleColor = Color.Red, iconColor = Color.Red, onClick = onClearChat) } }
            item { SettingsGroup(title = "Account") { SettingsItem(icon = Icons.Default.AccountCircle, title = "Puter Account / Sign In", onClick = onSignIn) } }
        }
    }

    if (showThemeDialog) {
        SelectionDialog(title = "Appearance", options = listOf("System", "Light", "Dark"), selectedOption = vm.themeMode, onDismiss = { showThemeDialog = false }, onSelect = { vm.updateTheme(it); showThemeDialog = false })
    }
    if (showFontSizeDialog) {
        Dialog(onDismissRequest = { showFontSizeDialog = false }) {
            Surface(shape = RoundedCornerShape(24.dp), color = CardDark) {
                Column(Modifier.padding(24.dp)) {
                    Text("Font Size", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Slider(value = vm.fontSize, onValueChange = { vm.updateFontSize(it) }, valueRange = 0.8f..1.5f, colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue))
                    Text("${(vm.fontSize * 100).toInt()}%", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.align(Alignment.End))
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { showFontSizeDialog = false }, modifier = Modifier.align(Alignment.End)) { Text("Done", color = AccentBlue) }
                }
            }
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

@Composable
fun SelectionDialog(title: String, options: List<String>, selectedOption: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    var selected by remember { mutableStateOf(selectedOption) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = CardDark) {
            Column(Modifier.padding(24.dp)) {
                Text(title, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                options.forEach { option ->
                    Row(Modifier.fillMaxWidth().clickable { selected = option }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selected == option, onClick = { selected = option }, colors = RadioButtonDefaults.colors(selectedColor = AccentBlue, unselectedColor = TextSecondary))
                        Spacer(Modifier.width(8.dp))
                        Text(option, color = TextPrimary, fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { onSelect(selected) }, modifier = Modifier.align(Alignment.End)) { Text("Confirm", color = AccentBlue) }
            }
        }
    }
}
