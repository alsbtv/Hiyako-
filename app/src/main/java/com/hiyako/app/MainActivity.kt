package com.hiyako.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.hiyako.app.ui.screens.ChatScreen
import com.hiyako.app.ui.screens.UpdaterScreen
import com.hiyako.app.ui.screens.WorkspaceScreen
import com.hiyako.app.ui.theme.AppTheme
import com.hiyako.app.ui.theme.OledBlack

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = OledBlack) {
                    var screenState by remember { mutableStateOf("chat") }
                    var refreshChatTrigger by remember { mutableIntStateOf(0) }
                    
                    when (screenState) {
                        "workspace" -> {
                            WorkspaceScreen(
                                onBack = { screenState = "chat" },
                                onRepoSelected = { 
                                    screenState = "chat"
                                    refreshChatTrigger++
                                },
                                onNavigateToUpdater = { screenState = "updater" }
                            )
                        }
                        "updater" -> {
                            UpdaterScreen(onBack = { screenState = "workspace" })
                        }
                        else -> {
                            key(refreshChatTrigger) {
                                ChatScreen(
                                    onNavigateToWorkspace = { screenState = "workspace" },
                                    onNavigateToUpdater = { screenState = "updater" }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
