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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import com.ejemplo.hiyako.scraper.ScrapedAnime
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hiyako", color = Color(0xFFE91E63), fontSize = 28.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { navController.navigate("search") }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B131D))
            )
        },
        containerColor = Color(0xFF0B131D)
    ) { paddingValues ->
        when (val state = uiState) {
            is HomeUiState.Loading -> Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFE91E63)) }
            is HomeUiState.Error -> Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) { Text(text = state.message, color = Color.White) }
            is HomeUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.padding(paddingValues).fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        Text("Featured Today", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                        AutoScrollingBanner(state.animeList.take(5), navController)
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Latest Episodes", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.animeList) { anime ->
                                AnimeCard(anime, navController)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AutoScrollingBanner(animeList: List<ScrapedAnime>, navController: NavController) {
    if (animeList.isEmpty()) return
    val pagerState = rememberPagerState { animeList.size }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % animeList.size)
        }
    }

    Box(modifier = Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 16.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val anime = animeList[page]
            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).clickable { 
                val encoded = java.net.URLEncoder.encode("${anime.title}|${anime.posterUrl}", "UTF-8")
                navController.navigate("details/$encoded")
            }) {
                AsyncImage(model = anime.posterUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC0B131D)))))
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(anime.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.background(Color(0xFFE91E63), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("On Going", color = Color.White, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                        Text(" 4.9", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
        Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(animeList.size) { index ->
                Box(modifier = Modifier.size(if (index == pagerState.currentPage) 8.dp else 6.dp).background(if (index == pagerState.currentPage) Color(0xFFE91E63) else Color.Gray, androidx.compose.foundation.shape.CircleShape))
            }
        }
    }
}

@Composable
fun AnimeCard(anime: ScrapedAnime, navController: NavController) {
    Column(modifier = Modifier.width(140.dp).clickable { 
        val encoded = java.net.URLEncoder.encode("${anime.title}|${anime.posterUrl}", "UTF-8")
        navController.navigate("details/$encoded")
    }) {
        Box(modifier = Modifier.height(200.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
            AsyncImage(model = anime.posterUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC0B131D)))))
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                Text(anime.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("الحلقة ${anime.episodeNumber}", color = Color(0xFF8899A6), fontSize = 10.sp)
            }
        }
    }
}
