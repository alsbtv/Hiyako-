package com.hiyako.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import com.hiyako.app.ui.screens.*
import com.hiyako.app.ui.theme.AppTheme
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            val aiViewModel: AiViewModel = viewModel()
            val darkTheme = when(aiViewModel.themeMode) { "Light" -> false; "Dark" -> true; else -> isSystemInDarkTheme() }
            val fontScale = aiViewModel.fontSize
            val density = LocalDensity.current

            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                AppTheme(darkTheme = darkTheme) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        var screenState by remember { mutableStateOf("chat") }
                        var refreshChatTrigger by remember { mutableIntStateOf(0) }
                        
                        when (screenState) {
                            "workspace" -> WorkspaceScreen(onBack = { screenState = "settings" }, onRepoSelected = { screenState = "chat"; refreshChatTrigger++ }, onNavigateToUpdater = { screenState = "updater" })
                            "updater" -> UpdaterScreen(onBack = { screenState = "settings" })
                            "memory" -> MemoryScreen(onBack = { screenState = "settings" }, vm = aiViewModel)
                            "settings" -> SettingsScreen(onBack = { screenState = "chat" }, onNavigateToWorkspace = { screenState = "workspace" }, onNavigateToUpdater = { screenState = "updater" }, onNavigateToMemory = { screenState = "memory" }, onSignIn = { screenState = "chat" }, onClearChat = { aiViewModel.clearAllChats() }, vm = aiViewModel)
                            else -> key(refreshChatTrigger) { ChatScreen(onNavigateToSettings = { screenState = "settings" }, vm = aiViewModel) }
                        }
                    }
                }
            }
        }
    }
}
