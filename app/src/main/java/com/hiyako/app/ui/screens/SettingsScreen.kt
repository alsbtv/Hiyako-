package com.hiyako.app.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.align
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hiyako.app.data.db.AppDatabase
import com.hiyako.app.providers.AccountStore
import com.hiyako.app.providers.ProviderCatalog
import com.hiyako.app.providers.ProviderSpec
import com.hiyako.app.security.KeyVault
import com.hiyako.app.ui.theme.AppPalette
import com.hiyako.app.ui.theme.AppPalettes
import com.hiyako.app.ui.theme.ThemeState

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
    var accounts by remember { mutableStateOf(AccountStore.load(context)) }
    var addingFor by remember { mutableStateOf<ProviderSpec?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmWipe by remember { mutableStateOf(false) }
    var wiped by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // ---------------------------------------------------------- Appearance
            SectionCard(title = "Appearance", subtitle = "Pick a theme. It applies to the whole app.") {
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
            }

            // ---------------------------------------------------------- Providers
            SectionCard(
                title = "Providers and accounts",
                subtitle = "Add several accounts per provider. Keys are encrypted with the Android Keystore."
            ) {
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
                                OutlinedButton(onClick = onSignIn) { Text("Sign in") }
                            } else {
                                IconButton(onClick = { addingFor = spec }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add account")
                                }
                            }
                        }
                        mine.forEach { account ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(account.label)
                                    Text(
                                        if (account.model.isEmpty()) "No model set" else account.model,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                IconButton(onClick = {
                                    AccountStore.remove(context, account.id)
                                    accounts = AccountStore.load(context)
                                    vm.reloadAccountModels()
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove account")
                                }
                            }
                        }
                    }
                }
            }

            // ---------------------------------------------------------- Tools
            SectionCard(title = "Tools") {
                OutlinedButton(onClick = onNavigateToWorkspace, modifier = Modifier.fillMaxWidth()) {
                    Text("Workspace (GitHub)")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onNavigateToMemory, modifier = Modifier.fillMaxWidth()) {
                    Text("Memory")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onNavigateToUpdater, modifier = Modifier.fillMaxWidth()) {
                    Text("Check for updates")
                }
            }

            // ---------------------------------------------------------- Privacy and data
            SectionCard(
                title = "Privacy and data",
                subtitle = "Everything is stored on this device. Deleting data cannot be undone."
            ) {
                OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Clear all chats")
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { confirmWipe = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete all data on this device")
                }
                if (wiped) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "All local data was deleted. Close and reopen the app to finish.",
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
                AccountStore.add(context, spec.id, label, url, model, key)
                accounts = AccountStore.load(context)
                vm.reloadAccountModels()
                addingFor = null
            }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all chats?") },
            text = { Text("This deletes every conversation on this device. It cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    onClearChat()
                    confirmClear = false
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text("Delete all data?") },
            text = {
                Text("This deletes all chats, memories, saved accounts and keys on this device. It cannot be undone.")
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
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete everything") }
            },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text("Cancel") } }
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
                    contentDescription = "Selected",
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
        title = { Text("Add ${spec.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Account name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (spec.needsKey) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API key") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL (https://)") },
                    singleLine = true,
                    enabled = spec.editableBaseUrl,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val url = baseUrl.trim()
                when {
                    spec.needsKey && apiKey.isBlank() -> error = "Enter an API key."
                    !url.startsWith("https://") -> error = "Base URL must start with https://"
                    model.isBlank() -> error = "Enter a model name."
                    else -> onSave(label.trim().ifEmpty { spec.name }, url, model.trim(), apiKey.trim())
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
