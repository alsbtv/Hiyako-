package com.hiyako.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hiyako.app.ui.screens.AiViewModel
import com.hiyako.app.ui.screens.ChatScreen
import com.hiyako.app.ui.screens.MemoryScreen
import com.hiyako.app.ui.screens.SettingsScreen
import com.hiyako.app.ui.screens.UpdaterScreen
import com.hiyako.app.ui.screens.WorkspaceScreen
import com.hiyako.app.ui.theme.AppTheme
import com.hiyako.app.ui.theme.OledBlack
import com.hiyako.app.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val aiViewModel: AiViewModel = viewModel()
            val darkTheme = when (aiViewModel.themeMode) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }
            val density = LocalDensity.current

            CompositionLocalProvider(LocalDensity provides Density(density.density, aiViewModel.fontSize)) {
                AppTheme(darkTheme = darkTheme) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        AppRoot(aiViewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRoot(vm: AiViewModel) {
    var screenState by remember { mutableStateOf("chat") }
    var showEngineHost by remember { mutableStateOf(false) }

    // Create the hidden Puter WebView only after the first frame so app start-up is not delayed.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        showEngineHost = true
    }

    BackHandler(enabled = screenState != "chat") {
        screenState = if (screenState == "settings") "chat" else "settings"
    }

    Box(Modifier.fillMaxSize()) {
        when (screenState) {
            "workspace" -> WorkspaceScreen(
                onBack = { screenState = "settings" },
                onRepoSelected = { screenState = "chat" },
                onNavigateToUpdater = { screenState = "updater" }
            )
            "updater" -> UpdaterScreen(onBack = { screenState = "settings" })
            "memory" -> MemoryScreen(onBack = { screenState = "settings" }, vm = vm)
            "settings" -> SettingsScreen(
                onBack = { screenState = "chat" },
                onNavigateToWorkspace = { screenState = "workspace" },
                onNavigateToUpdater = { screenState = "updater" },
                onNavigateToMemory = { screenState = "memory" },
                onSignIn = {
                    vm.signIn()
                    screenState = "chat"
                },
                onClearChat = { vm.clearAllChats() },
                vm = vm
            )
            else -> ChatScreen(onNavigateToSettings = { screenState = "settings" }, vm = vm)
        }

        if (showEngineHost) {
            EngineHost(vm)
        }
    }
}

/**
 * Keeps the hidden Puter WebView attached for the whole app lifetime, and shows
 * Puter's sign-in popup window on top of everything when it asks for one.
 */
@Composable
private fun EngineHost(vm: AiViewModel) {
    val engine = vm.engine
    val popup by engine.popup.collectAsState()

    AndroidView(
        factory = { engine.webView },
        modifier = Modifier.size(1.dp).alpha(0f)
    )

    BackHandler(enabled = popup != null) { engine.closePopup() }

    val popupView = popup
    if (popupView != null) {
        Column(Modifier.fillMaxSize().background(OledBlack)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { engine.closePopup() }) {
                    Icon(Icons.Default.Close, "Close", tint = TextPrimary)
                }
                Text("Puter sign-in", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            AndroidView(
                factory = { popupView },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
    }
}
