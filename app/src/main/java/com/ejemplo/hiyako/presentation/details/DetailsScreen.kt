package com.ejemplo.hiyako.presentation.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ejemplo.hiyako.data.model.Character
import com.ejemplo.hiyako.data.model.Episode
import com.ejemplo.hiyako.data.api.AniListApi
import com.ejemplo.hiyako.data.api.JikanApi
import com.ejemplo.hiyako.presentation.theme.*
import kotlinx.coroutines.launch

@Composable
fun DetailsScreen(
    animeUrl: String, 
    navController: NavController,
    anilistApi: AniListApi = AniListApi(),
    jikanApi: JikanApi = JikanApi()
) {
    val decodedUrl = java.net.URLDecoder.decode(animeUrl, "UTF-8")
    val parts = decodedUrl.split("|")
    val title = if(parts.isNotEmpty()) parts[0] else "Unknown Anime"
    val defaultImage = if(parts.size > 1) parts[1] else ""

    var synopsis by remember { mutableStateOf<String?>(null) }
    var score by remember { mutableStateOf<Double?>(null) }
    var studios by remember { mutableStateOf<List<String>>(emptyList()) }
    var genres by remember { mutableStateOf<List<String>>(emptyList()) }
    var episodesList by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var charactersList by remember { mutableStateOf<List<Character>>(emptyList()) }
    var bannerUrl by remember { mutableStateOf(defaultImage) }
    var posterUrl by remember { mutableStateOf(defaultImage) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(title) {
        coroutineScope.launch {
            val richData = anilistApi.getAnimeDetails(title) ?: jikanApi.getAnimeDetails(title)
            if (richData != null) {
                synopsis = richData.synopsis
                score = richData.score
                if (!richData.studios.isNullOrEmpty()) studios = richData.studios!!
                if (!richData.genres.isNullOrEmpty()) genres = richData.genres!!
                if (richData.episodesList.isNotEmpty()) episodesList = richData.episodesList
                if (richData.characters.isNotEmpty()) charactersList = richData.characters
                if (!richData.bannerUrl.isNullOrEmpty()) bannerUrl = richData.bannerUrl!!
                if (richData.posterUrl.isNotBlank()) posterUrl = richData.posterUrl
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        AsyncImage(model = bannerUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(350.dp).alpha(0.3f))
        Box(modifier = Modifier.fillMaxWidth().height(350.dp).background(Brush.verticalGradient(listOf(Color.Transparent, DarkBackground))))

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.height(100.dp))
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(title, color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (score != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = StarColor, modifier = Modifier.size(16.dp))
                                Text(" $score", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (studios.isNotEmpty()) {
                            Text(studios.joinToString(", "), color = AccentColor, fontSize = 14.sp)
                        }
                    }

                    if (genres.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            genres.take(3).forEach { genre ->
                                Surface(
                                    color = SurfaceColor,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(text = genre, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }

                    if (!synopsis.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Synopsis", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(synopsis!!, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Watch Now", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (charactersList.isNotEmpty()) {
                item {
                    Text("Characters", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(charactersList) { character ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(90.dp)) {
                                AsyncImage(model = character.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(80.dp).clip(CircleShape).background(SurfaceColor))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(character.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.padding(horizontal = 4.dp))
                                Text(character.role, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }

            if (episodesList.isNotEmpty()) {
                item {
                    Text("Episodes", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                }
                items(episodesList) { episode ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).background(SurfaceColor, RoundedCornerShape(12.dp)).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(model = episode.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(100.dp, 60.dp).clip(RoundedCornerShape(8.dp)).background(DarkBackground))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Episode ${episode.number}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(episode.title, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
