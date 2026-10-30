package com.ejemplo.hiyako.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ejemplo.hiyako.data.model.ScrapedAnime
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(navController: NavController, onMenuClick: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState { 5 }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(11000)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % 5)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White) }
                Text("Browse Anime", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = { }) { Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White) }
                    IconButton(onClick = { navController.navigate("search") }) { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) }
                }
            }

            val bannerAnime = (uiState as? HomeUiState.Success)?.animeList?.take(5) ?: emptyList()
            Box(modifier = Modifier.fillMaxWidth().height(250.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp))) {
                if (bannerAnime.isNotEmpty()) {
                    HorizontalPager(state = pagerState) { page ->
                        val anime = bannerAnime.getOrNull(page) ?: bannerAnime.first()
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(model = anime.bannerUrl ?: anime.posterUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC0F1115)))))
                            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                                Text(anime.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                                Text("Episode ${anime.episodeNumber}", color = Color.LightGray, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        when (val state = uiState) {
            is HomeUiState.Loading -> item { Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) } }
            is HomeUiState.Error -> item { Text(state.message, color = Color.Red, modifier = Modifier.padding(16.dp)) }
            is HomeUiState.Success -> {
                item {
                    SectionHeader("Recently Added")
                    LazyRow(contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.animeList) { anime -> AnimeCard(anime, navController) }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("View More", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.clickable { })
    }
}

@Composable
fun AnimeCard(anime: ScrapedAnime, navController: NavController) {
    Column(modifier = Modifier.width(130.dp).clickable {
        val route = "details/${java.net.URLEncoder.encode(anime.url, "UTF-8")}/${java.net.URLEncoder.encode(anime.title, "UTF-8")}/${java.net.URLEncoder.encode(anime.posterUrl, "UTF-8")}"
        navController.navigate(route)
    }) {
        Box(modifier = Modifier.height(180.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp))) {
            AsyncImage(model = anime.posterUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            val badgeColor = if (anime.sourceName == "Anime4Up") Color(0xFF2196F3) else Color(0xFF4CAF50)
            Box(modifier = Modifier.align(Alignment.TopStart).padding(4.dp).background(badgeColor, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                Text(anime.sourceName, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                Text("17+", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(anime.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
            Text(" ${anime.score ?: "N/A"}", color = Color.Gray, fontSize = 10.sp)
        }
    }
}
