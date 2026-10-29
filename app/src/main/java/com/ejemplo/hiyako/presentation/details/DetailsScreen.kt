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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B131D))) {
        AsyncImage(model = bannerUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(350.dp).alpha(0.3f))
        Box(modifier = Modifier.fillMaxWidth().height(350.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF0B131D)))))

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.height(100.dp))
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (score != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                Text(" $score", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (studios.isNotEmpty()) {
                            Text(studios.joinToString(", "), color = Color(0xFFB388FF), fontSize = 14.sp)
                        }
                    }

                    if (genres.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            genres.take(3).forEach { genre ->
                                Surface(
                                    color = Color(0xFF1E1B26),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(text = genre, color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }

                    if (!synopsis.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Synopsis", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = synopsis!!, color = Color.Gray, fontSize = 14.sp)
                    }

                    if (charactersList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Characters", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(charactersList) { char ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(80.dp)
                                ) {
                                    AsyncImage(
                                        model = char.imageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(70.dp).clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = char.name,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Episodes", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (episodesList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFB388FF))
                    }
                }
            } else {
                items(episodesList) { ep ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B26)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (ep.thumbnailUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ep.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.width(80.dp).height(50.dp).clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column {
                                Text(text = "Episode ${ep.number}", color = Color(0xFFB388FF), fontSize = 12.sp)
                                Text(text = ep.title, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
