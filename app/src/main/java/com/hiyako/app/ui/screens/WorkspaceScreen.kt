package com.hiyako.app.ui.screens

import android.content.Context
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hiyako.app.ui.components.*
import com.hiyako.app.ui.i18n.tr
import com.hiyako.app.ui.theme.*

@Composable
fun WorkspaceScreen(onBack: () -> Unit, onRepoSelected: () -> Unit, onNavigateToUpdater: () -> Unit, vm: WorkspaceViewModel = viewModel()) {
    val context = LocalContext.current
    val token by vm.token.collectAsState()
    val repos by vm.repos.collectAsState()
    val loading by vm.isLoading.collectAsState()
    val progress by vm.progress.collectAsState()
    val msg by vm.loadingMessage.collectAsState()
    val error by vm.error.collectAsState()
    var q by remember { mutableStateOf("") }
    var tokenInput by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    val settingsPrefs = remember { context.getSharedPreferences("nexus_settings", Context.MODE_PRIVATE) }
    var useContext by remember { mutableStateOf(settingsPrefs.getBoolean("use_workspace_context", false)) }
    LaunchedEffect(Unit) { vm.loadToken(context) }

    ScreenScaffold(
        title = tr("Workspace (GitHub)", "مساحة العمل (GitHub)"), onBack = onBack,
        actions = {
            if (token.isNotBlank()) {
                IconButton(onClick = { vm.fetchRepos() }) { Icon(Icons.Default.Refresh, tr("Refresh", "تحديث"), tint = TextPrimary) }
                IconButton(onClick = { vm.disconnect(context) }) { Icon(Icons.Default.LinkOff, tr("Disconnect", "فصل"), tint = TextPrimary) }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                token.isBlank() -> Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    EmptyState(Icons.Outlined.Folder, tr("Connect GitHub", "اربط GitHub"), tr("Create a fine-grained token with read-only access to Contents, then paste it here.", "أنشئ توكن fine-grained بصلاحية قراءة المحتوى فقط ثم الصقه هنا."))
                    OutlinedTextField(
                        value = tokenInput, onValueChange = { tokenInput = it }, singleLine = true,
                        placeholder = { Text("github_pat_…", color = TextMuted) },
                        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = { IconButton(onClick = { show = !show }) { Icon(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, tint = TextSecondary) } },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                    )
                    HiyakoButton(tr("Connect", "اتصال"), { vm.saveToken(context, tokenInput.trim()) }, Modifier.fillMaxWidth(), enabled = tokenInput.isNotBlank())
                }
                loading && repos.isEmpty() -> Column(Modifier.padding(16.dp)) { repeat(6) { Box(Modifier.fillMaxWidth().height(60.dp).padding(vertical = 8.dp).background(SecondarySurface, RoundedCornerShape(16.dp))) } }
                else -> Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(tr("Send project files to the AI", "إرسال ملفات المشروع للذكاء الاصطناعي"), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(tr("Adds the loaded repository to every message. Uses more credit and shares your code with the chosen provider.", "يضيف المستودع المحمّل لكل رسالة. يستهلك رصيداً أكثر ويشارك كودك مع المزوّد المختار."), color = TextMuted, fontSize = 11.sp)
                        }
                        Switch(checked = useContext, onCheckedChange = { useContext = it; settingsPrefs.edit().putBoolean("use_workspace_context", it).apply() })
                    }
                    TextField(value = q, onValueChange = { q = it }, placeholder = { Text(tr("Search repositories", "ابحث في المستودعات")) }, modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(14.dp), colors = TextFieldDefaults.colors(unfocusedContainerColor = SecondarySurface, focusedContainerColor = SecondarySurface, unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = AccentBlue))
                    val shown = repos.filter { q.isBlank() || it.fullName.contains(q, true) }
                    if (error != null) Text(error!!, color = DangerColor, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    if (shown.isEmpty() && !loading) EmptyState(Icons.Outlined.SearchOff, tr("No repositories", "لا توجد مستودعات"), tr("Try another search.", "جرّب بحثاً آخر."))
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(shown, key = { it.fullName }) { repo ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SecondarySurface).clickable { vm.selectRepoAndLoadFiles(repo) { useContext = true; settingsPrefs.edit().putBoolean("use_workspace_context", true).apply(); onRepoSelected() } }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Folder, null, tint = AccentBlue)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(repo.name, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    Text(repo.fullName, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextMuted)
                            }
                        }
                    }
                }
            }
            if (loading && repos.isNotEmpty()) {
                Box(Modifier.fillMaxSize().background(OledBlack.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = AccentBlue, trackColor = BorderColor)
                        Spacer(Modifier.height(12.dp))
                        Text(msg, color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
