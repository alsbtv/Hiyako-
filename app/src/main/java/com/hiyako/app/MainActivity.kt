package com.hiyako.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hiyako.app.ui.i18n.Lang
import com.hiyako.app.ui.screens.*
import com.hiyako.app.ui.theme.AppTheme
import com.hiyako.app.ui.theme.AuroraBackground
import com.hiyako.app.ui.theme.OledBlack
import com.hiyako.app.ui.theme.TextPrimary
import com.hiyako.app.ui.theme.ThemeState

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

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
                    val view = LocalView.current
                    val lightBars = !ThemeState.palette.dark
                    SideEffect {
                        val window = (view.context as Activity).window
                        val controller = WindowCompat.getInsetsController(window, view)
                        controller.isAppearanceLightStatusBars = lightBars
                        controller.isAppearanceLightNavigationBars = lightBars
                    }
                    AuroraBackground {
                        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                            AppRoot(aiViewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRoot(vm: AiViewModel) {
    var screenState by remember { mutableStateOf("chat") }
    var showEngineHost by remember { mutableStateOf(false) }
    var splashDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        androidx.compose.runtime.withFrameNanos { }
        showEngineHost = true
    }

    BackHandler(enabled = splashDone && screenState != "chat") {
        screenState = if (screenState == "settings") "chat" else "settings"
    }

    val dir = if (Lang.arabic) -1 else 1
    val order = listOf("chat", "settings", "workspace", "memory", "updater", "puter")

    Box(Modifier.fillMaxSize()) {
        if (!splashDone) {
            SplashScreen(onFinished = { splashDone = true })
        } else {
            AnimatedContent(
                targetState = screenState,
                transitionSpec = {
                    val deeper = order.indexOf(targetState) > order.indexOf(initialState)
                    val s = if (deeper) dir else -dir
                    (fadeIn(tween(340)) + scaleIn(tween(340), initialScale = 0.94f) + slideInHorizontally(tween(340)) { it / 8 * s }) togetherWith
                        (fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 1.04f))
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
                    "puter" -> PuterLoginScreen(onBack = { screenState = "settings" }, vm = vm)
                    "settings" -> SettingsScreen(
                        onBack = { screenState = "chat" },
                        onNavigateToWorkspace = { screenState = "workspace" },
                        onNavigateToUpdater = { screenState = "updater" },
                        onNavigateToMemory = { screenState = "memory" },
                        onSignIn = { screenState = "puter" },
                        onClearChat = { vm.clearAllChats() },
                        vm = vm
                    )
                    else -> ChatScreen(onNavigateToSettings = { screenState = "settings" }, vm = vm)
                }
            }
        }

        if (showEngineHost) {
            EngineHost(vm)
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
        Column(Modifier.fillMaxSize().background(OledBlack).statusBarsPadding().navigationBarsPadding()) {
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
