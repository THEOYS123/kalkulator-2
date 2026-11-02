package com.example.data.repository

import com.example.data.model.AdminStats
import com.example.data.model.AdminUserItem
import com.example.data.model.CloudFileItem
import com.example.data.model.CloudQuotaInfo
import com.example.data.model.RemoteAppConfig
import com.example.data.model.VaultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class CloudVaultClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return if (!trimmed.endsWith(".php") && !trimmed.contains("api.php")) {
            "${trimmed.trimEnd('/')}/api.php"
        } else {
            trimmed
        }
    }

    private fun parseJsonResponse(respStr: String, httpCode: Int): JSONObject {
        val trimmed = respStr.trim()
        if (trimmed.startsWith("<") || trimmed.contains("<html", ignoreCase = true)) {
            when {
                trimmed.contains("aes.js") || trimmed.contains("requires Javascript", ignoreCase = true) || trimmed.contains("InfinityFree", ignoreCase = true) -> {
                    throw IllegalStateException("Hosting InfinityFree Free Plan memblokir API Android dengan proteksi bot browser (TestCookie/aes.js). Solusi: gunakan hosting API yang mendukung aplikasi mobile seperti Render.com, cPanel, atau XAMPP lokal.")
                }
                httpCode == 404 -> {
                    throw IllegalStateException("File api.php tidak ditemukan di server (HTTP 404). Pastikan file api.php sudah diunggah.")
                }
                httpCode in 500..599 -> {
                    throw IllegalStateException("Terjadi kesalahan internal server (HTTP $httpCode). Periksa error log hosting.")
                }
                else -> {
                    throw IllegalStateException("Server mengembalikan halaman HTML (bukan JSON API). Periksa URL endpoint.")
                }
            }
        }
        return try {
            JSONObject(trimmed)
        } catch (e: Exception) {
            throw IllegalStateException("Format respon server tidak valid: ${trimmed.take(120)}")
        }
    }

    suspend fun fetchRemoteConfig(serverUrl: String): Result<RemoteAppConfig> = withContext(Dispatchers.IO) {
        try {
            val endpoint = normalizeUrl(serverUrl)
            val request = Request.Builder()
                .url("$endpoint?action=config")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; NetSubnet App)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val json = parseJsonResponse(respStr, response.code)
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    val cfg = RemoteAppConfig(
                        appName = json.optString("app_name", "NetSubnet"),
                        announcement = json.optString("announcement", ""),
                        maintenanceMode = json.optBoolean("maintenance_mode", false),
                        maxUploadSizeMb = json.optInt("max_upload_size_mb", 50),
                        defaultUserQuotaMb = json.optInt("default_user_quota_mb", 1024),
                        remoteTheme = json.optString("remote_theme", "CYAN"),
                        openingMode = json.optString("opening_mode", "NONE"),
                        configVersion = json.optInt("config_version", 2)
                    )
                    Result.success(cfg)
                } else {
                    Result.failure(Exception(json.optString("error", "Gagal memuat konfigurasi server")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(serverUrl: String, username: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val endpoint = normalizeUrl(serverUrl)
            val jsonBody = JSONObject().apply {
                put("username", username)
                put("password", password)
            }
            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("$endpoint?action=register")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; NetSubnet App)")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val json = parseJsonResponse(respStr, response.code)
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(json.optString("message", "Pendaftaran berhasil"))
                } else {
                    Result.failure(Exception(json.optString("error", "Pendaftaran gagal ($respStr)")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(serverUrl: String, username: String, password: String): Result<Pair<String, CloudQuotaInfo>> = withContext(Dispatchers.IO) {
        try {
            val endpoint = normalizeUrl(serverUrl)
            val jsonBody = JSONObject().apply {
                put("username", username)
                put("password", password)
            }
            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("$endpoint?action=login")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; NetSubnet App)")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val json = parseJsonResponse(respStr, response.code)
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    val token = json.getString("token")
                    val quotaObj = json.optJSONObject("quota")
                    val quotaInfo = CloudQuotaInfo(
                        usedBytes = quotaObj?.optLong("used_bytes") ?: 0L,
                        totalBytes = quotaObj?.optLong("total_bytes") ?: 1073741824L,
                        fileCount = quotaObj?.optInt("file_count") ?: 0,
                        username = username
                    )
                    Result.success(Pair(token, quotaInfo))
                } else {
                    Result.failure(Exception(json.optString("error", "Login gagal: ${json.optString("error")}")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendHeartbeat(serverUrl: String, token: String, deviceModel: String, clientVersion: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val endpoint = normalizeUrl(serverUrl)
            val jsonBody = JSONObject().apply {
                put("device_model", deviceModel)
                put("client_version", clientVersion)
            }
            val request = Request.Builder()
                .url("$endpoint?action=heartbeat")
                .header("Authorization", "Bearer $token")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; NetSubnet App)")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(serverUrl: String, token: String, oldPass: String, newPass: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val endpoint = normalizeUrl(serverUrl)
            val jsonBody = JSONObject().apply {
                put("old_password", oldPass)
                put("new_password", newPass)
            }
            val request = Request.Builder()
                .url("$endpoint?action=change_password")
                .header("Authorization", "Bearer $token")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; NetSubnet App)")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val json = parseJsonResponse(respStr, response.code)
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(json.optString("message", "Password berhasil diubah"))
                } else {
                    Result.failure(Exception(json.optString("error", "Gagal mengubah password")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(serverUrl: String, token: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val endpoint = normalizeUrl(serverUrl)
            val request = Request.Builder()
                .url("$endpoint?action=delete_account")
                .header("Authorization", "Bearer $token")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; NetSubnet App)")
                .post("{}".toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val json = parseJsonResponse(respStr, response.code)
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(json.optString("message", "Akun berhasil dihapus"))
                } else {
                    Result.failure(Exception(json.optString("error", "Gagal menghapus akun")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchQuota(serverUrl: String, token: String): Result<CloudQuotaInfo> = withContext(Dispatchers.IO) {
        try {
            val endpoint = normalizeUrl(serverUrl)
            val request = Request.Builder()
                .url("$endpoint?action=quota")
                .header("Authorization", "Bearer $token")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; NetSubnet App)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val json = parseJsonResponse(respStr, response.code)
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    val quotaObj = json.getJSONObject("quota")
                    Result.success(
                        CloudQuotaInfo(
                            usedBytes = quotaObj.optLong("used_bytes"),
                            totalBytes = quotaObj.optLong("total_bytes"),
                            fileCount = quotaObj.optInt("file_count"),
                            username = json.optString("username", "")
                        )
                    )
                } else {
                    Result.failure(Exception(json.optString("error", "Gagal mengambil data kuota")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listCloudFiles(serverUrl: String, token: String): Result<List<CloudFileItem>> = withContext(Dispatchers.IO) {
        try {
            val endpoint = normalizeUrl(serverUrl)
            val request = Request.Builder()
                .url("$endpoint?action=list_files")
                .header("Authorization", "Bearer $token")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; NetSubnet App)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val json = parseJsonResponse(respStr, response.code)
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    val filesArray = json.optJSONArray("files") ?: JSONArray()
                    val list = mutableListOf<CloudFileItem>()
                    for (i in 0 until filesArray.length()) {
                        val f = filesArray.getJSONObject(i)
                        list.add(
                            CloudFileItem(
                                id = f.getString("id"),
                                name = f.getString("name"),
                                originalName = f.getString("original_name"),
                                mimeType = f.optString("mime_type", "application/octet-stream"),
                                size = f.optLong("size", 0L),
                                uploadedAt = f.optLong("uploaded_at", 0L),
                                checksum = f.optString("checksum", "")
                            )
                        )
                    }
                    Result.success(list)
                } else {
                    Result.failure(Exception(json.optString("error", "Gagal mengambil daftar file cloud")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadVaultItem(
        serverUrl: String,
        token: String,
        item: VaultItem,
        decryptedFile: File
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val endpoint = normalizeUrl(serverUrl)
            val fileBody = decryptedFile.asRequestBody(item.mimeType.toMediaTypeOrNull())
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("name", item.name)
                .addFormDataPart("mime_type", item.mimeType)
                .addFormDataPart("file", item.name, fileBody)
                .build()

            val request = Request.Builder()
                .url("$endpoint?action=upload")
                .header("Authorization", "Bearer $token")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; NetSubnet App)")
                .post(multipart)
                .build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val json = parseJsonResponse(respStr, response.code)
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    val cloudFileId = json.getString("file_id")
                    Result.success(cloudFileId)
                } else {
                    Result.failure(Exception(json.optString("error", "Upload gagal: ${json.optString("error")}")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadCloudFile(
        serverUrl: String,
        token: String,
        cloudFileId: String,
        targetFile: File
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val endpoint = normalizeUrl(serverUrl)
            val request = Request.Builder()
                .url("$endpoint?action=download&id=$cloudFileId")
                .header("Authorization", "Bearer $token")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; NetSubnet App)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP Error ${response.code} saat mengunduh"))
                }
                val body = response.body ?: return@withContext Result.failure(Exception("Body kosong"))
                FileOutputStream(targetFile).use { fos ->
                    body.byteStream().use { input ->
                        input.copyTo(fos)
                    }
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCloudFile(
        serverUrl: String,
        token: String,
        cloudFileId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val endpoint = normalizeUrl(serverUrl)
            val request = Request.Builder()
                .url("$endpoint?action=delete&id=$cloudFileId")
                .header("Authorization", "Bearer $token")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; NetSubnet App)")
                .post("{}".toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val json = parseJsonResponse(respStr, response.code)
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(json.optString("error", "Gagal menghapus file dari cloud")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // ADMINISTRATOR API METHODS
    // =========================================================================

    suspend fun adminLogin(serverUrl: String, username: String, password: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val endpoint = normalizeUrl(serverUrl)
            val jsonBody = JSONObject().apply {
                put("username", username)
                put("password", password)
            }
            val request = Request.Builder()
                .url("$endpoint?action=admin_login")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; NetSubnet App)")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val json = parseJsonResponse(respStr, response.code)
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    val token = json.getString("admin_token")
                    val adminUser = json.optString("username", username)
                    Result.success(Pair(token, adminUser))
                } else {
                    Result.failure(Exception(json.optString("error", "Login admin gagal: ${json.optString("error")}")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminFetchStats(serverUrl: String, adminToken: String): Result<AdminStats> = withContext(Dispatchers.IO) {
        try {
            val endpoint = normalizeUrl(serverUrl)
            val request = Request.Builder()
                .url("$endpoint?action=admin_stats")
                .header("Authorization", "Bearer $adminToken")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; NetSubnet App)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val json = parseJsonResponse(respStr, response.code)
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    val statsObj = json.getJSONObject("stats")
                    val usersArr = statsObj.optJSONArray("users") ?: JSONArray()
                    val userList = mutableListOf<AdminUserItem>()
                    for (i in 0 until usersArr.length()) {
                        val u = usersArr.getJSONObject(i)
                        userList.add(
                            AdminUserItem(
                                id = u.getString("id"),
                                username = u.getString("username"),
                                status = u.optString("status", "active"),
                                createdAt = u.optLong("created_at", 0L),
                                lastActive = u.optLong("last_active", 0L),
                                usedBytes = u.optLong("used_bytes", 0L),
                                storageQuotaBytes = u.optLong("storage_quota_bytes", 1073741824L),
                                fileCount = u.optInt("file_count", 0),
                                deviceModel = u.optString("device_model", ""),
                                clientVersion = u.optString("client_version", "")
                            )
                        )
                    }

                    val stats = AdminStats(
                        totalUsers = statsObj.optInt("total_users", 0),
                        activeDaily = statsObj.optInt("active_daily", 0),
                        activeWeekly = statsObj.optInt("active_weekly", 0),
                        activeMonthly = statsObj.optInt("active_monthly", 0),
                        totalStorageBytes = statsObj.optLong("total_storage_bytes", 0L),
                        totalFilesCount = statsObj.optInt("total_files_count", 0),
                        users = userList
                    )
                    Result.success(stats)
                } else {
                    Result.failure(Exception(json.optString("error", "Gagal memuat statistik admin")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminSaveConfig(serverUrl: String, adminToken: String, newConfig: RemoteAppConfig): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val endpoint = normalizeUrl(serverUrl)
            val jsonBody = JSONObject().apply {
                put("app_name", newConfig.appName)
                put("announcement", newConfig.announcement)
                put("maintenance_mode", newConfig.maintenanceMode)
                put("max_upload_size_mb", newConfig.maxUploadSizeMb)
                put("default_user_quota_mb", newConfig.defaultUserQuotaMb)
                put("remote_theme", newConfig.remoteTheme)
                put("opening_mode", newConfig.openingMode)
            }
            val request = Request.Builder()
                .url("$endpoint?action=admin_save_config")
                .header("Authorization", "Bearer $adminToken")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; NetSubnet App)")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val json = parseJsonResponse(respStr, response.code)
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(json.optInt("config_version", newConfig.configVersion + 1))
                } else {
                    Result.failure(Exception(json.optString("error", "Gagal menyimpan konfigurasi server")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminToggleUser(serverUrl: String, adminToken: String, targetUserId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val endpoint = normalizeUrl(serverUrl)
            val jsonBody = JSONObject().apply {
                put("user_id", targetUserId)
            }
            val request = Request.Builder()
                .url("$endpoint?action=admin_toggle_user")
                .header("Authorization", "Bearer $adminToken")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; NetSubnet App)")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val json = parseJsonResponse(respStr, response.code)
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(json.optString("new_status", "active"))
                } else {
                    Result.failure(Exception(json.optString("error", "Gagal mengubah status akun")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminUpdateQuota(serverUrl: String, adminToken: String, targetUserId: String, quotaMb: Int): Result<String> = withContext(Dispatchers.IO) {
        try {
            val endpoint = normalizeUrl(serverUrl)
            val jsonBody = JSONObject().apply {
                put("user_id", targetUserId)
                put("quota_mb", quotaMb)
            }
            val request = Request.Builder()
                .url("$endpoint?action=admin_update_quota")
                .header("Authorization", "Bearer $adminToken")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; NetSubnet App)")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val json = parseJsonResponse(respStr, response.code)
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(json.optString("message", "Kuota berhasil diubah"))
                } else {
                    Result.failure(Exception(json.optString("error", "Gagal mengubah kuota")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminDeleteUser(serverUrl: String, adminToken: String, targetUserId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val endpoint = normalizeUrl(serverUrl)
            val jsonBody = JSONObject().apply {
                put("user_id", targetUserId)
            }
            val request = Request.Builder()
                .url("$endpoint?action=admin_delete_user")
                .header("Authorization", "Bearer $adminToken")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; NetSubnet App)")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val json = parseJsonResponse(respStr, response.code)
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(json.optString("message", "Pengguna berhasil dihapus"))
                } else {
                    Result.failure(Exception(json.optString("error", "Gagal menghapus akun pengguna")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminFetchAuditLogs(serverUrl: String, adminToken: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val endpoint = normalizeUrl(serverUrl)
            val request = Request.Builder()
                .url("$endpoint?action=admin_audit_logs")
                .header("Authorization", "Bearer $adminToken")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; NetSubnet App)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val json = parseJsonResponse(respStr, response.code)
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    val logsArr = json.optJSONArray("logs") ?: JSONArray()
                    val logsList = mutableListOf<String>()
                    for (i in 0 until logsArr.length()) {
                        logsList.add(logsArr.getString(i))
                    }
                    Result.success(logsList)
                } else {
                    Result.failure(Exception(json.optString("error", "Gagal memuat catatan log audit")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminSaveTelegramConfig(
        serverUrl: String,
        adminToken: String,
        botToken: String,
        chatId: String,
        apiBase: String,
        enable2fa: Boolean,
        sendTest: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val endpoint = normalizeUrl(serverUrl)
            val jsonBody = JSONObject().apply {
                put("bot_token", botToken)
                put("chat_id", chatId)
                put("api_base", apiBase)
                put("enable_2fa", enable2fa)
                put("send_test", sendTest)
            }
            val request = Request.Builder()
                .url("$endpoint?action=admin_telegram_config")
                .header("Authorization", "Bearer $adminToken")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; NetSubnet App)")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                val json = parseJsonResponse(respStr, response.code)
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(json.optString("message", "Konfigurasi Telegram tersimpan"))
                } else {
                    Result.failure(Exception(json.optString("error", "Gagal menyimpan konfigurasi Telegram")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
