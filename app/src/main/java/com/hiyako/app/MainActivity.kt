package com.hiyako.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0F1115)) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Puter Chat", color = Color(0xFF00E5FF)) } } }
    }
}
