package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ActivityLogItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "netsubnet_prefs")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        private val KEY_PIN_HASH = stringPreferencesKey("vault_pin_hash")
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("vault_biometric_enabled")
        private val KEY_FAILED_ATTEMPTS = intPreferencesKey("vault_failed_attempts")
        private val KEY_LOCKOUT_UNTIL = longPreferencesKey("vault_lockout_until")
        private val KEY_SERVER_URL = stringPreferencesKey("cloud_server_url")
        private val KEY_AUTH_TOKEN = stringPreferencesKey("cloud_auth_token")
        private val KEY_AUTH_USERNAME = stringPreferencesKey("cloud_auth_username")
        private val KEY_ADMIN_TOKEN = stringPreferencesKey("cloud_admin_token")
        private val KEY_ADMIN_USERNAME = stringPreferencesKey("cloud_admin_username")
        private val KEY_HISTORY_JSON = stringPreferencesKey("calc_history_json")
        private val KEY_SECRET_TRIGGER_INFO_SEEN = booleanPreferencesKey("vault_trigger_info_seen")
        private val KEY_SECRET_IP_CODE = stringPreferencesKey("vault_secret_ip_code")

        // Personalization & Branding Settings
        private val KEY_DISPLAY_APP_NAME = stringPreferencesKey("pref_display_app_name")
        private val KEY_APP_DESCRIPTION = stringPreferencesKey("pref_app_description")
        private val KEY_ACCENT_THEME = stringPreferencesKey("pref_accent_theme")
        private val KEY_OPENING_MODE = stringPreferencesKey("pref_opening_mode")

        // App Vault
        private val KEY_LOCKED_APPS_CSV = stringPreferencesKey("vault_locked_apps_csv")
        private val KEY_SILENCED_APPS_CSV = stringPreferencesKey("vault_silenced_apps_csv")

        // Activity Logs
        private val KEY_ACTIVITY_LOGS_JSON = stringPreferencesKey("vault_activity_logs_json")

        const val DEFAULT_PIN = "1234"
        const val DEFAULT_SECRET_IP = "192.168.99.99"
        const val DEFAULT_SERVER_URL = "http://10.0.2.2/backend_php/api.php"
        const val DEFAULT_DISPLAY_APP_NAME = "NetSubnet"
        const val DEFAULT_APP_DESCRIPTION = "Kalkulator Subnetting IPv4 & VLSM Terpadu dengan Ruang Rahasia"
        const val DEFAULT_ACCENT_THEME = "CYAN"
        const val DEFAULT_OPENING_MODE = "NONE" // "NONE", "BANNER_IMAGE", "ANIMATION"
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    val pinHashFlow: Flow<String?> = context.dataStore.data.map { it[KEY_PIN_HASH] }
    val secretIpCodeFlow: Flow<String> = context.dataStore.data.map { it[KEY_SECRET_IP_CODE] ?: DEFAULT_SECRET_IP }
    val biometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_BIOMETRIC_ENABLED] ?: false }
    val failedAttemptsFlow: Flow<Int> = context.dataStore.data.map { it[KEY_FAILED_ATTEMPTS] ?: 0 }
    val lockoutUntilFlow: Flow<Long> = context.dataStore.data.map { it[KEY_LOCKOUT_UNTIL] ?: 0L }
    val serverUrlFlow: Flow<String> = context.dataStore.data.map { it[KEY_SERVER_URL] ?: DEFAULT_SERVER_URL }
    val authTokenFlow: Flow<String?> = context.dataStore.data.map { it[KEY_AUTH_TOKEN] }
    val authUsernameFlow: Flow<String?> = context.dataStore.data.map { it[KEY_AUTH_USERNAME] }
    val adminTokenFlow: Flow<String?> = context.dataStore.data.map { it[KEY_ADMIN_TOKEN] }
    val adminUsernameFlow: Flow<String?> = context.dataStore.data.map { it[KEY_ADMIN_USERNAME] }
    val historyJsonFlow: Flow<String?> = context.dataStore.data.map { it[KEY_HISTORY_JSON] }

    val displayAppNameFlow: Flow<String> = context.dataStore.data.map { it[KEY_DISPLAY_APP_NAME] ?: DEFAULT_DISPLAY_APP_NAME }
    val appDescriptionFlow: Flow<String> = context.dataStore.data.map { it[KEY_APP_DESCRIPTION] ?: DEFAULT_APP_DESCRIPTION }
    val accentThemeFlow: Flow<String> = context.dataStore.data.map { it[KEY_ACCENT_THEME] ?: DEFAULT_ACCENT_THEME }
    val openingModeFlow: Flow<String> = context.dataStore.data.map { it[KEY_OPENING_MODE] ?: DEFAULT_OPENING_MODE }

    val lockedAppsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        val csv = prefs[KEY_LOCKED_APPS_CSV] ?: ""
        if (csv.isBlank()) emptySet() else csv.split(",").toSet()
    }

    val silencedAppsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        val csv = prefs[KEY_SILENCED_APPS_CSV] ?: ""
        if (csv.isBlank()) emptySet() else csv.split(",").toSet()
    }

    val activityLogsFlow: Flow<List<ActivityLogItem>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_ACTIVITY_LOGS_JSON] ?: "[]"
        try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<ActivityLogItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ActivityLogItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        action = obj.optString("action", ""),
                        details = obj.optString("details", ""),
                        type = obj.optString("type", "SECURITY")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun setSecretIpCode(code: String) {
        context.dataStore.edit { it[KEY_SECRET_IP_CODE] = code.trim() }
        recordActivityLog("CONFIG", "Mengubah kode IP pemicu rahasia", "SECURITY")
    }

    suspend fun verifyPin(enteredPin: String): Boolean {
        val enteredHash = hashPin(enteredPin)
        val prefs = context.dataStore.data.first()
        val storedHash = prefs[KEY_PIN_HASH]
        val targetHash = storedHash ?: hashPin(DEFAULT_PIN)
        val isValid = enteredHash == targetHash
        if (isValid) {
            recordActivityLog("AUTH_SUCCESS", "Berhasil membuka Ruang Rahasia dengan PIN", "SECURITY")
        } else {
            recordActivityLog("AUTH_FAILED", "Percobaan gagal membuka Ruang Rahasia", "SECURITY")
        }
        return isValid
    }

    suspend fun setPin(newPin: String) {
        val hash = hashPin(newPin)
        context.dataStore.edit {
            it[KEY_PIN_HASH] = hash
            it[KEY_FAILED_ATTEMPTS] = 0
            it[KEY_LOCKOUT_UNTIL] = 0L
        }
        recordActivityLog("PIN_CHANGED", "PIN akses Ruang Rahasia berhasil diubah", "SECURITY")
    }

    suspend fun recordFailedAttempt(): Pair<Int, Long> {
        var attempts = 0
        var lockoutUntil = 0L
        context.dataStore.edit {
            attempts = (it[KEY_FAILED_ATTEMPTS] ?: 0) + 1
            it[KEY_FAILED_ATTEMPTS] = attempts
            if (attempts >= 5) {
                // Lockout for 30 seconds
                lockoutUntil = System.currentTimeMillis() + 30_000L
                it[KEY_LOCKOUT_UNTIL] = lockoutUntil
            }
        }
        return Pair(attempts, lockoutUntil)
    }

    suspend fun resetFailedAttempts() {
        context.dataStore.edit {
            it[KEY_FAILED_ATTEMPTS] = 0
            it[KEY_LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BIOMETRIC_ENABLED] = enabled }
        recordActivityLog("BIOMETRIC", if (enabled) "Biometrik diaktifkan" else "Biometrik dinonaktifkan", "SECURITY")
    }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { it[KEY_SERVER_URL] = url.trim() }
        recordActivityLog("SERVER_URL", "URL server cloud diubah ke: ${url.trim()}", "CLOUD")
    }

    suspend fun saveAuthSession(token: String, username: String) {
        context.dataStore.edit {
            it[KEY_AUTH_TOKEN] = token
            it[KEY_AUTH_USERNAME] = username
        }
        recordActivityLog("LOGIN", "Berhasil login ke akun cloud: $username", "CLOUD")
    }

    suspend fun clearAuthSession() {
        context.dataStore.edit {
            it.remove(KEY_AUTH_TOKEN)
            it.remove(KEY_AUTH_USERNAME)
        }
        recordActivityLog("LOGOUT", "Keluar dari akun cloud", "CLOUD")
    }

    suspend fun saveAdminSession(token: String, username: String) {
        context.dataStore.edit {
            it[KEY_ADMIN_TOKEN] = token
            it[KEY_ADMIN_USERNAME] = username
        }
        recordActivityLog("ADMIN_LOGIN", "Administrator terotentikasi di Ruang Rahasia", "SECURITY")
    }

    suspend fun clearAdminSession() {
        context.dataStore.edit {
            it.remove(KEY_ADMIN_TOKEN)
            it.remove(KEY_ADMIN_USERNAME)
        }
        recordActivityLog("ADMIN_LOGOUT", "Administrator keluar dari sesi admin", "SECURITY")
    }

    suspend fun setDisplayAppName(name: String) {
        context.dataStore.edit { it[KEY_DISPLAY_APP_NAME] = name.trim() }
    }

    suspend fun setAppDescription(desc: String) {
        context.dataStore.edit { it[KEY_APP_DESCRIPTION] = desc.trim() }
    }

    suspend fun setAccentTheme(theme: String) {
        context.dataStore.edit { it[KEY_ACCENT_THEME] = theme.trim() }
    }

    suspend fun setOpeningMode(mode: String) {
        context.dataStore.edit { it[KEY_OPENING_MODE] = mode.trim() }
    }

    suspend fun toggleAppLock(packageName: String, shouldLock: Boolean) {
        context.dataStore.edit { prefs ->
            val current = (prefs[KEY_LOCKED_APPS_CSV] ?: "").split(",").filter { it.isNotBlank() }.toMutableSet()
            if (shouldLock) current.add(packageName) else current.remove(packageName)
            prefs[KEY_LOCKED_APPS_CSV] = current.joinToString(",")
        }
        recordActivityLog("APP_LOCK", "${if (shouldLock) "Mengunci" else "Membuka kunci"} aplikasi: $packageName", "APP_VAULT")
    }

    suspend fun toggleAppSilenced(packageName: String, shouldSilence: Boolean) {
        context.dataStore.edit { prefs ->
            val current = (prefs[KEY_SILENCED_APPS_CSV] ?: "").split(",").filter { it.isNotBlank() }.toMutableSet()
            if (shouldSilence) current.add(packageName) else current.remove(packageName)
            prefs[KEY_SILENCED_APPS_CSV] = current.joinToString(",")
        }
        recordActivityLog("APP_SILENCED", "${if (shouldSilence) "Membungkam notifikasi" else "Mengaktifkan notifikasi"} aplikasi: $packageName", "APP_VAULT")
    }

    suspend fun recordActivityLog(action: String, details: String, type: String = "SECURITY") {
        try {
            val prefs = context.dataStore.data.first()
            val currentJson = prefs[KEY_ACTIVITY_LOGS_JSON] ?: "[]"
            val arr = JSONArray(currentJson)
            val newObj = JSONObject().apply {
                put("id", UUID.randomUUID().toString())
                put("timestamp", System.currentTimeMillis())
                put("action", action)
                put("details", details)
                put("type", type)
            }
            // Simpan hingga 60 entri terbaru
            val newArr = JSONArray()
            newArr.put(newObj)
            for (i in 0 until minOf(59, arr.length())) {
                newArr.put(arr.getJSONObject(i))
            }
            context.dataStore.edit { it[KEY_ACTIVITY_LOGS_JSON] = newArr.toString() }
        } catch (e: Exception) {
            // Ignore log error
        }
    }

    suspend fun clearActivityLogs() {
        context.dataStore.edit { it[KEY_ACTIVITY_LOGS_JSON] = "[]" }
    }

    suspend fun saveHistoryJson(json: String) {
        context.dataStore.edit { it[KEY_HISTORY_JSON] = json }
    }
}
