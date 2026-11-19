package com.hiyako.app.ui.screens

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hiyako.app.data.db.AppDatabase
import com.hiyako.app.providers.Account
import com.hiyako.app.providers.AccountStore
import com.hiyako.app.providers.ModelCache
import com.hiyako.app.providers.OpenAiCompatClient
import com.hiyako.app.providers.ProviderCatalog
import com.hiyako.app.providers.ProviderSpec
import com.hiyako.app.providers.defaultModelOf
import com.hiyako.app.providers.setAccountModel
import com.hiyako.app.security.KeyVault
import com.hiyako.app.ui.i18n.Lang
import com.hiyako.app.ui.i18n.tr
import com.hiyako.app.ui.theme.AppPalette
import com.hiyako.app.ui.theme.AppPalettes
import com.hiyako.app.ui.theme.ThemeState
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
        topBar = {
            TopAppBar(
                title = { Text(tr("Settings", "الإعدادات"), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = tr("Back", "رجوع"))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---------------------------------------------------------- Appearance and language
            SectionCard(
                title = tr("Appearance", "المظهر"),
                subtitle = tr("Pick a theme. It applies to the whole app.", "اختر ثيماً. يُطبَّق على التطبيق كله.")
            ) {
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
                Text(tr("Language", "اللغة"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LangOption("العربية", selected = Lang.arabic, onClick = { Lang.set(context, true) }, modifier = Modifier.weight(1f))
                    LangOption("English", selected = !Lang.arabic, onClick = { Lang.set(context, false) }, modifier = Modifier.weight(1f))
                }
            }

            // ---------------------------------------------------------- Providers
            SectionCard(
                title = tr("Providers and accounts", "المزودون والحسابات"),
                subtitle = tr(
                    "Add as many accounts as you like. Keys are encrypted with the Android Keystore. Every model of every account appears in the model picker.",
                    "أضف أي عدد من الحسابات. تُشفَّر المفاتيح بمخزن أندرويد. تظهر كل نماذج كل الحسابات في منتقي النماذج."
                )
            ) {
                status?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                }
                ProviderCatalog.all.forEach { spec ->
                    val mine = accounts.filter { it.providerId == spec.id }
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(spec.name, fontWeight = FontWeight.SemiBold)
                                if (spec.note.isNotEmpty()) {
                                    Text(spec.note, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (spec.id == ProviderCatalog.PUTER) {
                                OutlinedButton(onClick = {
                                    clearPuterSession()
                                    onSignIn()
                                }) { Text(tr("Switch account", "تبديل الحساب")) }
                            } else {
                                IconButton(onClick = { addingFor = spec }) {
                                    Icon(Icons.Default.Add, contentDescription = tr("Add account", "إضافة حساب"))
                                }
                            }
                        }
                        mine.forEach { account ->
                            val modelCount = ModelCache.models(context, account.id).size
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f).clickable { pickingFor = account }) {
                                    Text(account.label)
                                    Text(
                                        defaultModelOf(context, account).ifBlank { tr("No model yet", "لا يوجد نموذج بعد") } +
                                            tr(" · $modelCount models", " · $modelCount نموذجاً"),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { fetchModels(account) }) {
                                    Icon(Icons.Default.Refresh, contentDescription = tr("Load models", "تحميل النماذج"))
                                }
                                IconButton(onClick = {
                                    AccountStore.remove(context, account.id)
                                    accounts = AccountStore.load(context)
                                    vm.reloadAccountModels()
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = tr("Remove account", "حذف الحساب"))
                                }
                            }
                        }
                    }
                }
            }

            // ---------------------------------------------------------- Tools
            SectionCard(title = tr("Tools", "الأدوات")) {
                OutlinedButton(onClick = onNavigateToWorkspace, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Workspace (GitHub)", "مساحة العمل (GitHub)"))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onNavigateToMemory, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Memory", "الذاكرة"))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onNavigateToUpdater, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Check for updates", "التحقق من التحديثات"))
                }
            }

            // ---------------------------------------------------------- Privacy and data
            SectionCard(
                title = tr("Privacy and data", "الخصوصية والبيانات"),
                subtitle = tr(
                    "Everything is stored on this device. Deleting data cannot be undone.",
                    "كل شيء مخزّن على هذا الجهاز. حذف البيانات لا يمكن التراجع عنه."
                )
            ) {
                OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Clear all chats", "حذف كل المحادثات"))
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { confirmWipe = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(tr("Delete all data on this device", "حذف كل البيانات على هذا الجهاز"))
                }
                if (wiped) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tr(
                            "All local data was deleted. Close and reopen the app to finish.",
                            "تم حذف كل البيانات المحلية. أغلق التطبيق وافتحه من جديد لإكمال الحذف."
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
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
            title = { Text(tr("Clear all chats?", "حذف كل المحادثات؟")) },
            text = {
                Text(tr("This deletes every conversation on this device. It cannot be undone.", "سيتم حذف كل المحادثات على هذا الجهاز. لا يمكن التراجع."))
            },
            confirmButton = {
                Button(onClick = {
                    onClearChat()
                    confirmClear = false
                }) { Text(tr("Delete", "حذف")) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(tr("Cancel", "إلغاء")) } }
        )
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text(tr("Delete all data?", "حذف كل البيانات؟")) },
            text = {
                Text(
                    tr(
                        "This deletes all chats, memories, saved accounts and keys on this device. It cannot be undone.",
                        "سيتم حذف كل المحادثات والذاكرة والحسابات والمفاتيح على هذا الجهاز. لا يمكن التراجع."
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        wipeLocalData(context)
                        accounts = emptyList()
                        vm.reloadAccountModels()
                        wiped = true
                        confirmWipe = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(tr("Delete everything", "حذف كل شيء")) }
            },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text(tr("Cancel", "إلغاء")) } }
        )
    }
}

@Composable
private fun SectionCard(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
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
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.background
            )
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
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
                .background(palette.background)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else palette.border,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { onClick() }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(width = 36.dp, height = 10.dp)
                    .background(palette.userBubble, RoundedCornerShape(6.dp))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .size(16.dp)
                    .background(palette.accent, CircleShape)
            )
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = tr("Selected", "محدد"),
                    tint = palette.accent,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).size(16.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            palette.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun DefaultModelDialog(
    account: Account,
    models: List<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Default model for ${account.label}", "النموذج الافتراضي لـ ${account.label}")) },
        text = {
            if (models.isEmpty()) {
                Text(tr("No models loaded yet. Tap the refresh button on this account first.", "لم تُحمَّل النماذج بعد. اضغط زر التحديث لهذا الحساب أولاً."))
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(models) { id ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(id) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(id, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (id == account.model) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = tr("Current default", "الافتراضي الحالي"),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Close", "إغلاق")) } }
    )
}

/** Clears the Puter web session so the next sign-in starts with a different account. */
private fun clearPuterSession() {
    try {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
    } catch (e: Exception) {
        Log.w("Settings", "Could not clear the Puter session", e)
    }
}

private fun wipeLocalData(context: Context) {
    Thread {
        try {
            AppDatabase.getDatabase(context).clearAllTables()
        } catch (e: Exception) {
            Log.w("Settings", "Could not clear the chat database", e)
        }
        KeyVault.prefs(context).edit().clear().apply()
        context.getSharedPreferences("nexus_settings", Context.MODE_PRIVATE).edit().clear().apply()
    }.start()
    clearPuterSession()
}

@Composable
private fun AddAccountDialog(
    spec: ProviderSpec,
    onDismiss: () -> Unit,
    onSave: (label: String, baseUrl: String, model: String, apiKey: String) -> Unit
) {
    var label by remember { mutableStateOf(spec.name) }
    var baseUrl by remember { mutableStateOf(spec.baseUrl) }
    var model by remember { mutableStateOf(spec.defaultModel) }
    var apiKey by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Add ${spec.name}", "إضافة ${spec.name}")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(tr("Account name", "اسم الحساب")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (spec.needsKey) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text(tr("API key", "مفتاح API")) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text(tr("Base URL (https://)", "رابط الأساس (https://)")) },
                    singleLine = true,
                    enabled = spec.editableBaseUrl,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text(tr("Default model (optional)", "النموذج الافتراضي (اختياري)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    tr("All models are loaded from the provider after you save.", "تُحمَّل كل النماذج من المزود بعد الحفظ."),
                    style = MaterialTheme.typography.bodySmall
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
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
            }) { Text(tr("Save", "حفظ")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "إلغاء")) } }
    )
}
