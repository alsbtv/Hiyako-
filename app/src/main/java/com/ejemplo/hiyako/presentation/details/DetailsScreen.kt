package com.ejemplo.hiyako.presentation.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ejemplo.hiyako.scraper.WitAnimeScraper
import kotlinx.coroutines.launch

@Composable
fun DetailsScreen(
    animeUrl: String, 
    title: String, 
    poster: String, 
    navController: NavController,
    scraper: WitAnimeScraper = hiltViewModel<DetailsViewModel>().scraper
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F1115))) {
        AsyncImage(model = poster, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(400.dp).blur(20.dp))
        Box(modifier = Modifier.fillMaxWidth().height(400.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF0F1115)))))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.padding(16.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.height(100.dp))
                Row(modifier = Modifier.padding(16.dp)) {
                    AsyncImage(model = poster, contentDescription = null, modifier = Modifier.width(120.dp).height(180.dp).clip(RoundedCornerShape(8.dp)))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 3)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Anime3rb Source", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                
                Button(
                    onClick = { 
                        isLoading = true
                        scope.launch {
                            val realUrl = scraper.getVideoUrl(animeUrl)
                            isLoading = false
                            if (realUrl != null) {
                                navController.navigate("player/${java.net.URLEncoder.encode(realUrl, "UTF-8")}")
                            } else {
                                navController.navigate("player/${java.net.URLEncoder.encode("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "UTF-8")}")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50046)),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Watch Now", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                
                Text("Episodes", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
            }
            items(12) { index ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Episode ${index + 1}", color = Color.White, modifier = Modifier.weight(1f))
                        IconButton(onClick = { 
                            isLoading = true
                            scope.launch {
                                val realUrl = scraper.getVideoUrl(animeUrl)
                                isLoading = false
                                if (realUrl != null) {
                                    navController.navigate("player/${java.net.URLEncoder.encode(realUrl, "UTF-8")}")
                                } else {
                                    navController.navigate("player/${java.net.URLEncoder.encode("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "UTF-8")}")
                                }
                            }
                        }) {
                            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFFE50046))
                            else Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFE50046))
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(50.dp)) }
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class DetailsViewModel @javax.inject.Inject constructor(
    val scraper: WitAnimeScraper
) : androidx.lifecycle.ViewModel()
