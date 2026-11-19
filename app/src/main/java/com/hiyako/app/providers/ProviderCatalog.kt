package com.hiyako.app.providers

import android.content.Context
import com.hiyako.app.security.KeyVault
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * A provider the app knows how to talk to.
 * editableBaseUrl = true means the user must type or confirm the endpoint.
 * Endpoints marked "not verified" in note must be checked against the provider docs before release.
 */
data class ProviderSpec(
    val id: String,
    val name: String,
    val baseUrl: String,
    val defaultModel: String,
    val needsKey: Boolean = true,
    val editableBaseUrl: Boolean = false,
    val note: String = ""
)

object ProviderCatalog {
    const val PUTER = "puter"
    const val CUSTOM = "custom"

    val all: List<ProviderSpec> = listOf(
        ProviderSpec(
            id = PUTER,
            name = "Puter.js",
            baseUrl = "",
            defaultModel = "",
            needsKey = false,
            note = "Sign in with Puter. No API key needed; usage follows your Puter account credits."
        ),
        ProviderSpec(
            id = "freeaiapikey",
            name = "FreeAIapikey",
            baseUrl = "",
            defaultModel = "",
            editableBaseUrl = true,
            note = "Endpoint not verified yet. Enter the base URL from the provider's docs."
        ),
        ProviderSpec(
            id = "agentrouter",
            name = "AgentRouter",
            baseUrl = "",
            defaultModel = "",
            editableBaseUrl = true,
            note = "Endpoint not verified yet. Enter the base URL from the provider's docs."
        ),
        ProviderSpec(
            id = "github",
            name = "GitHub Models",
            baseUrl = "https://models.github.ai/inference",
            defaultModel = "openai/gpt-4.1-mini",
            note = "Use a GitHub token with Models access."
        ),
        ProviderSpec(
            id = "groq",
            name = "Groq",
            baseUrl = "https://api.groq.com/openai/v1",
            defaultModel = "llama-3.3-70b-versatile",
            note = "Fast inference with a free tier."
        ),
        ProviderSpec(
            id = "gemini",
            name = "Google AI Studio (Gemini)",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
            defaultModel = "gemini-2.5-flash",
            note = "Gemini through its OpenAI-compatible endpoint."
        ),
        ProviderSpec(
            id = "nvidia",
            name = "NVIDIA NIM",
            baseUrl = "https://integrate.api.nvidia.com/v1",
            defaultModel = "meta/llama-3.3-70b-instruct",
            note = "Hosted open models from NVIDIA."
        ),
        ProviderSpec(
            id = "openrouter",
            name = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1",
            defaultModel = "openrouter/auto",
            note = "One key for hundreds of models, including free ones."
        ),
        ProviderSpec(
            id = "cloudflare",
            name = "Cloudflare Workers AI",
            baseUrl = "",
            defaultModel = "@cf/meta/llama-3.1-8b-instruct",
            editableBaseUrl = true,
            note = "Base URL is https://api.cloudflare.com/client/v4/accounts/{account_id}/ai/v1"
        ),
        ProviderSpec(
            id = "requesty",
            name = "Requesty",
            baseUrl = "https://router.requesty.ai/v1",
            defaultModel = "openai/gpt-4o-mini",
            note = "Router that exposes many models behind one key."
        ),
        ProviderSpec(
            id = "mistral",
            name = "Mistral AI",
            baseUrl = "https://api.mistral.ai/v1",
            defaultModel = "mistral-small-latest"
        ),
        ProviderSpec(
            id = "cerebras",
            name = "Cerebras",
            baseUrl = "https://api.cerebras.ai/v1",
            defaultModel = "llama3.1-8b",
            note = "Very fast inference."
        ),
        ProviderSpec(
            id = "huggingface",
            name = "Hugging Face",
            baseUrl = "https://router.huggingface.co/v1",
            defaultModel = "meta-llama/Llama-3.1-8B-Instruct"
        ),
        ProviderSpec(
            id = "anakin",
            name = "Anakin.ai",
            baseUrl = "",
            defaultModel = "",
            editableBaseUrl = true,
            note = "Endpoint not verified yet. Enter the base URL from the provider's docs."
        ),
        ProviderSpec(
            id = CUSTOM,
            name = "Custom (any OpenAI-compatible API)",
            baseUrl = "",
            defaultModel = "",
            editableBaseUrl = true,
            note = "Any service with a /chat/completions endpoint. Base URL must start with https://"
        )
    )

    fun find(id: String): ProviderSpec? = all.firstOrNull { it.id == id }
}

/** A saved account. The API key is stored only as an encrypted blob in keyEnc. */
data class Account(
    val id: String,
    val providerId: String,
    val label: String,
    val baseUrl: String,
    val model: String,
    val keyEnc: String
)

object AccountStore {
    private const val PREFS_KEY = "accounts_v1"

    fun load(context: Context): List<Account> {
        val raw = KeyVault.prefs(context).getString(PREFS_KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val result = ArrayList<Account>(array.length())
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                result.add(
                    Account(
                        id = o.getString("id"),
                        providerId = o.getString("providerId"),
                        label = o.optString("label"),
                        baseUrl = o.optString("baseUrl"),
                        model = o.optString("model"),
                        keyEnc = o.optString("keyEnc")
                    )
                )
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun save(context: Context, accounts: List<Account>) {
        val array = JSONArray()
        for (a in accounts) {
            array.put(
                JSONObject()
                    .put("id", a.id)
                    .put("providerId", a.providerId)
                    .put("label", a.label)
                    .put("baseUrl", a.baseUrl)
                    .put("model", a.model)
                    .put("keyEnc", a.keyEnc)
            )
        }
        KeyVault.prefs(context).edit().putString(PREFS_KEY, array.toString()).apply()
    }

    fun add(
        context: Context,
        providerId: String,
        label: String,
        baseUrl: String,
        model: String,
        apiKey: String
    ): Account {
        val account = Account(
            id = UUID.randomUUID().toString(),
            providerId = providerId,
            label = label,
            baseUrl = baseUrl.trim(),
            model = model.trim(),
            keyEnc = if (apiKey.isEmpty()) "" else KeyVault.encrypt(apiKey)
        )
        save(context, load(context) + account)
        return account
    }

    fun remove(context: Context, accountId: String) {
        save(context, load(context).filterNot { it.id == accountId })
    }

    /** Returns the decrypted key, or an empty string when the account has none. Never log the result. */
    fun apiKey(account: Account): String {
        if (account.keyEnc.isEmpty()) return ""
        return KeyVault.decrypt(account.keyEnc).orEmpty()
    }
}
