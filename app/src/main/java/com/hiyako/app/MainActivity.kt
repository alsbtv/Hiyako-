package com.hiyako.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hiyako.app.ui.screens.*
import com.hiyako.app.ui.theme.AppTheme
import com.hiyako.app.ui.theme.OledBlack
import com.hiyako.app.ui.theme.TextPrimary
import com.hiyako.app.ui.theme.AccentBlue
import com.hiyako.app.ui.i18n.Lang
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val aiViewModel: AiViewModel = viewModel()
            val darkTheme = when (aiViewModel.themeMode) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }
            val density = LocalDensity.current

            CompositionLocalProvider(LocalDensity provides Density(density.density, aiViewModel.fontSize)) {
                AppTheme(darkTheme = darkTheme) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        var showApp by remember { mutableStateOf(false) }
                        var showSplash by remember { mutableStateOf(true) }

                        LaunchedEffect(Unit) {
                            delay(1800) // Splash duration
                            showSplash = false
                            delay(300) // Crossfade duration
                            showApp = true
                        }

                        Box(Modifier.fillMaxSize()) {
                            if (showApp) {
                                AppRoot(aiViewModel)
                            }
                            if (showSplash) {
                                AnimatedSplashScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedSplashScreen() {
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        alpha.animateTo(1f, animationSpec = tween(600))
    }

    Box(Modifier.fillMaxSize().background(OledBlack), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(120.dp).scale(scale.value).alpha(alpha.value),
                contentAlignment = Alignment.Center
            ) {
                // Glow
                Box(Modifier.size(160.dp).background(Brush.radialGradient(listOf(AccentBlue.copy(alpha = 0.3f), Color.Transparent))))
                // Logo Circle
                Box(Modifier.size(100.dp).background(AccentBlue, CircleShape), contentAlignment = Alignment.Center) {
                    Text("H", color = Color.White, fontSize = 50.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Hiyako",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(alpha.value)
            )
        }
    }
}

@Composable
private fun AppRoot(vm: AiViewModel) {
    var screenState by remember { mutableStateOf("chat") }
    var showEngineHost by remember { mutableStateOf(false) }
    val isSigningIn by vm.isSigningIn.collectAsState()

    LaunchedEffect(Unit) {
        androidx.compose.runtime.withFrameNanos { }
        showEngineHost = true
    }

    BackHandler(enabled = screenState != "chat") {
        screenState = if (screenState == "settings") "chat" else "settings"
    }

    val dir = if (Lang.arabic) -1 else 1
    val order = listOf("chat", "settings", "workspace", "memory", "updater")

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = screenState,
            transitionSpec = {
                val deeper = order.indexOf(targetState) > order.indexOf(initialState)
                val s = if (deeper) dir else -dir
                (slideInHorizontally(tween(300)) { it / 6 * s } + fadeIn(tween(250))) togetherWith
                (slideOutHorizontally(tween(300)) { -it / 6 * s } + fadeOut(tween(150)))
            },
            label = "screens"
        ) { screen ->
            when (screen) {
                "workspace" -> WorkspaceScreen(
                    onBack = { screenState = "settings" },
                    onRepoSelected = { screenState = "chat" },
                    onNavigateToUpdater = { screenState = "updater" }
                )
                "updater" -> UpdaterScreen(onBack = { screenState = "settings" })
                "memory" -> MemoryScreen(onBack = { screenState = "settings" }, vm = vm)
                "settings" -> SettingsScreen(
                    onBack = { screenState = "chat" },
                    onNavigateToWorkspace = { screenState = "workspace" },
                    onNavigateToUpdater = { screenState = "updater" },
                    onNavigateToMemory = { screenState = "memory" },
                    onSignIn = { vm.signIn() },
                    onClearChat = { vm.clearAllChats() },
                    vm = vm
                )
                else -> ChatScreen(onNavigateToSettings = { screenState = "settings" }, vm = vm)
            }
        }

        if (showEngineHost) {
            EngineHost(vm)
        }

        // Global Loading Overlay for Puter Sign In
        if (isSigningIn) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentBlue)
                        Spacer(Modifier.height(16.dp))
                        Text("Connecting to Puter...", color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun EngineHost(vm: AiViewModel) {
    val engine = vm.engine
    val popup by engine.popup.collectAsState()

    AndroidView(factory = { engine.webView }, modifier = Modifier.size(1.dp).alpha(0f))

    BackHandler(enabled = popup != null) { engine.closePopup() }

    val popupView = popup
    if (popupView != null) {
        Column(Modifier.fillMaxSize().background(OledBlack)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { engine.closePopup() }) {
                    Icon(Icons.Default.Close, "Close", tint = TextPrimary)
                }
                Text("Puter sign-in", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            AndroidView(factory = { popupView }, modifier = Modifier.weight(1f).fillMaxWidth())
        }
    }
}
