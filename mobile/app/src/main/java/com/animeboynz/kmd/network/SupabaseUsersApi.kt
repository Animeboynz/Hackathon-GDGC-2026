package com.animeboynz.kmd.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import org.json.JSONObject

object SupabaseUsersApi {
    private const val SUPABASE_URL = "https://djxgcwyayhoqwbqnpfeg.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_zUTrELhzqJY53PNT6mIEwg_HioiXrZp"
    private val jsonMediaType = "application/json".toMediaType()

    data class UserReliability(
        val id: Long,
        val baseReliability: Long,
        val effectiveReliability: Long,
        val publicKey: String,
    )

    suspend fun upsertUser(
        client: OkHttpClient,
        id: Long,
        name: String,
        dob: String,
        reliability: Long,
        publicKey: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("id", id)
                .put("name", name)
                .put("dob", dob)
                .put("reliability", reliability)
                .put("public_key", publicKey)
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

    suspend fun getReliabilityForPublicKey(
        client: OkHttpClient,
        publicKey: String,
    ): Result<UserReliability> = withContext(Dispatchers.IO) {
        runCatching {
            val encodedKey = publicKey.urlEncode()
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/users?public_key=eq.$encodedKey&select=id,reliability,public_key&limit=1")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Supabase user lookup failed: HTTP ${response.code} ${response.body?.string().orEmpty()}")
                }
                val array = org.json.JSONArray(response.body?.string().orEmpty())
                if (array.length() == 0) error("No user found for this public key")
                val user = array.getJSONObject(0)
                val baseReliability = user.optLong("reliability", 0L).coerceIn(0L, 100L)
                val votes = getVoteCounts(client, publicKey).getOrThrow()
                UserReliability(
                    id = user.getLong("id"),
                    baseReliability = baseReliability,
                    effectiveReliability = scaleReliability(baseReliability, votes.up, votes.down),
                    publicKey = user.optString("public_key", publicKey),
                )
            }
        }
    }

    suspend fun submitReputationVote(
        client: OkHttpClient,
        authorPublicKey: String,
        targetPublicKey: String,
        isReliable: Boolean,
    ): Result<UserReliability> = withContext(Dispatchers.IO) {
        runCatching {
            deleteOppositeVote(client, authorPublicKey, targetPublicKey, isReliable).getOrThrow()
            val body = JSONObject()
                .put("author", authorPublicKey)
                .put("target_user", targetPublicKey)
                .put("is_reliable", isReliable)
                .toString()
                .toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/reputation?on_conflict=author,is_reliable,target_user")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Supabase reputation vote failed: HTTP ${response.code} ${response.body?.string().orEmpty()}")
                }
            }

            getReliabilityForPublicKey(client, targetPublicKey).getOrThrow()
        }
    }

    private data class VoteCounts(val up: Int, val down: Int)

    private suspend fun getVoteCounts(client: OkHttpClient, targetPublicKey: String): Result<VoteCounts> = withContext(Dispatchers.IO) {
        runCatching {
            val encodedTarget = targetPublicKey.urlEncode()
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/reputation?target_user=eq.$encodedTarget&select=is_reliable")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Supabase reputation lookup failed: HTTP ${response.code} ${response.body?.string().orEmpty()}")
                }
                val array = org.json.JSONArray(response.body?.string().orEmpty())
                var up = 0
                var down = 0
                for (index in 0 until array.length()) {
                    if (array.getJSONObject(index).optBoolean("is_reliable")) up++ else down++
                }
                VoteCounts(up = up, down = down)
            }
        }
    }

    private suspend fun deleteOppositeVote(
        client: OkHttpClient,
        authorPublicKey: String,
        targetPublicKey: String,
        isReliable: Boolean,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(
                    "$SUPABASE_URL/rest/v1/reputation" +
                        "?author=eq.${authorPublicKey.urlEncode()}" +
                        "&target_user=eq.${targetPublicKey.urlEncode()}" +
                        "&is_reliable=eq.${(!isReliable)}",
                )
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Supabase opposite vote delete failed: HTTP ${response.code} ${response.body?.string().orEmpty()}")
                }
            }
        }
    }

    private fun scaleReliability(baseReliability: Long, upVotes: Int, downVotes: Int): Long {
        if (baseReliability >= 100L) return 100L
        val voteDelta = ((upVotes - downVotes) * 5L)
        return (baseReliability + voteDelta).coerceIn(0L, 99L)
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())
}
