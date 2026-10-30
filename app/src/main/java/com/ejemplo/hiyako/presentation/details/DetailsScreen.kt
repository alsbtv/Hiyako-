package com.ejemplo.hiyako.presentation.details

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun DetailsScreen(animeUrl: String, navController: NavController) {
    // Dummy data for exact UI replication
    val title = "Bleach: Sennen Kessen-hen - Kashin-tan"
    val altTitle = "Bleach: Thousand-year Blood War - The Calamity"
    val nativeTitle = "BLEACH 千年血戦篇-禍進譚-"
    val story = "Final Part Of Bleach: Sennen Kessen-hen."
    
    val images = listOf(
        "https://s4.anilist.co/file/anilistcdn/media/anime/banner/116674-ZXyP8fK5Jz8Z.jpg",
        "https://s4.anilist.co/file/anilistcdn/media/anime/banner/116674-w4Xz3jT4rP8Y.jpg"
    )
    var currentBgIndex by remember { mutableStateOf(0) }
    val clipboardManager = LocalClipboardManager.current

    // Background changing every 7 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(7000)
            currentBgIndex = (currentBgIndex + 1) % images.size
        }
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Details", "Episodes", "Watch History", "Favorite Episodes", "Gallery", "More Like This")

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F1115))) {
        // 1. Blurred Changing Background
        Crossfade(targetState = images[currentBgIndex], animationSpec = tween(2000), label = "bg") { image ->
            AsyncImage(
                model = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(500.dp).blur(30.dp).alpha(0.6f)
            )
        }
        // Gradient overlay to darken bottom
        Box(
            modifier = Modifier.fillMaxWidth().height(500.dp).background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF0F1115)))
            )
        )

        // 2. Main Content
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                TopBar(navController)
                HeaderSection(title)
            }
            item {
                ActionButtonsRow()
                Spacer(modifier = Modifier.height(16.dp))
                TabsRow(tabs, selectedTab) { selectedTab = it }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Tab Content
            if (selectedTab == 0) {
                item {
                    WarningBanner()
                    Spacer(modifier = Modifier.height(16.dp))
                    InfoSection(title, altTitle, nativeTitle, story, clipboardManager)
                    GenresSection()
                    StudiosSection()
                    StatisticsSection()
                    Spacer(modifier = Modifier.height(24.dp))
                    CharactersSection()
                    Spacer(modifier = Modifier.height(24.dp))
                    RelatedAnimesSection()
                }
            } else if (selectedTab == 1) {
                items(10) { index ->
                    EpisodeItem(index + 1)
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) } // Bottom padding
        }
    }
}

@Composable
fun TopBar(navController: NavController) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.background(Color(0x80000000), CircleShape)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = { navController.navigate("player/https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8") }, modifier = Modifier.background(Color(0x80000000), CircleShape)) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = Color.White)
            }
            IconButton(onClick = { navController.navigate("player/https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8") }, modifier = Modifier.background(Color(0x80000000), CircleShape)) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
            }
        }
    }
}

@Composable
fun HeaderSection(title: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        // Poster
        Box {
            AsyncImage(
                model = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx116674-2P4wLh56yH2W.jpg",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(120.dp).height(180.dp).clip(RoundedCornerShape(8.dp))
            )
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).background(Color(0xFFE50046), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("17+", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Spacer(modifier = Modifier.height(8.dp))
            Text("TV • Summer 2026", color = Color.LightGray, fontSize = 12.sp)
            Text("10 Episode", color = Color.LightGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                Text(" N/A", color = Color.LightGray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE50046), modifier = Modifier.size(16.dp))
                Text(" 3K", color = Color.LightGray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.background(Color(0xFFE50046), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("Completed", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Ratings
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Box(modifier = Modifier.background(Color(0xFF2E51A2), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("MAL", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("8.92 /10", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("56K", color = Color.Gray, fontSize = 10.sp)
                }
                Column {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("9.63 /10", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("1K", color = Color.Gray, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun ActionButtonsRow() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50046)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f).height(45.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Play Now", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        Icon(Icons.Default.StarBorder, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        Icon(Icons.Default.Subject, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun TabsRow(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tabs.size) { index ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .background(if (isSelected) Color(0xFF2E7D32) else Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                    .clickable { onSelect(index) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(tabs[index], color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun WarningBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Color(0xFFE50046), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            "This anime may contain sexual, violence, or mature scenes that are not suitable for children or family viewing. Viewer discretion is advised.",
            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun InfoSection(title: String, altTitle: String, nativeTitle: String, story: String, clipboard: androidx.compose.ui.platform.ClipboardManager) {
    Column(modifier = Modifier.padding(16.dp)) {
        InfoItem("Title", title, clipboard)
        InfoItem("Alternative Title", altTitle, clipboard)
        InfoItem("Native Title", nativeTitle, clipboard)
        InfoItem("Story", story, clipboard)
        InfoItem("Content Advisor", "17+ May contain profanity, violence scenes\nParental guidance recommended", clipboard)
        InfoItem("Material Source", "Adapted From Manga", clipboard)
        InfoItem("Country Of Origin", "Japanese", clipboard)
    }
}

@Composable
fun InfoItem(label: String, value: String, clipboard: androidx.compose.ui.platform.ClipboardManager) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
            value, 
            color = Color.LightGray, 
            fontSize = 14.sp,
            modifier = Modifier.clickable { clipboard.setText(AnnotatedString(value)) }
        )
    }
}

@Composable
fun GenresSection() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("Genres", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("Action", "Adventure", "Shounen", "Supernatural")) { genre ->
                Box(
                    modifier = Modifier.background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp)).clickable { /* TODO: Navigate to genre */ }.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(genre, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun StudiosSection() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Studios", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier.background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Pierrot Films", color = Color.White)
        }
    }
}

@Composable
fun StatisticsSection() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Statics", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatItem(Icons.Default.PlayArrow, "Watching: 78K (41.62%)", Color(0xFF00BCD4))
                StatItem(Icons.Default.Check, "Completed: 31K (16.29%)", Color(0xFF4CAF50))
                StatItem(Icons.Default.Pause, "On Hold: 2K (0.97%)", Color(0xFFFFC107))
                StatItem(Icons.Default.Close, "Dropped: 617 (0.33%)", Color(0xFFE50046))
                StatItem(Icons.Default.Bookmark, "Plan To Watch: 77K (40.79%)", Color(0xFF00BCD4))
            }
            // Simple Donut Chart representation
            Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(color = Color(0xFF00BCD4), startAngle = 0f, sweepAngle = 150f, useCenter = false, style = Stroke(width = 30f))
                    drawArc(color = Color(0xFF4CAF50), startAngle = 150f, sweepAngle = 60f, useCenter = false, style = Stroke(width = 30f))
                    drawArc(color = Color(0xFFE50046), startAngle = 210f, sweepAngle = 150f, useCenter = false, style = Stroke(width = 30f))
                }
            }
        }
    }
}

@Composable
fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = Color.LightGray, fontSize = 12.sp)
    }
}

@Composable
fun CharactersSection() {
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Characters", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("View All", color = Color.LightGray, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val chars = listOf("Kurosaki, Ichigo", "Aizen, Sousuke", "Urahara, Kisuke", "Zaraki, Kenpachi", "Kuchi, Rukia")
            items(chars) { char ->
                Column(modifier = Modifier.width(100.dp)) {
                    Box(modifier = Modifier.height(140.dp).fillMaxWidth().background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                         Icon(Icons.Default.ImageNotSupported, contentDescription = null, tint = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(char, color = Color.White, fontSize = 12.sp, maxLines = 2)
                    Text("39K", color = Color(0xFFE50046), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun RelatedAnimesSection() {
    Column {
        Text("Related Animes", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(modifier = Modifier.width(140.dp)) {
                    Box {
                        AsyncImage(
                            model = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx116674-2P4wLh56yH2W.jpg",
                            contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.height(200.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        )
                        Box(modifier = Modifier.padding(8.dp).background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp)) { Text("Prequel", color = Color.White, fontSize = 10.sp) }
                    }
                    Text("Bleach: Sennen Kessen-h...", color = Color.White, fontSize = 12.sp, maxLines = 1)
                    Text("★ 8.67 (180K Vote)", color = Color.Gray, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun EpisodeItem(index: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx116674-2P4wLh56yH2W.jpg",
                contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.width(120.dp).height(70.dp).clip(RoundedCornerShape(4.dp))
            )
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).background(Color(0xCC000000), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                Text("24:35", color = Color.White, fontSize = 10.sp)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text("Episode $index", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Button(onClick = { navController.navigate("player/https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50046)), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            Text("Play Now", color = Color.White, fontSize = 12.sp)
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}
