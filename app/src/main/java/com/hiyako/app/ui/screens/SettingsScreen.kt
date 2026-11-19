package com.hiyako.app.ui.screens

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hiyako.app.data.db.AppDatabase
import com.hiyako.app.providers.*
import com.hiyako.app.security.KeyVault
import com.hiyako.app.ui.i18n.Lang
import com.hiyako.app.ui.i18n.tr
import com.hiyako.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToWorkspace: () -> Unit,
    onNavigateToUpdater: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onSignIn: () -> Unit,
    onClearChat: () -> Unit,
    vm: AiViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf(AccountStore.load(context)) }
    var addingFor by remember { mutableStateOf<ProviderSpec?>(null) }
    var pickingFor by remember { mutableStateOf<Account?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmWipe by remember { mutableStateOf(false) }
    var wiped by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    
    var showAllProviders by remember { mutableStateOf(false) }
    val auth by vm.auth.collectAsState()

    fun fetchModels(account: Account) {
        scope.launch {
            status = tr("Loading models for ${account.label}...", "جارٍ تحميل نماذج ${account.label}...")
            val result = OpenAiCompatClient.listModels(account.baseUrl, AccountStore.apiKey(account))
            result.onSuccess { ids ->
                ModelCache.save(context, account.id, ids)
                status = tr("${ids.size} models loaded for ${account.label}.", "تم تحميل ${ids.size} نموذجاً لـ ${account.label}.")
            }.onFailure { e ->
                status = tr(
                    "Could not load models for ${account.label}: ${e.message ?: "unknown error"}",
                    "تعذّر تحميل نماذج ${account.label}: ${e.message ?: "خطأ غير معروف"}"
                )
            }
            accounts = AccountStore.load(context)
            vm.reloadAccountModels()
        }
    }

    Scaffold(
        containerColor = OledBlack,
        topBar = {
            TopAppBar(
                title = { Text(tr("Settings", "الإعدادات"), fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = tr("Back", "رجوع"), tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OledBlack)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(title = tr("Appearance", "المظهر"), subtitle = tr("Pick a theme. It applies to the whole app.", "اختر ثيماً. يُطبَّق على التطبيق كله.")) {
                AppPalettes.all.chunked(3).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { palette ->
                            ThemeCard(
                                palette = palette,
                                selected = ThemeState.palette.id == palette.id,
                                onClick = { ThemeState.select(context, palette.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(tr("Language", "اللغة"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LangOption("العربية", selected = Lang.arabic, onClick = { Lang.set(context, true) }, modifier = Modifier.weight(1f))
                    LangOption("English", selected = !Lang.arabic, onClick = { Lang.set(context, false) }, modifier = Modifier.weight(1f))
                }
            }

            SectionCard(
                title = tr("Providers and accounts", "المزودون والحسابات"),
                subtitle = tr(
                    "Add as many accounts as you like. Keys are encrypted with the Android Keystore.",
                    "أضف أي عدد من الحسابات. تُشفَّر المفاتيح بمخزن أندرويد."
                )
            ) {
                status?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = AccentBlue)
                    Spacer(Modifier.height(6.dp))
                }
                
                val displayedProviders = if (showAllProviders) ProviderCatalog.all else ProviderCatalog.all.take(4)
                
                displayedProviders.forEach { spec ->
                    val mine = accounts.filter { it.providerId == spec.id }
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(spec.name, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                if (spec.note.isNotEmpty()) {
                                    Text(spec.note, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }
                            if (spec.id == ProviderCatalog.PUTER) {
                                OutlinedButton(onClick = {
                                    if (auth.signedIn) {
                                        clearPuterSession()
                                        vm.engine.reload()
                                        scope.launch {
                                            delay(1000)
                                            onSignIn()
                                        }
                                    } else {
                                        onSignIn()
                                    }
                                }) { 
                                    Text(if (auth.signedIn) tr("Switch account", "تبديل الحساب") else tr("Sign In", "تسجيل الدخول"), color = AccentBlue) 
                                }
                            } else {
                                IconButton(onClick = { addingFor = spec }) {
                                    Icon(Icons.Default.Add, contentDescription = tr("Add account", "إضافة حساب"), tint = AccentBlue)
                                }
                            }
                        }
                        mine.forEach { account ->
                            val modelCount = ModelCache.models(context, account.id).size
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f).clickable { pickingFor = account }) {
                                    Text(account.label, color = TextPrimary)
                                    Text(
                                        defaultModelOf(context, account).ifBlank { tr("No model yet", "لا يوجد نموذج بعد") } +
                                            tr(" · $modelCount models", " · $modelCount نموذجاً"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { fetchModels(account) }) {
                                    Icon(Icons.Default.Refresh, contentDescription = tr("Load models", "تحميل النماذج"), tint = TextSecondary)
                                }
                                IconButton(onClick = {
                                    AccountStore.remove(context, account.id)
                                    accounts = AccountStore.load(context)
                                    vm.reloadAccountModels()
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = tr("Remove account", "حذف الحساب"), tint = Color.Red)
                                }
                            }
                        }
                    }
                }
                
                if (ProviderCatalog.all.size > 4) {
                    TextButton(
                        onClick = { showAllProviders = !showAllProviders },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(if (showAllProviders) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = AccentBlue)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (showAllProviders) tr("Show less", "عرض أقل") else tr("Show all providers", "عرض كل المزودين"),
                            color = AccentBlue
                        )
                    }
                }
            }

            SectionCard(title = tr("Tools", "الأدوات")) {
                OutlinedButton(onClick = onNavigateToWorkspace, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Workspace (GitHub)", "مساحة العمل (GitHub)"), color = TextPrimary)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onNavigateToMemory, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Memory", "الذاكرة"), color = TextPrimary)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onNavigateToUpdater, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Check for updates", "التحقق من التحديثات"), color = TextPrimary)
                }
            }

            SectionCard(
                title = tr("Privacy and data", "الخصوصية والبيانات"),
                subtitle = tr("Everything is stored on this device. Deleting data cannot be undone.", "كل شيء مخزّن على هذا الجهاز. حذف البيانات لا يمكن التراجع عنه.")
            ) {
                OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Clear all chats", "حذف كل المحادثات"), color = TextPrimary)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { confirmWipe = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f))
                ) {
                    Text(tr("Delete all data on this device", "حذف كل البيانات على هذا الجهاز"), color = Color.Red)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    addingFor?.let { spec ->
        AddAccountDialog(
            spec = spec,
            onDismiss = { addingFor = null },
            onSave = { label, url, model, key ->
                val account = AccountStore.add(context, spec.id, label, url, model, key)
                accounts = AccountStore.load(context)
                addingFor = null
                fetchModels(account)
            }
        )
    }
    pickingFor?.let { account ->
        DefaultModelDialog(
            account = account,
            models = ModelCache.models(context, account.id),
            onPick = { model ->
                setAccountModel(context, account.id, model)
                accounts = AccountStore.load(context)
                vm.reloadAccountModels()
                pickingFor = null
            },
            onDismiss = { pickingFor = null }
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(tr("Clear all chats?", "حذف كل المحادثات؟"), color = TextPrimary) },
            text = { Text(tr("This deletes every conversation on this device. It cannot be undone.", "سيتم حذف كل المحادثات على هذا الجهاز. لا يمكن التراجع."), color = TextSecondary) },
            confirmButton = { Button(onClick = { onClearChat(); confirmClear = false }) { Text(tr("Delete", "حذف")) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(tr("Cancel", "إلغاء"), color = TextSecondary) } }
        )
    }
    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text(tr("Delete all data?", "حذف كل البيانات؟"), color = TextPrimary) },
            text = { Text(tr("This deletes all chats, memories, saved accounts and keys on this device.", "سيتم حذف كل المحادثات والذاكرة والحسابات والمفاتيح على هذا الجهاز."), color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        wipeLocalData(context)
                        accounts = emptyList()
                        vm.reloadAccountModels()
                        wiped = true
                        confirmWipe = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text(tr("Delete everything", "حذف كل شيء")) }
            },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text(tr("Cancel", "إلغاء"), color = TextSecondary) } }
        )
    }
}

@Composable
private fun SectionCard(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun LangOption(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) AccentBlue.copy(alpha = 0.15f) else SecondarySurface)
            .border(1.dp, if (selected) AccentBlue else BorderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) AccentBlue else TextPrimary, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun ThemeCard(palette: AppPalette, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (palette.id == "paper") Color(0xFFFFFFFF) else Color(0xFF000000))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) AccentBlue else palette.border,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { onClick() }
        ) {
            Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(width = 36.dp, height = 10.dp).background(palette.userBubble, RoundedCornerShape(6.dp)))
            Box(Modifier.align(Alignment.BottomStart).padding(8.dp).size(16.dp).background(palette.accent, CircleShape))
            if (selected) Icon(Icons.Default.Check, null, tint = palette.accent, modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).size(16.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(palette.name, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun DefaultModelDialog(account: Account, models: List<String>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = { Text(tr("Default model for ${account.label}", "النموذج الافتراضي لـ ${account.label}"), color = TextPrimary) },
        text = {
            if (models.isEmpty()) {
                Text(tr("No models loaded yet.", "لم تُحمَّل النماذج بعد."), color = TextSecondary)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(models) { id ->
                        Row(Modifier.fillMaxWidth().clickable { onPick(id) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(id, modifier = Modifier.weight(1f), color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (id == account.model) Icon(Icons.Default.Check, null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Close", "إغلاق"), color = AccentBlue) } }
    )
}

private fun clearPuterSession() {
    try {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
    } catch (e: Exception) { Log.w("Settings", "Could not clear Puter session", e) }
}

private fun wipeLocalData(context: Context) {
    Thread {
        try { AppDatabase.getDatabase(context).clearAllTables() } catch (e: Exception) {}
        KeyVault.prefs(context).edit().clear().apply()
        context.getSharedPreferences("nexus_settings", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        java.io.File(context.filesDir, "attachments").deleteRecursively()
        java.io.File(context.cacheDir, "update.apk").delete()
    }.start()
    clearPuterSession()
}

@Composable
private fun AddAccountDialog(spec: ProviderSpec, onDismiss: () -> Unit, onSave: (String, String, String, String) -> Unit) {
    var label by remember { mutableStateOf(spec.name) }
    var baseUrl by remember { mutableStateOf(spec.baseUrl) }
    var model by remember { mutableStateOf(spec.defaultModel) }
    var apiKey by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = { Text(tr("Add ${spec.name}", "إضافة ${spec.name}"), color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text(tr("Account name", "اسم الحساب")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (spec.needsKey) {
                    OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text(tr("API key", "مفتاح API")) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text(tr("Base URL (https://)", "رابط الأساس (https://)")) }, singleLine = true, enabled = spec.editableBaseUrl, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text(tr("Default model (optional)", "النموذج الافتراضي (اختياري)")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val url = baseUrl.trim()
                when {
                    spec.needsKey && apiKey.isBlank() -> error = tr("Enter an API key.", "أدخل مفتاح API.")
                    !url.startsWith("https://") -> error = tr("Base URL must start with https://", "يجب أن يبدأ رابط الأساس بـ https://")
                    else -> onSave(label.trim().ifEmpty { spec.name }, url, model.trim(), apiKey.trim())
                }
            }) { Text(tr("Save", "حفظ"), color = AccentBlue) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "إلغاء"), color = TextSecondary) } }
    )
}
