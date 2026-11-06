package com.nexus.ai.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

data class ChatItem(
    val id: String,
    val title: String,
    val timestamp: Long,
    val isPinned: Boolean = false
)

class DrawerViewModel : ViewModel() {
    // قائمة المحادثات تبدأ فارغة (لا بيانات وهمية)
    val chatList = mutableStateListOf<ChatItem>()
    
    var searchQuery by mutableStateOf("")
    var isMultiSelectMode by mutableStateOf(false)
    val selectedChats = mutableStateListOf<String>()

    fun addChat(title: String) {
        val newChat = ChatItem(
            id = System.currentTimeMillis().toString(),
            title = title,
            timestamp = System.currentTimeMillis()
        )
        chatList.add(0, newChat) // إضافة المحادثة في الأعلى
    }

    fun toggleMultiSelect(chatId: String) {
        if (selectedChats.contains(chatId)) {
            selectedChats.remove(chatId)
        } else {
            selectedChats.add(chatId)
        }
        isMultiSelectMode = selectedChats.isNotEmpty()
    }

    fun exitMultiSelect() {
        selectedChats.clear()
        isMultiSelectMode = false
    }

    fun deleteSelected() {
        chatList.removeAll { selectedChats.contains(it.id) }
        exitMultiSelect()
    }

    fun pinSelected() {
        for (i in chatList.indices) {
            if (selectedChats.contains(chatList[i].id)) {
                chatList[i] = chatList[i].copy(isPinned = true)
            }
        }
        exitMultiSelect()
    }

    fun deleteSingleChat(chatId: String) {
        chatList.removeAll { it.id == chatId }
    }

    fun renameSingleChat(chatId: String, newTitle: String) {
        val index = chatList.indexOfFirst { it.id == chatId }
        if (index != -1) {
            chatList[index] = chatList[index].copy(title = newTitle)
        }
    }
}
