package com.hiyako.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiyako.app.ui.i18n.tr
import com.hiyako.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(onBack: () -> Unit, vm: AiViewModel) {
    val memories by vm.memories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newMemoryText by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("Memory", "الذاكرة"), color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary) } },
                actions = { 
                    IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, null, tint = AccentBlue) }
                    IconButton(onClick = { vm.clearMemories() }) { Icon(Icons.Default.Delete, null, tint = Color.Red) } 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OledBlack)
            )
        },
        containerColor = OledBlack
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (memories.isEmpty()) {
                item { 
                    Text(tr("No memories saved.", "لا توجد ذكريات محفوظة."), color = TextSecondary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) 
                }
            } else {
                items(memories, key = { it.id }) { memory ->
                    Surface(color = CardDark, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(memory.content, color = TextPrimary, modifier = Modifier.padding(16.dp), fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = CardDark,
            title = { Text(tr("Add Memory", "إضافة ذكرى"), color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newMemoryText,
                    onValueChange = { newMemoryText = it },
                    placeholder = { Text(tr("e.g. My name is Ahmed", "مثال: اسمي أحمد")) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newMemoryText.isNotBlank()) {
                        vm.addMemory(newMemoryText)
                        newMemoryText = ""
                        showAddDialog = false
                    }
                }) { Text(tr("Save", "حفظ"), color = AccentBlue) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text(tr("Cancel", "إلغاء"), color = TextSecondary) } }
        )
    }
}
