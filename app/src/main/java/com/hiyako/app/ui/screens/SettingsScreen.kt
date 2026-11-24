package com.hiyako.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

private const val ISSUES_URL = "https://github.com/alsbtv/Hiyako-/issues"
private const val REPO_URL = "https://github.com/alsbtv/Hiyako-"

private enum class Page { Hub, Account, Providers, Data, Language, Appearance, FontSize, Personalization, Audio, About, Terms, Help }

private fun parentOf(page: Page): Page = if (page == Page.Terms) Page.About else Page.Hub

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        Log.w("Settings", "No app can open $url", e)
    }
}

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
    val sp = remember { context.getSharedPreferences("nexus_settings", Context.MODE_PRIVATE) }
    var page by rememberSaveable { mutableStateOf(Page.Hub) }
    var accounts by remember { mutableStateOf(AccountStore.load(context)) }
    var addingFor by remember { mutableStateOf<ProviderSpec?>(null) }
    var pickingFor by remember { mutableStateOf<Account?>(null) }
    var deletingAccount by remember { mutableStateOf<Account?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmWipe by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var wiped by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val auth by vm.auth.collectAsState()
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

    fun signOut() {
        clearPuterSession()
        vm.engine.reload()
        if (vm.selectedModel.value.accountId.isEmpty()) {
            vm.availableModels.value.firstOrNull { it.accountId.isNotEmpty() }?.let { vm.selectModel(it) }
        }
    }

    BackHandler(enabled = page != Page.Hub) { page = parentOf(page) }

    val title = when (page) {
        Page.Hub -> tr("Settings", "الإعدادات")
        Page.Account -> tr("Account settings", "إعدادات الحساب")
        Page.Providers -> tr("Providers and models", "المزودون والنماذج")
        Page.Data -> tr("Data controls", "التحكم في البيانات")
        Page.Language -> tr("Language", "اللغة")
        Page.Appearance -> tr("Appearance", "المظهر")
        Page.FontSize -> tr("Font size", "حجم الخط")
        Page.Personalization -> tr("Personalization", "التخصيص")
        Page.Audio -> tr("Audio and voice", "الصوت")
        Page.About -> tr("About", "حول التطبيق")
        Page.Terms -> tr("Service agreement", "اتفاقية الخدمة")
        Page.Help -> tr("Help and feedback", "المساعدة والملاحظات")
    }

    ScreenScaffold(title = title, onBack = { if (page == Page.Hub) onBack() else page = parentOf(page) }) { inner ->
        Crossfade(targetState = page, label = "settingsPage") { current ->
            Column(
                Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                when (current) {
                    Page.Hub -> {
                        ProfileCard(auth.signedIn, auth.username) { page = Page.Account }
                        SettingsGroup(tr("Account", "الحساب")) {
                            SettingsRow(Icons.Outlined.ManageAccounts, tr("Account settings", "إعدادات الحساب"), onClick = { page = Page.Account })
                            SettingsRow(Icons.Outlined.Storage, tr("Data controls", "التحكم في البيانات"), showDivider = false, onClick = { page = Page.Data })
                        }
                        SettingsGroup(tr("App", "التطبيق")) {
                            SettingsRow(Icons.Outlined.Translate, tr("Language", "اللغة"), if (Lang.arabic) "العربية" else "English", onClick = { page = Page.Language })
                            SettingsRow(Icons.Outlined.Palette, tr("Appearance", "المظهر"), ThemeState.palette.name, onClick = { page = Page.Appearance })
                            SettingsRow(Icons.Outlined.TextFields, tr("Font size", "حجم الخط"), "${(vm.fontSize * 100).roundToInt()}%", onClick = { page = Page.FontSize })
                            SettingsRow(Icons.Outlined.Tune, tr("Personalization", "التخصيص"), toneLabel(sp.getString("tone", "Balanced") ?: "Balanced"), showDivider = false, onClick = { page = Page.Personalization })
                        }
                        SettingsGroup(tr("AI", "الذكاء الاصطناعي")) {
                            SettingsRow(Icons.Outlined.Dns, tr("Providers and models", "المزودون والنماذج"), tr("${accounts.size} saved accounts", "${accounts.size} حسابات محفوظة"), onClick = { page = Page.Providers })
                            SettingsRow(Icons.Outlined.Memory, tr("Memory", "الذاكرة"), onClick = onNavigateToMemory)
                            SettingsRow(Icons.Outlined.Folder, tr("Workspace (GitHub)", "مساحة العمل (GitHub)"), showDivider = false, onClick = onNavigateToWorkspace)
                        }
                        SettingsGroup(tr("Audio", "الصوت")) {
                            SettingsRow(Icons.Outlined.RecordVoiceOver, tr("Audio and voice", "الصوت والنطق"), tr("Voice input, reading speed", "الإدخال الصوتي وسرعة القراءة"), showDivider = false, onClick = { page = Page.Audio })
                        }
                        SettingsGroup(tr("About", "حول")) {
                            SettingsRow(Icons.Outlined.SystemUpdate, tr("Check for updates", "التحقق من التحديثات"), onClick = onNavigateToUpdater)
                            SettingsRow(Icons.Outlined.Info, tr("About ${Brand.EN}", "حول ${Brand.AR}"), versionLabel.ifBlank { null }, onClick = { page = Page.About })
                            SettingsRow(Icons.Outlined.Description, tr("Service agreement", "اتفاقية الخدمة"), showDivider = false, onClick = { page = Page.Terms })
                        }
                        SettingsGroup {
                            SettingsRow(Icons.Outlined.Help, tr("Help and feedback", "المساعدة والملاحظات"), showDivider = false, onClick = { page = Page.Help })
                        }
                        if (auth.signedIn) {
                            HiyakoButton(tr("Log out", "تسجيل الخروج"), onClick = { confirmLogout = true }, kind = ButtonKind.Tonal, modifier = Modifier.fillMaxWidth(), icon = Icons.Outlined.PowerSettingsNew)
                        }
                        Text("${tr(Brand.EN, Brand.AR)} $versionLabel", color = TextMuted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(8.dp), textAlign = TextAlign.Center)
                    }

                    Page.Account -> {
                        ProfileCard(auth.signedIn, auth.username, null)
                        SettingsGroup(tr("Puter", "Puter")) {
                            if (auth.signedIn) {
                                SettingsRow(Icons.Outlined.SwitchAccount, tr("Switch account", "تبديل الحساب"), tr("Sign in with another Puter account", "سجّل الدخول بحساب Puter آخر"), onClick = {
                                    clearPuterSession()
                                    vm.engine.reload()
                                    scope.launch { withTimeoutOrNull(15_000L) { vm.engine.ready.first { it } }; onSignIn() }
                                })
                                SettingsRow(Icons.Outlined.PowerSettingsNew, tr("Log out", "تسجيل الخروج"), danger = true, showDivider = false, onClick = { confirmLogout = true })
                            } else {
                                SettingsRow(Icons.Outlined.Login, tr("Sign in to Puter", "تسجيل الدخول إلى Puter"), tr("Gives access to Puter models", "يفتح لك نماذج Puter"), showDivider = false, onClick = onSignIn)
                            }
                        }
                        InfoNote(tr(
                            "Puter models appear in the model list only while you are signed in. If Puter asks for verification you cannot complete, add an API key from Providers and models instead.",
                            "تظهر نماذج Puter في قائمة النماذج فقط أثناء تسجيل الدخول. إذا طلب Puter تحققاً لا تستطيع إكماله، أضف مفتاح API من صفحة المزودين والنماذج بدلاً من ذلك."
                        ))
                    }

                    Page.Providers -> {
                        status?.let { InfoNote(it) }
                        SettingsGroup(tr("Saved accounts", "الحسابات المحفوظة")) {
                            if (accounts.isEmpty()) {
                                Text(tr("No accounts yet. Add a provider below.", "لا توجد حسابات بعد. أضف مزوداً من القائمة أدناه."), color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(16.dp))
                            }
                            accounts.forEachIndexed { i, a ->
                                SettingsRow(
                                    icon = Icons.Outlined.Key, title = a.label,
                                    subtitle = defaultModelOf(context, a).ifBlank { tr("No model yet", "لا يوجد نموذج بعد") },
                                    showDivider = i < accounts.lastIndex, onClick = { pickingFor = a }
                                )
                            }
                        }
                        SettingsGroup(tr("Add a provider", "إضافة مزوّد")) {
                            addableProviders.forEachIndexed { i, spec ->
                                SettingsRow(
                                    icon = Icons.Outlined.Dns, title = spec.name, subtitle = spec.note.takeIf { it.isNotBlank() },
                                    showDivider = i < addableProviders.lastIndex, onClick = { addingFor = spec },
                                    trailing = { Icon(Icons.Default.Add, null, tint = AccentBlue) }
                                )
                            }
                        }
                    }

                    Page.Data -> {
                        SettingsGroup(tr("Local data", "البيانات المحلية")) {
                            SettingsRow(Icons.Outlined.DeleteSweep, tr("Clear all chats", "حذف كل المحادثات"), onClick = { confirmClear = true })
                            SettingsRow(Icons.Outlined.DeleteForever, tr("Delete all data on this device", "حذف كل البيانات على هذا الجهاز"), danger = true, showDivider = false, onClick = { confirmWipe = true })
                        }
                        if (wiped) InfoNote(tr("All local data was deleted.", "تم حذف كل البيانات المحلية."))
                        InfoNote(tr(
                            "Chats, memories, saved accounts and keys are stored only on this device.",
                            "المحادثات والذاكرة والحسابات والمفاتيح محفوظة على هذا الجهاز فقط."
                        ))
                    }

                    Page.Language -> {
                        SettingsGroup {
                            ChoiceRow("العربية", null, Lang.arabic, true) { Lang.set(context, true) }
                            ChoiceRow("English", null, !Lang.arabic, false) { Lang.set(context, false) }
                        }
                    }

                    Page.Appearance -> {
                        AppearancePage()
                    }

                    Page.FontSize -> {
                        FontSizePage(vm)
                    }

                    Page.Personalization -> {
                        PersonalizationPage(sp, onNavigateToMemory)
                    }

                    Page.Audio -> {
                        AudioPage(sp, vm)
                    }

                    Page.About -> {
                        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            AuroraOrb(72.dp)
                            Text(tr(Brand.EN, Brand.AR), color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                            if (versionLabel.isNotBlank()) Text(tr("Version $versionLabel", "الإصدار $versionLabel"), color = TextMuted, fontSize = 13.sp)
                        }
                        InfoNote(tr(
                            "A chat app that connects to Puter or to the AI providers you add with your own API keys.",
                            "تطبيق محادثة يتصل بـ Puter أو بمزودي الذكاء الاصطناعي الذين تضيفهم بمفاتيح API الخاصة بك."
                        ))
                        SettingsGroup {
                            SettingsRow(Icons.Outlined.SystemUpdate, tr("Check for updates", "التحقق من التحديثات"), onClick = onNavigateToUpdater)
                            SettingsRow(Icons.Outlined.Description, tr("Service agreement", "اتفاقية الخدمة"), onClick = { page = Page.Terms })
                            SettingsRow(Icons.Outlined.Code, tr("Source code", "الكود المصدري"), showDivider = false, onClick = { openUrl(context, REPO_URL) })
                        }
                    }

                    Page.Terms -> TermsPage()

                    Page.Help -> {
                        HelpPage()
                        SettingsGroup {
                            SettingsRow(Icons.Outlined.BugReport, tr("Report a problem", "الإبلاغ عن مشكلة"), tr("Opens GitHub issues", "يفتح صفحة المشاكل على GitHub"), onClick = { openUrl(context, ISSUES_URL) })
                            SettingsRow(Icons.Outlined.Feedback, tr("Send feedback", "إرسال ملاحظات"), showDivider = false, onClick = { openUrl(context, ISSUES_URL) })
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (confirmLogout) ConfirmDialog(
        title = tr("Log out of Puter?", "تسجيل الخروج من Puter؟"),
        message = tr("Puter models will be hidden until you sign in again. Your saved API keys are not affected.", "ستختفي نماذج Puter حتى تسجّل الدخول مرة أخرى. مفاتيح API المحفوظة لن تتأثر."),
        confirmLabel = tr("Log out", "تسجيل الخروج"), danger = true,
        onConfirm = { signOut(); confirmLogout = false }, onDismiss = { confirmLogout = false }
    )

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

private fun toneLabel(tone: String): String = when (tone) {
    "Precise" -> tr("Precise", "دقيق")
    "Creative" -> tr("Creative", "إبداعي")
    else -> tr("Balanced", "متوازن")
}

@Composable
private fun ProfileCard(signedIn: Boolean, username: String, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(SecondarySurface)
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(54.dp).background(AccentBrush, CircleShape), contentAlignment = Alignment.Center) {
            Text(username.firstOrNull()?.uppercase() ?: "✦", color = OnAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (signedIn && username.isNotEmpty()) username else tr("Not signed in", "غير مسجّل الدخول"),
                color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(if (signedIn) "Puter" else tr("Sign in to use Puter models", "سجّل الدخول لاستخدام نماذج Puter"), color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun InfoNote(text: String) {
    Text(text, color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp))
}

@Composable
private fun ChoiceRow(title: String, subtitle: String?, selected: Boolean, showDivider: Boolean, onClick: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 56.dp).padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                if (subtitle != null) Text(subtitle, color = TextMuted, fontSize = 12.sp)
            }
            if (selected) Icon(Icons.Default.Check, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
        }
        if (showDivider) Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(1.dp).background(BorderColor))
    }
}

@Composable
private fun OptionChips(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { option ->
            val on = option.first == selected
            Box(
                Modifier.clip(RoundedCornerShape(50))
                    .background(if (on) AccentBlue.copy(alpha = 0.18f) else ElevatedSurface)
                    .border(1.dp, if (on) AccentBlue else BorderColor, RoundedCornerShape(50))
                    .clickable { onSelect(option.first) }.padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) { Text(option.second, color = if (on) AccentBlue else TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable
private fun SectionCard(title: String, description: String? = null, content: @Composable ColumnScope.() -> Unit) {
    SettingsGroup(title) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (description != null) Text(description, color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
            content()
        }
    }
}

@Composable
private fun AppearancePage() {
    val context = LocalContext.current
    AppPalettes.all.chunked(2).forEach { rowItems ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            rowItems.forEach { p ->
                val selected = ThemeState.palette.id == p.id
                Column(Modifier.weight(1f)) {
                    Column(
                        Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(16.dp)).background(p.base)
                            .border(if (selected) 2.dp else 1.dp, if (selected) p.accent else p.border, RoundedCornerShape(16.dp))
                            .clickable { ThemeState.select(context, p.id) }.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.align(Alignment.End).size(56.dp, 14.dp).background(p.userBubble, RoundedCornerShape(7.dp)))
                        Box(Modifier.size(70.dp, 9.dp).background(p.textSecondary.copy(alpha = 0.5f), RoundedCornerShape(5.dp)))
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.size(16.dp).background(p.accent, CircleShape))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(p.name, color = TextPrimary, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
            if (rowItems.size < 2) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun FontSizePage(vm: AiViewModel) {
    val current = vm.fontSize
    var draft by remember { mutableStateOf(current) }
    SectionCard(tr("Preview", "معاينة")) {
        Text(
            tr("This is how chat text will look.", "هكذا سيظهر نص المحادثة."),
            color = TextPrimary, fontSize = (16f * draft / current).sp, lineHeight = (26f * draft / current).sp
        )
    }
    SectionCard(tr("Size", "الحجم")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("A", color = TextSecondary, fontSize = (13f / current).sp)
            Slider(
                value = draft, onValueChange = { draft = it }, valueRange = 0.85f..1.4f,
                onValueChangeFinished = { vm.setFontSize(draft) },
                colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue, inactiveTrackColor = BorderColor),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            Text("A", color = TextSecondary, fontSize = (22f / current).sp)
        }
        Text("${(draft * 100).roundToInt()}%", color = TextMuted, fontSize = 12.sp)
        HiyakoButton(tr("Reset to default", "إعادة للافتراضي"), onClick = { draft = 1f; vm.setFontSize(1f) }, kind = ButtonKind.Tonal, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PersonalizationPage(sp: android.content.SharedPreferences, onOpenMemory: () -> Unit) {
    var instruction by remember { mutableStateOf(sp.getString("custom_instruction", "") ?: "") }
    var tone by remember { mutableStateOf(sp.getString("tone", "Balanced") ?: "Balanced") }
    var useMemory by remember { mutableStateOf(sp.getBoolean("use_memory", true)) }
    var contextLimit by remember { mutableStateOf(sp.getInt("context_limit", 30).toString()) }
    var maxTokens by remember { mutableStateOf(sp.getInt("max_tokens", 0).toString()) }

    SectionCard(
        tr("Custom instructions", "تعليمات مخصصة"),
        tr("Added to every chat, for example your name, your job or how you want answers written.", "تُضاف إلى كل محادثة، مثل اسمك أو عملك أو طريقة كتابة الإجابات التي تفضلها.")
    ) {
        OutlinedTextField(
            value = instruction,
            onValueChange = { v -> val cut = v.take(1500); instruction = cut; sp.edit().putString("custom_instruction", cut).apply() },
            placeholder = { Text(tr("For example: answer briefly and in Arabic.", "مثال: أجب باختصار وباللغة العربية.")) },
            minLines = 4, maxLines = 8, modifier = Modifier.fillMaxWidth()
        )
        Text("${instruction.length}/1500", color = TextMuted, fontSize = 12.sp)
    }
    SectionCard(tr("Response style", "أسلوب الإجابة"), tr("Controls how predictable or varied the answers are.", "يتحكم في مدى ثبات الإجابات أو تنوعها.")) {
        OptionChips(
            listOf("Precise" to toneLabel("Precise"), "Balanced" to toneLabel("Balanced"), "Creative" to toneLabel("Creative")),
            tone
        ) { tone = it; sp.edit().putString("tone", it).apply() }
    }
    SectionCard(tr("Memory", "الذاكرة")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Use saved memories", "استخدام الذكريات المحفوظة"), color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(tr("Sends your saved memories with each message.", "يرسل ذكرياتك المحفوظة مع كل رسالة."), color = TextMuted, fontSize = 12.sp)
            }
            Switch(
                checked = useMemory, onCheckedChange = { useMemory = it; sp.edit().putBoolean("use_memory", it).apply() },
                colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue, checkedThumbColor = OnAccent)
            )
        }
        HiyakoButton(tr("Manage memories", "إدارة الذكريات"), onClick = onOpenMemory, kind = ButtonKind.Tonal, modifier = Modifier.fillMaxWidth())
    }
    SectionCard(tr("Conversation length", "طول سياق المحادثة"), tr("How many recent messages are sent with each request. Fewer messages use less credit.", "عدد الرسائل الأخيرة المرسلة مع كل طلب. رسائل أقل تعني استهلاكاً أقل.")) {
        OptionChips(listOf("10", "20", "30", "40", "60").map { it to it }, contextLimit) { contextLimit = it; sp.edit().putInt("context_limit", it.toInt()).apply() }
    }
    SectionCard(tr("Maximum reply length", "أقصى طول للرد"), tr("Limits the length of each answer in tokens. Auto lets the provider decide.", "يحدد طول كل إجابة بالرموز. تلقائي يترك القرار للمزود.")) {
        OptionChips(
            listOf("0" to tr("Auto", "تلقائي"), "512" to "512", "1024" to "1024", "2048" to "2048", "4096" to "4096"),
            maxTokens
        ) { maxTokens = it; sp.edit().putInt("max_tokens", it.toInt()).apply() }
    }
}

@Composable
private fun AudioPage(sp: android.content.SharedPreferences, vm: AiViewModel) {
    var inputLang by remember { mutableStateOf(sp.getString("voice_input_lang", "auto") ?: "auto") }
    var speechLang by remember { mutableStateOf(sp.getString("speech_lang", "auto") ?: "auto") }
    var rate by remember { mutableStateOf(sp.getFloat("speech_rate", 1f)) }
    val langs = listOf("auto" to tr("Auto", "تلقائي"), "ar" to "العربية", "en" to "English")

    SectionCard(tr("Voice input language", "لغة الإدخال الصوتي"), tr("Used by the microphone button in the message box.", "تُستخدم مع زر الميكروفون في صندوق الرسالة.")) {
        OptionChips(langs, inputLang) { inputLang = it; sp.edit().putString("voice_input_lang", it).apply() }
    }
    SectionCard(tr("Reading language", "لغة القراءة"), tr("Language of the voice that reads answers aloud.", "لغة الصوت الذي يقرأ الإجابات.")) {
        OptionChips(langs, speechLang) { speechLang = it; sp.edit().putString("speech_lang", it).apply() }
    }
    SectionCard(tr("Reading speed", "سرعة القراءة")) {
        Slider(
            value = rate, onValueChange = { rate = it }, valueRange = 0.5f..2f,
            onValueChangeFinished = { sp.edit().putFloat("speech_rate", rate).apply() },
            colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue, inactiveTrackColor = BorderColor)
        )
        Text("x" + "%.1f".format(rate), color = TextMuted, fontSize = 12.sp)
        HiyakoButton(
            tr("Test voice", "تجربة الصوت"),
            onClick = { sp.edit().putFloat("speech_rate", rate).apply(); vm.speak(tr("Hello, this is a voice test.", "مرحباً، هذه تجربة للصوت.")) },
            kind = ButtonKind.Tonal, modifier = Modifier.fillMaxWidth(), icon = Icons.Outlined.VolumeUp
        )
    }
}

@Composable
private fun TermsPage() {
    val sections = listOf(
        tr("What this app is", "ما هذا التطبيق") to tr(
            "Nexus is a chat client. It does not run AI models itself. It sends your messages to the service you choose: Puter, or a provider you add with your own API key.",
            "نيكسوس تطبيق محادثة. لا يشغّل نماذج ذكاء اصطناعي بنفسه، بل يرسل رسائلك إلى الخدمة التي تختارها: Puter أو مزوّد تضيفه بمفتاح API الخاص بك."
        ),
        tr("Third-party services", "الخدمات الخارجية") to tr(
            "Your messages, and any file or image you attach, are processed by the selected provider under that provider's own terms and privacy policy. Usage costs and limits come from your account with them.",
            "تُعالَج رسائلك وأي ملف أو صورة ترفقها لدى المزوّد المختار وفق شروطه وسياسة خصوصيته. التكاليف والحدود تأتي من حسابك لديه."
        ),
        tr("Data on your device", "البيانات على جهازك") to tr(
            "Chats, memories and settings are stored on this device. API keys are stored encrypted on this device. You can delete everything from Settings, Data controls.",
            "المحادثات والذكريات والإعدادات محفوظة على هذا الجهاز، ومفاتيح API محفوظة مشفّرة عليه. يمكنك حذف كل شيء من الإعدادات ثم التحكم في البيانات."
        ),
        tr("GitHub workspace", "مساحة عمل GitHub") to tr(
            "If you turn on the workspace feature, the loaded repository files are sent to the selected provider with your messages. Turn it off when you do not want that.",
            "إذا فعّلت مساحة العمل فسيتم إرسال ملفات المستودع المحمّل إلى المزوّد المختار مع رسائلك. أوقفها إن لم ترد ذلك."
        ),
        tr("Your responsibility", "مسؤوليتك") to tr(
            "Answers from AI can be wrong. Check important information before relying on it. Keep your API keys private and do not use the app for anything unlawful.",
            "قد تكون إجابات الذكاء الاصطناعي خاطئة، فتحقق من المعلومات المهمة قبل الاعتماد عليها. حافظ على سرية مفاتيحك ولا تستخدم التطبيق في أي أمر مخالف للقانون."
        )
    )
    sections.forEach { (heading, body) ->
        SettingsGroup(heading) {
            Text(body, color = TextSecondary, fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun HelpPage() {
    val faq = listOf(
        tr("I cannot send messages", "لا أستطيع إرسال الرسائل") to tr(
            "Open the model picker at the top of the chat and choose a provider you have set up. If the list is empty, add an API key in Settings, Providers and models, or sign in to Puter.",
            "افتح قائمة النماذج أعلى المحادثة واختر مزوداً أعددته. إذا كانت القائمة فارغة فأضف مفتاح API من الإعدادات ثم المزودون والنماذج، أو سجّل الدخول إلى Puter."
        ),
        tr("How do I add a provider?", "كيف أضيف مزوداً؟") to tr(
            "Go to Settings, Providers and models, pick a provider, paste your API key and save. The model list loads automatically.",
            "اذهب إلى الإعدادات ثم المزودون والنماذج، اختر مزوداً، الصق مفتاح API واحفظ. تُحمَّل قائمة النماذج تلقائياً."
        ),
        tr("A provider shows no models", "مزوّد لا تظهر له نماذج") to tr(
            "Open the account in Providers and models and tap Refresh models. Check that the key is valid and the base URL is correct.",
            "افتح الحساب في المزودون والنماذج واضغط تحديث النماذج. تأكد أن المفتاح صحيح وأن رابط الأساس سليم."
        ),
        tr("Puter asks me for verification", "Puter يطلب مني التحقق") to tr(
            "That check is done by Puter and cannot be skipped from this app. You can use any provider with an API key instead.",
            "هذا التحقق يتم من Puter ولا يمكن تجاوزه من التطبيق. يمكنك استخدام أي مزوّد بمفتاح API بدلاً منه."
        ),
        tr("The microphone button does nothing", "زر الميكروفون لا يعمل") to tr(
            "It uses the speech recognition service installed on your phone, usually Google. Make sure it is installed and enabled.",
            "يستخدم خدمة التعرف على الصوت المثبتة في هاتفك وهي غالباً من Google. تأكد أنها مثبتة ومفعّلة."
        )
    )
    faq.forEach { (q, a) ->
        SettingsGroup {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(q, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(a, color = TextSecondary, fontSize = 14.sp, lineHeight = 22.sp)
            }
        }
    }
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
