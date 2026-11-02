package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.data.repository.CloudVaultClient
import com.example.data.repository.UserPreferencesRepository
import com.example.data.repository.VaultRepository
import com.example.ui.calculator.SubnetCalculatorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.vault.OpeningScreen
import com.example.ui.vault.VaultLockScreen
import com.example.ui.vault.VaultMainScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class ScreenState {
    OPENING,
    CALCULATOR,
    VAULT_LOCK,
    VAULT_MAIN
}

class MainActivity : FragmentActivity() {

    private lateinit var userPrefs: UserPreferencesRepository
    private lateinit var vaultRepo: VaultRepository
    private val cloudClient = CloudVaultClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        userPrefs = UserPreferencesRepository(applicationContext)
        vaultRepo = VaultRepository(applicationContext)

        // Telemetri ringan & sinkronisasi konfigurasi remote saat startup
        lifecycleScopeLaunch()

        setContent {
            val accentTheme by userPrefs.accentThemeFlow.collectAsState(initial = UserPreferencesRepository.DEFAULT_ACCENT_THEME)

            MyApplicationTheme(themePalette = accentTheme) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                ) {
                    AppNavigation(
                        userPrefs = userPrefs,
                        vaultRepo = vaultRepo,
                        cloudClient = cloudClient
                    )
                }
            }
        }
    }

    private fun lifecycleScopeLaunch() {
        // Coroutine background untuk fetching remote config & heartbeat
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val serverUrl = userPrefs.serverUrlFlow.first()
                val authToken = userPrefs.authTokenFlow.first()

                // Fetch Remote Config dari backend server
                val remoteRes = cloudClient.fetchRemoteConfig(serverUrl)
                if (remoteRes.isSuccess) {
                    val remoteCfg = remoteRes.getOrThrow()
                    // Sinkronkan pengumuman remote jika ada
                    if (remoteCfg.announcement.isNotBlank()) {
                        // User can see announcement in Ruang Rahasia
                    }
                }

                // Heartbeat pengguna terdaftar jika login
                if (!authToken.isNullOrEmpty()) {
                    val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
                    cloudClient.sendHeartbeat(serverUrl, authToken, deviceModel, "2.0.0")
                }
            } catch (_: Exception) {
                // Offline fallback - aplikasi tetap berfungsi penuh secara lokal
            }
        }
    }
}

@Composable
fun AppNavigation(
    userPrefs: UserPreferencesRepository,
    vaultRepo: VaultRepository,
    cloudClient: CloudVaultClient
) {
    val openingMode by userPrefs.openingModeFlow.collectAsState(initial = UserPreferencesRepository.DEFAULT_OPENING_MODE)
    val displayAppName by userPrefs.displayAppNameFlow.collectAsState(initial = UserPreferencesRepository.DEFAULT_DISPLAY_APP_NAME)
    val appDescription by userPrefs.appDescriptionFlow.collectAsState(initial = UserPreferencesRepository.DEFAULT_APP_DESCRIPTION)

    var hasShownOpening by remember { mutableStateOf(false) }
    var screenState by remember {
        mutableStateOf(
            if (openingMode != "NONE" && !hasShownOpening) ScreenState.OPENING
            else ScreenState.CALCULATOR
        )
    }

    // When inside the Vault, pressing system back locks and returns to Calculator
    BackHandler(enabled = screenState == ScreenState.VAULT_MAIN || screenState == ScreenState.VAULT_LOCK) {
        screenState = ScreenState.CALCULATOR
    }

    Crossfade(targetState = screenState, label = "ScreenTransition") { currentScreen ->
        when (currentScreen) {
            ScreenState.OPENING -> {
                OpeningScreen(
                    appName = displayAppName,
                    appDescription = appDescription,
                    mode = openingMode,
                    onFinish = {
                        hasShownOpening = true
                        screenState = ScreenState.CALCULATOR
                    }
                )
            }
            ScreenState.CALCULATOR -> {
                SubnetCalculatorScreen(
                    userPrefs = userPrefs,
                    onOpenVaultRequested = {
                        screenState = ScreenState.VAULT_LOCK
                    }
                )
            }
            ScreenState.VAULT_LOCK -> {
                VaultLockScreen(
                    userPrefs = userPrefs,
                    onUnlocked = {
                        screenState = ScreenState.VAULT_MAIN
                    },
                    onCancel = {
                        screenState = ScreenState.CALCULATOR
                    }
                )
            }
            ScreenState.VAULT_MAIN -> {
                VaultMainScreen(
                    vaultRepo = vaultRepo,
                    userPrefs = userPrefs,
                    cloudClient = cloudClient,
                    onLockVault = {
                        screenState = ScreenState.CALCULATOR
                    }
                )
            }
        }
    }
}
