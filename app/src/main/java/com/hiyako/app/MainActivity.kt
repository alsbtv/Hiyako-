package com.hiyako.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.hiyako.app.ui.screens.ChatScreen
import com.hiyako.app.ui.theme.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) { delay(2000); showSplash = false }
                
                if (showSplash) {
                    Box(Modifier.fillMaxSize().background(OledBlack), contentAlignment = Alignment.Center) {
                        Text("بسام هرار", color = AccentBlue, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(Modifier.fillMaxSize(), color = OledBlack) { ChatScreen() }
                }
            }
        }
    }
}
