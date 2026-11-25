package com.hiyako.app.providers

import android.content.Context
import com.hiyako.app.security.KeyVault
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * A provider the app knows how to talk to. Every provider here has a free tier.
 * baseUrl is fixed, so the user never types it (only Custom is editable).
 * keyUrl is the page where the user creates an API key.
 * needsAccountId is for services whose URL contains the user's account id (Cloudflare).
 * fallbackModels are used when the provider has no /models endpoint.
 */
data class ProviderSpec(
    val id: String,
    val name: String,
    val baseUrl: String,
    val defaultModel: String,
    val needsKey: Boolean = true,
    val editableBaseUrl: Boolean = false,
    val note: String = "",
    val noteAr: String = "",
    val keyUrl: String = "",
    val needsAccountId: Boolean = false,
    val fallbackModels: List<String> = emptyList()
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
            note = "Sign in with Puter. No API key needed; usage follows your Puter account credits.",
            noteAr = "سجّل الدخول بحساب Puter. لا يحتاج مفتاح API، والاستخدام يتبع رصيد حسابك."
        ),
        ProviderSpec(
            id = "groq",
            name = "Groq",
            baseUrl = "https://api.groq.com/openai/v1",
            defaultModel = "llama-3.3-70b-versatile",
            note = "Very fast. Free tier with rate limits.",
            noteAr = "سريع جداً. طبقة مجانية بحدود استخدام.",
            keyUrl = "https://console.groq.com/keys"
        ),
        ProviderSpec(
            id = "gemini",
            name = "Google AI Studio (Gemini)",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
            defaultModel = "gemini-2.5-flash",
            note = "Free tier with rate limits. On the free tier your prompts may be used to improve Google products.",
            noteAr = "طبقة مجانية بحدود استخدام. في الطبقة المجانية قد تُستخدم رسائلك لتحسين منتجات Google.",
            keyUrl = "https://aistudio.google.com/apikey"
        ),
        ProviderSpec(
            id = "github",
            name = "GitHub Models",
            baseUrl = "https://models.github.ai/inference",
            defaultModel = "openai/gpt-4.1-mini",
            note = "Free with rate limits. Create a token with the Models permission.",
            noteAr = "مجاني بحدود استخدام. أنشئ توكن بصلاحية Models.",
            keyUrl = "https://github.com/settings/personal-access-tokens/new"
        ),
        ProviderSpec(
            id = "openrouter",
            name = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1",
            defaultModel = "",
            note = "One key for many models. Choose models ending in :free to avoid charges.",
            noteAr = "مفتاح واحد لنماذج كثيرة. اختر النماذج التي تنتهي بـ :free لتجنب الرسوم.",
            keyUrl = "https://openrouter.ai/keys"
        ),
        ProviderSpec(
            id = "nvidia",
            name = "NVIDIA NIM",
            baseUrl = "https://integrate.api.nvidia.com/v1",
            defaultModel = "meta/llama-3.3-70b-instruct",
            note = "Hosted open models with free access for developers.",
            noteAr = "نماذج مفتوحة مستضافة مع وصول مجاني للمطورين.",
            keyUrl = "https://build.nvidia.com/settings/api-keys"
        ),
        ProviderSpec(
            id = "cerebras",
            name = "Cerebras",
            baseUrl = "https://api.cerebras.ai/v1",
            defaultModel = "llama3.1-8b",
            note = "Very fast inference. Free tier with limits.",
            noteAr = "استدلال سريع جداً. طبقة مجانية بحدود.",
            keyUrl = "https://cloud.cerebras.ai/"
        ),
        ProviderSpec(
            id = "mistral",
            name = "Mistral AI",
            baseUrl = "https://api.mistral.ai/v1",
            defaultModel = "mistral-small-latest",
            note = "Free plan with rate limits. On the free plan your data may be used to train their models.",
            noteAr = "خطة مجانية بحدود استخدام. في الخطة المجانية قد تُستخدم بياناتك لتدريب نماذجهم.",
            keyUrl = "https://console.mistral.ai/api-keys"
        ),
        ProviderSpec(
            id = "cohere",
            name = "Cohere",
            baseUrl = "https://api.cohere.ai/compatibility/v1",
            defaultModel = "command-a-03-2025",
            note = "Free trial key with a monthly call limit.",
            noteAr = "مفتاح تجريبي مجاني بحد شهري للطلبات.",
            keyUrl = "https://dashboard.cohere.com/api-keys",
            fallbackModels = listOf("command-a-03-2025", "command-r-plus-08-2024", "command-r-08-2024", "command-r7b-12-2024")
        ),
        ProviderSpec(
            id = "cloudflare",
            name = "Cloudflare Workers AI",
            baseUrl = "https://api.cloudflare.com/client/v4/accounts/{account_id}/ai/v1",
            defaultModel = "@cf/meta/llama-3.1-8b-instruct",
            note = "Free daily allowance on Workers AI. Needs your Cloudflare Account ID and an API token.",
            noteAr = "حصة يومية مجانية على Workers AI. يحتاج معرّف حساب Cloudflare وتوكن API.",
            keyUrl = "https://dash.cloudflare.com/profile/api-tokens",
            needsAccountId = true,
            fallbackModels = listOf("@cf/meta/llama-3.1-8b-instruct", "@cf/meta/llama-3.3-70b-instruct-fp8-fast", "@cf/mistralai/mistral-small-3.1-24b-instruct")
        ),
        ProviderSpec(
            id = "requesty",
            name = "Requesty",
            baseUrl = "https://router.requesty.ai/v1",
            defaultModel = "",
            note = "Free models with a daily request limit. Paid models need credit.",
            noteAr = "نماذج مجانية بحد يومي للطلبات. النماذج المدفوعة تحتاج رصيداً.",
            keyUrl = "https://app.requesty.ai/api-keys"
        ),
        ProviderSpec(
            id = "huggingface",
            name = "Hugging Face",
            baseUrl = "https://router.huggingface.co/v1",
            defaultModel = "meta-llama/Llama-3.1-8B-Instruct",
            note = "Free with community rate limits.",
            noteAr = "مجاني بحدود يحددها المجتمع.",
            keyUrl = "https://huggingface.co/settings/tokens"
        ),
        ProviderSpec(
            id = CUSTOM,
            name = "Custom (any OpenAI-compatible API)",
            baseUrl = "",
            defaultModel = "",
            editableBaseUrl = true,
            note = "Any service with a /chat/completions endpoint. Base URL must start with https://",
            noteAr = "أي خدمة فيها /chat/completions. يجب أن يبدأ الرابط بـ https://"
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

    fun save(context: Context, accounts: List<Account>) {
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
        ModelCache.remove(context, accountId)
    }

    /** Returns the decrypted key, or an empty string when the account has none. Never log the result. */
    fun apiKey(account: Account): String {
        if (account.keyEnc.isEmpty()) return ""
        return KeyVault.decrypt(account.keyEnc).orEmpty()
    }
}
