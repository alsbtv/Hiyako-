package com.hiyako.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.hiyako.app.ui.screens.ChatScreen
import com.hiyako.app.ui.theme.AppTheme
import com.hiyako.app.ui.theme.OledBlack
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppTheme { Surface(color = OledBlack) { ChatScreen() } } }
    }
}
