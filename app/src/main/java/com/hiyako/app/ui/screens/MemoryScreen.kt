package com.hiyako.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiyako.app.ui.components.*
import com.hiyako.app.ui.i18n.tr
import com.hiyako.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(onBack: () -> Unit, vm: AiViewModel) {
    val memories by vm.memories.collectAsState()
    var adding by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var newText by remember { mutableStateOf("") }

    ScreenScaffold(
        title = tr("Memory", "الذاكرة"), onBack = onBack,
        actions = { if (memories.isNotEmpty()) IconButton(onClick = { confirmClear = true }) { Icon(Icons.Outlined.DeleteSweep, tr("Clear all", "مسح الكل"), tint = DangerColor) } },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true }, containerColor = AccentBlue, contentColor = OnAccent,
                icon = { Icon(Icons.Default.Add, null) }, text = { Text(tr("Add", "إضافة")) }
            )
        }
    ) { padding ->
        if (memories.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Outlined.Memory, tr("No memories yet", "لا توجد ذكريات بعد"), tr("Add facts you want the assistant to remember.", "أضف حقائق تريد أن يتذكرها المساعد."), tr("Add a memory", "إضافة ذاكرة"), { adding = true })
            }
        } else LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(memories, key = { it.id }) { m ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SecondarySurface).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(m.content, color = TextPrimary, fontSize = 15.sp, lineHeight = 24.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.deleteMemory(m.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Delete, null, tint = DangerColor, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    if (adding) {
        AlertDialog(
            onDismissRequest = { adding = false }, containerColor = CardDark, shape = RoundedCornerShape(24.dp),
            title = { Text(tr("Add a memory", "إضافة ذاكرة"), color = TextPrimary) },
            text = { OutlinedTextField(newText, { newText = it }, minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth(), placeholder = { Text(tr("e.g. My name is Salem", "مثال: اسمي سالم"), color = TextMuted) }) },
            confirmButton = { TextButton(onClick = { if (newText.isNotBlank()) { vm.addMemory(newText); newText = ""; adding = false } }, enabled = newText.isNotBlank()) { Text(tr("Save", "حفظ"), color = AccentBlue) } },
            dismissButton = { TextButton(onClick = { adding = false }) { Text(tr("Cancel", "إلغاء"), color = TextSecondary) } }
        )
    }

    if (confirmClear) {
        ConfirmDialog(
            title = tr("Clear all memories?", "مسح كل الذكريات؟"), message = tr("This cannot be undone.", "لا يمكن التراجع."),
            confirmLabel = tr("Clear", "مسح"), danger = true,
            onConfirm = { vm.clearMemories(); confirmClear = false }, onDismiss = { confirmClear = false }
        )
    }
}
