package com.hiyako.app.providers

import android.content.Context
import com.hiyako.app.security.KeyVault
import org.json.JSONArray

/** Model ids loaded from each account's /models endpoint, cached on the device. */
object ModelCache {
    private fun key(accountId: String) = "models_$accountId"

    fun models(context: Context, accountId: String): List<String> {
        val raw = KeyVault.prefs(context).getString(key(accountId), null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { array.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, accountId: String, ids: List<String>) {
        KeyVault.prefs(context).edit().putString(key(accountId), JSONArray(ids).toString()).apply()
    }

    fun remove(context: Context, accountId: String) {
        KeyVault.prefs(context).edit().remove(key(accountId)).apply()
    }
}

/** The model a request uses when the account has no explicit default. */
fun defaultModelOf(context: Context, account: Account): String =
    account.model.ifBlank { ModelCache.models(context, account.id).firstOrNull().orEmpty() }

/** Stores the account's default model. */
fun setAccountModel(context: Context, accountId: String, model: String) {
    val updated = AccountStore.load(context).map { if (it.id == accountId) it.copy(model = model) else it }
    AccountStore.save(context, updated)
}
