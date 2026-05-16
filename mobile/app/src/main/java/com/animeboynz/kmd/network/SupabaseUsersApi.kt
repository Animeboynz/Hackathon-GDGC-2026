package com.animeboynz.kmd.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object SupabaseUsersApi {
    private const val SUPABASE_URL = "https://djxgcwyayhoqwbqnpfeg.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_zUTrELhzqJY53PNT6mIEwg_HioiXrZp"
    private val jsonMediaType = "application/json".toMediaType()

    suspend fun upsertUser(
        client: OkHttpClient,
        id: Long,
        name: String,
        dob: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("id", id)
                .put("name", name)
                .put("dob", dob)
                .toString()
                .toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/users?on_conflict=id")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Supabase users insert failed: HTTP ${response.code} ${response.body?.string().orEmpty()}")
                }
            }
        }
    }
}
