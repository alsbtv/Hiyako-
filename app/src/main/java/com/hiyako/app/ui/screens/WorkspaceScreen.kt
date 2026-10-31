package com.hiyako.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hiyako.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(onBack: () -> Unit, onRepoSelected: () -> Unit, vm: WorkspaceViewModel = viewModel()) {
    val context = LocalContext.current
    val token by vm.token.collectAsState()
    val repos by vm.repos.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val loadingMessage by vm.loadingMessage.collectAsState()
    var inputToken by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.loadToken(context) }

    Scaffold(
        containerColor = OledBlack,
        topBar = {
            TopAppBar(
                title = { Text("Workspace (GitHub)", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OledBlack)
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            if (isLoading) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentBlue)
                    Spacer(Modifier.height(16.dp))
                    Text(loadingMessage, color = TextSecondary)
                }
            } else if (token.isBlank()) {
                Column {
                    Text("Enter your GitHub Personal Access Token to connect:", color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputToken, onValueChange = { inputToken = it }, placeholder = { Text("ghp_...", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = CardDark, unfocusedContainerColor = CardDark, focusedBorderColor = AccentBlue, unfocusedBorderColor = Color.Transparent, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { vm.saveToken(context, inputToken) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentBlue), shape = RoundedCornerShape(8.dp)) {
                        Text("Connect to GitHub", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Your Repositories", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { vm.fetchRepos() }) { Text("Refresh", color = AccentBlue) }
                    }
                    Spacer(Modifier.height(8.dp))
                    
                    if (repos.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No repositories found.", color = TextSecondary) }
                    } else {
                        LazyColumn {
                            items(repos) { repo ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { vm.selectRepoAndLoadFiles(repo) { onRepoSelected() } }.padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Folder, null, tint = AccentBlue)
                                    Spacer(Modifier.width(12.dp))
                                    Text(repo.fullName, color = TextPrimary, fontSize = 16.sp)
                                }
                                Divider(color = CardDark)
                            }
                        }
                    }
                }
            }
        }
    }
}
