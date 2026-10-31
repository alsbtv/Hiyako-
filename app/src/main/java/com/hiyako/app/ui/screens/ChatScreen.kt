package com.hiyako.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiyako.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf("Chat") }
    var showModelMenu by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = OledBlack, modifier = Modifier.width(300.dp)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).background(AccentCyan, CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Ahmed", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("@alsbtv", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                    Divider(color = CardDark)
                    Row(modifier = Modifier.fillMaxWidth().clickable { scope.launch { drawerState.close() } }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = "New Chat", tint = AccentCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("New Chat", color = AccentCyan, fontWeight = FontWeight.Bold)
                    }
                    Divider(color = CardDark)
                    Text("History", color = TextSecondary, modifier = Modifier.padding(16.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(5) { index ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Previous Chat ${index + 1}", color = TextPrimary, fontSize = 14.sp)
                            }
                        }
                    }
                    Divider(color = CardDark)
                    Column(modifier = Modifier.padding(bottom = 24.dp)) {
                        DrawerItem(icon = Icons.Default.Image, text = "Image Gen")
                        DrawerItem(icon = Icons.Default.VideoCall, text = "Video Gen")
                        DrawerItem(icon = Icons.Default.Folder, text = "Library")
                        DrawerItem(icon = Icons.Default.Settings, text = "Settings")
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = OledBlack,
            topBar = {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimary) }
                    Text("Puter Chat", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { }) { Icon(Icons.Default.Add, contentDescription = "New Chat", tint = TextPrimary) }
                }
            },
            bottomBar = {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Row(modifier = Modifier.background(CardDark, RoundedCornerShape(8.dp)).clickable { showModelMenu = true }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(selectedModel, color = TextPrimary, fontSize = 12.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                        }
                        DropdownMenu(expanded = showModelMenu, onDismissRequest = { showModelMenu = false }, modifier = Modifier.background(CardDark)) {
                            listOf("Chat", "Images", "Video", "Research", "Writing").forEach { model ->
                                DropdownMenuItem(text = { Text(model, color = TextPrimary) }, onClick = { selectedModel = model; showModelMenu = false })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = inputText, onValueChange = { inputText = it }, placeholder = { Text("Type...", color = TextSecondary, fontSize = 14.sp) },
                        modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = CardDark, unfocusedContainerColor = CardDark, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(45.dp).background(AccentCyan, RoundedCornerShape(8.dp)).clickable { }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black)
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Start a new conversation", color = TextSecondary)
            }
        }
    }
}

@Composable
fun DrawerItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(modifier = Modifier.fillMaxWidth().clickable { }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = text, tint = TextSecondary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = TextPrimary, fontSize = 14.sp)
    }
}
