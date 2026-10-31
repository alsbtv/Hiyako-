package com.hiyako.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.hiyako.app.ui.screens.ChatScreen
import com.hiyako.app.ui.screens.WorkspaceScreen
import com.hiyako.app.ui.theme.AppTheme
import com.hiyako.app.ui.theme.OledBlack

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = OledBlack) {
                    var showWorkspace by remember { mutableStateOf(false) }
                    var refreshChatTrigger by remember { mutableStateOf(0) }
                    
                    if (showWorkspace) {
                        WorkspaceScreen(
                            onBack = { showWorkspace = false },
                            onRepoSelected = { 
                                showWorkspace = false 
                                refreshChatTrigger++ // لإجبار الشات على التحديث
                            }
                        )
                    } else {
                        key(refreshChatTrigger) {
                            ChatScreen(onNavigateToWorkspace = { showWorkspace = true })
                        }
                    }
                }
            }
        }
    }
}
