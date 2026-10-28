package com.ejemplo.hiyako.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ejemplo.hiyako.presentation.theme.DarkBackground
import com.ejemplo.hiyako.presentation.theme.DarkSurface
import com.ejemplo.hiyako.presentation.theme.PrimaryPink
import com.ejemplo.hiyako.presentation.theme.TextPrimary
import com.ejemplo.hiyako.presentation.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSearchClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PrimaryPink
                )
            }
            is HomeUiState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Error: ${state.message}", color = PrimaryPink)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        onClick = { viewModel.loadLatestAnime() },
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Text(
                            text = "Retry",
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            is HomeUiState.Success -> {
                val animeList = state.animeList
                val heroList = animeList.take(5)
                val watchHistoryList = animeList.take(3)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // A. TopAppBar
                    item {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Hiyako",
                                    color = PrimaryPink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            },
                            actions = {
                                IconButton(onClick = onSearchClick) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = TextPrimary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent
                            ),
                            modifier = Modifier.height(52.dp)
                        )
                    }

                    // B. Hero Carousel
                    item {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "Featured Today",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(heroList) { anime ->
                                    Box(
                                        modifier = Modifier
                                            .width(300.dp)
                                            .height(240.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(DarkSurface)
                                    ) {
                                        AsyncImage(
                                            model = anime.posterUrl,
                                            contentDescription = anime.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(Color.Transparent, DarkBackground.copy(alpha = 0.9f)),
                                                        startY = 100f
                                                    )
                                                )
                                        )
                                        Column(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(16.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(PrimaryPink)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(text = "On Going", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(text = "4.9", color = TextSecondary, fontSize = 12.sp)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = anime.title,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // C. Watch History
                    item {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text(
                                text = "Continue Watching",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(watchHistoryList) { anime ->
                                    Column(
                                        modifier = Modifier.width(120.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(120.dp)
                                                .height(160.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(DarkSurface)
                                        ) {
                                            AsyncImage(
                                                model = anime.posterUrl,
                                                contentDescription = anime.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = anime.title,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { 0.65f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            color = PrimaryPink,
                                            trackColor = DarkSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // D. Categories
                    item {
                        Column(modifier = Modifier.padding(top = 20.dp, start = 16.dp, end = 16.dp)) {
                            Text(
                                text = "Categories",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            val chunkedCategories = categories.chunked(2)
                            chunkedCategories.forEach { rowCategories ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowCategories.forEach { cat ->
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp)
                                                .clickable { }
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = cat,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // E. Most Popular
                    item {
                        Column(modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp)) {
                            Text(
                                text = "Most Popular",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }

                    item {
                        val gridItems = animeList.chunked(2)
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            gridItems.forEach { rowList ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowList.forEach { anime ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(200.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(DarkSurface)
                                        ) {
                                            AsyncImage(
                                                model = anime.posterUrl,
                                                contentDescription = anime.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(Color.Transparent, DarkBackground.copy(alpha = 0.9f)),
                                                            startY = 80f
                                                        )
                                                    )
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .align(Alignment.TopStart)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(PrimaryPink)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(text = "On Going", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Column(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(10.dp)
                                            ) {
                                                Text(
                                                    text = anime.title,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    maxLines = 1
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(text = "4.8", color = TextSecondary, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                    if (rowList.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
