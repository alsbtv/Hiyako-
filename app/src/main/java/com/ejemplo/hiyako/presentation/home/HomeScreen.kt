package com.ejemplo.hiyako.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ejemplo.hiyako.data.model.ScrapedAnime

@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFF0F1115)), contentPadding = PaddingValues(16.dp)) {
        item {
            Text("Latest Episodes", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        }
        when (val state = uiState) {
            is HomeUiState.Loading -> item { CircularProgressIndicator(color = Color(0xFFE50046)) }
            is HomeUiState.Error -> item { Text(state.message, color = Color.Red) }
            is HomeUiState.Success -> {
                items(state.animeList.chunked(3)) { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowItems.forEach { anime ->
                            Column(modifier = Modifier.weight(1f).clickable {
                                val encUrl = java.net.URLEncoder.encode(anime.url, "UTF-8")
                                val encTitle = java.net.URLEncoder.encode(anime.title, "UTF-8")
                                val encPoster = java.net.URLEncoder.encode(anime.posterUrl, "UTF-8")
                                navController.navigate("details/$encUrl/$encTitle/$encPoster")
                            }) {
                                AsyncImage(model = anime.posterUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.height(180.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)))
                                Text(anime.title, color = Color.White, maxLines = 2, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                        if (rowItems.size < 3) { repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) } }
                    }
                }
            }
        }
    }
}
