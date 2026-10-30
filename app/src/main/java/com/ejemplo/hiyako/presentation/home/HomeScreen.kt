package com.ejemplo.hiyako.presentation.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
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
import com.ejemplo.hiyako.presentation.theme.*
import kotlinx.coroutines.delay
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenDrawer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browse Anime", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = TextPrimary)
                    }
                    IconButton(onClick = { navController.navigate("search") }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        when (val state = uiState) {
            is HomeUiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentColor)
            }
            is HomeUiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = state.message, color = TextPrimary)
            }
            is HomeUiState.Success -> {
                HomeContent(state.animeList, paddingValues, navController)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeContent(animeList: List<ScrapedAnime>, paddingValues: PaddingValues, navController: NavController) {
    LazyColumn(
        modifier = Modifier.padding(paddingValues).fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Task 3: Hero Banner
        item {
            HeroBanner(animeList.take(5), navController)
        }

        // Task 5: Continue Watching
        item {
            SectionHeader("Continue Watching", onViewMore = {})
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(animeList.take(3)) { anime ->
                    ContinueWatchingCard(anime)
                }
            }
        }

        // Task 3: Recent Releases
        item {
            SectionHeader("Recent Releases", onViewMore = {})
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(animeList) { anime ->
                    PremiumAnimeCard(anime, navController)
                }
            }
        }

        // Task 5: Popular Genres
        item {
            SectionHeader("Popular Genres", onViewMore = {})
            PopularGenresGrid()
        }

        // More Sections
        val sections = listOf(
            "Award Winning Picks", "Recently Added", "Popular Chinese Picks", 
            "Highest Rating", "Most Liked Anime", "Most Watched Movies", 
            "Trending This Year", "Most Popular", "Latest Dub Releases", "Trending This Season"
        )

        sections.forEach { sectionTitle ->
            item {
                SectionHeader(sectionTitle, onViewMore = {})
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(animeList.shuffled()) { anime ->
                        PremiumAnimeCard(anime, navController)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroBanner(animeList: List<ScrapedAnime>, navController: NavController) {
    if (animeList.isEmpty()) return
    val pagerState = rememberPagerState { animeList.size }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(6000)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % animeList.size)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(240.dp).padding(16.dp)
        ) { page ->
            val anime = animeList[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        val encoded = URLEncoder.encode(anime.url, "UTF-8")
                        navController.navigate("details/$encoded")
                    }
            ) {
                AsyncImage(
                    model = anime.bannerUrl ?: anime.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f))))
                )
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(anime.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("Episode ${anime.episodeNumber ?: "1"}", color = Color.LightGray, fontSize = 14.sp)
                }
            }
        }
        
        // Pager Indicators
        Row(
            Modifier.height(10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(animeList.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) AccentColor else Color.Gray
                val width = if (pagerState.currentPage == iteration) 16.dp else 8.dp
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(width = width, height = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, onViewMore: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            "View More",
            color = TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.clickable { onViewMore() }
        )
    }
}

@Composable
fun PremiumAnimeCard(anime: ScrapedAnime, navController: NavController) {
    Column(modifier = Modifier.width(130.dp).clickable {
        val encoded = URLEncoder.encode(anime.url, "UTF-8")
        navController.navigate("details/$encoded")
    }) {
        Box(modifier = Modifier.height(190.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
            AsyncImage(
                model = anime.posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Badges
            val isOngoing = anime.episodeNumber?.toIntOrNull() != null // Mock check
            
            // Top-Left: Status
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                color = if (isOngoing) StatusOngoing else StatusCompleted,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = if (isOngoing) "On Going" else "Completed",
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            // Top-Right: Age Rating (Mock)
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                color = AccentColor,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("+17", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            }

            // Bottom-Left: Type & Year (Mock)
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                color = Color.Black.copy(0.6f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("TV - 2026", color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }

            // Bottom-Right: 3 dots
            IconButton(
                onClick = {},
                modifier = Modifier.align(Alignment.BottomEnd).size(30.dp)
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            
            // Episode Label (as seen in screenshots)
            if (isOngoing) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 32.dp),
                    color = Color.Black.copy(0.4f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("Episode : ${anime.episodeNumber}", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            anime.title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = StarColor, modifier = Modifier.size(14.dp))
            Text(" ${anime.score ?: 8.5}", color = TextSecondary, fontSize = 12.sp)
            Text(" (1.3M Vote)", color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
fun ContinueWatchingCard(anime: ScrapedAnime) {
    Surface(
        modifier = Modifier.width(240.dp).height(80.dp),
        color = SurfaceColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.05f))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = anime.posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(100.dp).fillMaxHeight()
            )
            Column(modifier = Modifier.padding(8.dp).weight(1f)) {
                Text("Episode ${anime.episodeNumber ?: "01"}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(anime.title, color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Progress Bar
                LinearProgressIndicator(
                    progress = 1.0f,
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = AccentColor,
                    trackColor = Color.DarkGray
                )
                Text("100.0%", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}

@Composable
fun PopularGenresGrid() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GenreCard("SHOUNEN", Color(0xFFFF9800), "https://art-manga.com/wp-content/uploads/2021/04/gon-freecss-character.png", Modifier.weight(1f))
            GenreCard("ROMANCE", Color(0xFFE91E63), "https://i.pinimg.com/originals/80/e5/2d/80e52d67d74068571891e4e3c3f972df.png", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GenreCard("ACTION", Color(0xFF2196F3), "https://static.wikia.nocookie.net/p__/images/e/e0/Goku_MUI.png", Modifier.weight(1f))
            GenreCard("DRAMA", Color(0xFF9C27B0), "https://i.pinimg.com/originals/0d/1d/1a/0d1d1a1f0a1f0a1f0a1f0a1f0a1f0a1f.png", Modifier.weight(1f))
        }
    }
}

@Composable
fun GenreCard(title: String, color: Color, imageUrl: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
    ) {
        Text(
            title,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp)
        )
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(60.dp),
            contentScale = ContentScale.Fit
        )
    }
}
