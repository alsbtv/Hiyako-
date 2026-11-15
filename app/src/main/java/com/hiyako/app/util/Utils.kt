package com.hiyako.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

fun queryDisplayName(context: Context, uri: Uri): String {
    var name: String? = null
    try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) name = cursor.getString(index)
            }
        }
    } catch (e: Exception) {
        // ignore and fall back below
    }
    return name ?: uri.lastPathSegment ?: "file"
}

fun isRtlText(text: String): Boolean {
    for (ch in text) {
        if (Character.isLetter(ch)) {
            val direction = Character.getDirectionality(ch)
            return direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
        }
    }
    return false
}
