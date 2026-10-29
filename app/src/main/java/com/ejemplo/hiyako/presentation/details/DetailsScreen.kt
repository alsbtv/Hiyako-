package com.ejemplo.hiyako.presentation.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage

@Composable
fun DetailsScreen(animeUrl: String, navController: NavController) {
    // Decode the URL to get the title and image for now (Mock details based on URL)
    val decodedUrl = java.net.URLDecoder.decode(animeUrl, "UTF-8")
    val parts = decodedUrl.split("|")
    val title = if(parts.isNotEmpty()) parts[0] else "Unknown Anime"
    val image = if(parts.size > 1) parts[1] else ""

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B131D))) {
        // Blurred Background (Simulated with Gradient)
        AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(350.dp).alpha(0.3f))
        Box(modifier = Modifier.fillMaxWidth().height(350.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF0B131D)))))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.height(100.dp))
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Button(onClick = { /* TODO: Play */ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))) {
                             Text("Watch Now", color = Color.White)
                         }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Episodes", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    // Mock Episode List
                    for (i in 1..12) {
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF15202B))) {
                            Text("Episode $i", color = Color.White, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}
