package com.hiyako.app.ui.screens

import com.hiyako.app.ui.theme.Brand
import kotlinx.coroutines.flow.first
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Brush
import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiyako.app.data.db.AppDatabase
import com.hiyako.app.providers.*
import com.hiyako.app.security.KeyVault
import com.hiyako.app.ui.components.*
import com.hiyako.app.ui.i18n.Lang
import com.hiyako.app.ui.i18n.tr
import com.hiyako.app.ui.theme.*
import kotlinx.coroutines.withTimeoutOrNull
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
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var deletingAccount by remember { mutableStateOf<Account?>(null) }
    val addableProviders = remember { ProviderCatalog.all.filter { it.id != ProviderCatalog.PUTER } }
    val versionLabel = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() } catch (e: Exception) { "" }
    }

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
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            LargeTopAppBar(
                title = { Text(tr("Settings", "الإعدادات")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Back", "رجوع")) } },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent, scrolledContainerColor = DeepSurface.copy(alpha = 0.92f),
                    titleContentColor = TextPrimary, navigationIconContentColor = TextPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1) Account Card
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(AccentBlue.copy(alpha = 0.50f), AccentTwo.copy(alpha = 0.28f))))
                    .border(1.dp, GlassBorderBrush, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(54.dp).background(AccentBrush, CircleShape), contentAlignment = Alignment.Center) {
                    Text(auth.username.firstOrNull()?.uppercase() ?: "✦", color = OnAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (auth.signedIn && auth.username.isNotEmpty()) auth.username else tr("Not signed in", "غير مسجّل الدخول"), color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (auth.signedIn) "Puter" else tr("Sign in to start chatting", "سجّل الدخول لتبدأ"), color = TextSecondary, fontSize = 13.sp)
                }
                HiyakoButton(
                    text = if (auth.signedIn) tr("Switch", "تبديل") else tr("Sign in", "دخول"),
                    onClick = {
                        if (auth.signedIn) {
                            clearPuterSession()
                            vm.engine.reload()
                            scope.launch { withTimeoutOrNull(15_000L) { vm.engine.ready.first { it } }; onSignIn() }
                        } else {
                            onSignIn()
                        }
                    },
                    kind = ButtonKind.Primary
                )
            }

            // 2) Appearance
            SettingsGroup(tr("Appearance", "المظهر")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(AppPalettes.all, key = { it.id }) { p ->
                            Column(Modifier.width(100.dp)) {
                                Column(
                                    Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(16.dp)).background(p.base)
                                        .border(if (ThemeState.palette.id == p.id) 2.dp else 1.dp, if (ThemeState.palette.id == p.id) p.accent else p.border, RoundedCornerShape(16.dp))
                                        .clickable { ThemeState.select(context, p.id) }.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(Modifier.align(Alignment.End).size(50.dp, 12.dp).background(p.userBubble, RoundedCornerShape(6.dp)))
                                    Box(Modifier.size(60.dp, 8.dp).background(p.textSecondary.copy(alpha = 0.5f), RoundedCornerShape(4.dp)))
                                    Spacer(Modifier.weight(1f))
                                    Box(Modifier.size(14.dp).background(p.accent, CircleShape))
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(p.name, color = TextPrimary, fontSize = 12.sp, fontWeight = if (ThemeState.palette.id == p.id) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LangChip("العربية", Lang.arabic, Modifier.weight(1f)) { Lang.set(context, true) }
                        LangChip("English", !Lang.arabic, Modifier.weight(1f)) { Lang.set(context, false) }
                    }
                }
            }

            // 3) Providers
            SettingsGroup(tr("Providers and accounts", "المزودون والحسابات")) {
                if (accounts.isEmpty()) {
                    Text(tr("No accounts yet. Add one to unlock more models.", "لا توجد حسابات بعد. أضف حساباً لفتح نماذج أكثر."), color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(16.dp))
                }
                accounts.forEachIndexed { i, a ->
                    SettingsRow(
                        icon = Icons.Outlined.Key, title = a.label,
                        subtitle = (defaultModelOf(context, a).ifBlank { tr("No model yet", "لا يوجد نموذج بعد") }),
                        showDivider = i < accounts.lastIndex,
                        onClick = { pickingFor = a }
                    )
                }
                
                val displayedProviders = if (showAllProviders) addableProviders else addableProviders.take(3)
                displayedProviders.forEachIndexed { i, spec ->
                    SettingsRow(
                        icon = Icons.Outlined.Dns,
                        title = spec.name,
                        subtitle = spec.note.takeIf { it.isNotBlank() },
                        showDivider = i < displayedProviders.lastIndex,
                        onClick = { addingFor = spec },
                        trailing = {
                            Icon(Icons.Default.Add, null, tint = AccentBlue)
                        }
                    )
                }
                
                if (addableProviders.size > 3) {
                    TextButton(
                        onClick = { showAllProviders = !showAllProviders },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Icon(if (showAllProviders) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = AccentBlue)
                        Spacer(Modifier.width(4.dp))
                        Text(if (showAllProviders) tr("Show less", "عرض أقل") else tr("Show all providers", "عرض كل المزودين"), color = AccentBlue)
                    }
                }
            }

            // 4) Tools
            SettingsGroup(tr("Tools", "الأدوات")) {
                SettingsRow(Icons.Outlined.Folder, tr("Workspace (GitHub)", "مساحة العمل (GitHub)"), onClick = onNavigateToWorkspace)
                SettingsRow(Icons.Outlined.Memory, tr("Memory", "الذاكرة"), onClick = onNavigateToMemory)
                SettingsRow(Icons.Outlined.SystemUpdate, tr("Check for updates", "التحقق من التحديثات"), showDivider = false, onClick = onNavigateToUpdater)
            }

            // 5) Privacy
            SettingsGroup(tr("Privacy and data", "الخصوصية والبيانات")) {
                SettingsRow(Icons.Outlined.DeleteSweep, tr("Clear all chats", "حذف كل المحادثات"), onClick = { confirmClear = true })
                SettingsRow(Icons.Outlined.DeleteForever, tr("Delete all data on this device", "حذف كل البيانات على هذا الجهاز"), danger = true, showDivider = false, onClick = { confirmWipe = true })
            }

            Text("${tr(Brand.EN, Brand.AR)} $versionLabel", color = TextMuted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
        }
    }

    // Dialogs...
    if (confirmClear) ConfirmDialog(
        title = tr("Clear all chats?", "حذف كل المحادثات؟"), message = tr("This deletes every conversation on this device. It cannot be undone.", "سيتم حذف كل المحادثات على هذا الجهاز. لا يمكن التراجع."),
        confirmLabel = tr("Delete", "حذف"), danger = true,
        onConfirm = { onClearChat(); confirmClear = false }, onDismiss = { confirmClear = false }
    )

    if (confirmWipe) ConfirmDialog(
        title = tr("Delete all data?", "حذف كل البيانات؟"), message = tr("This deletes all chats, memories, saved accounts and keys on this device.", "سيتم حذف كل المحادثات والذاكرة والحسابات والمفاتيح على هذا الجهاز."),
        confirmLabel = tr("Delete everything", "حذف كل شيء"), danger = true,
        onConfirm = { wipeLocalData(context); accounts = emptyList(); vm.reloadAccountModels(); wiped = true; confirmWipe = false }, onDismiss = { confirmWipe = false }
    )

    addingFor?.let { spec ->
        AddAccountDialog(spec = spec, onDismiss = { addingFor = null }, onSave = { label, url, model, key ->
            val account = AccountStore.add(context, spec.id, label, url, model, key)
            accounts = AccountStore.load(context)
            addingFor = null
            fetchModels(account)
        })
    }
    pickingFor?.let { account ->
        DefaultModelDialog(account = account, models = ModelCache.models(context, account.id), onPick = { model ->
            setAccountModel(context, account.id, model)
            accounts = AccountStore.load(context)
            vm.reloadAccountModels()
            pickingFor = null
        }, onRefresh = {
            pickingFor = null
            fetchModels(account)
        }, onDelete = {
            pickingFor = null
            deletingAccount = account
        }, onDismiss = { pickingFor = null })
    }
    deletingAccount?.let { account ->
        ConfirmDialog(
            title = tr("Delete ${account.label}?", "حذف ${account.label}؟"),
            message = tr("The saved key and model list for this account will be removed from this device.", "سيتم حذف المفتاح المحفوظ وقائمة النماذج لهذا الحساب من هذا الجهاز."),
            confirmLabel = tr("Delete", "حذف"), danger = true,
            onConfirm = {
                AccountStore.remove(context, account.id)
                ModelCache.remove(context, account.id)
                if (vm.selectedModel.value.accountId == account.id) vm.selectModel(AiModel("Default (Auto)", ""))
                accounts = AccountStore.load(context)
                vm.reloadAccountModels()
                deletingAccount = null
            },
            onDismiss = { deletingAccount = null }
        )
    }
}

@Composable
private fun LangChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp))
            .background(if (selected) AccentBlue.copy(alpha = 0.18f) else ElevatedSurface)
            .border(1.dp, if (selected) AccentBlue else BorderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = if (selected) AccentBlue else TextSecondary, fontWeight = FontWeight.SemiBold) }
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
private fun DefaultModelDialog(account: Account, models: List<String>, onPick: (String) -> Unit, onRefresh: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
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
        confirmButton = {
            Row {
                TextButton(onClick = onRefresh) { Text(tr("Refresh models", "تحديث النماذج"), color = AccentBlue) }
                TextButton(onClick = onDismiss) { Text(tr("Close", "إغلاق"), color = AccentBlue) }
            }
        },
        dismissButton = { TextButton(onClick = onDelete) { Text(tr("Delete account", "حذف الحساب"), color = DangerColor) } }
    )
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
                error?.let { Text(it, color = DangerColor, style = MaterialTheme.typography.bodySmall) }
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
