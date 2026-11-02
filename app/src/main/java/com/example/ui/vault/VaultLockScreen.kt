package com.example.ui.vault

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.theme.DangerRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VaultLockScreen(
    userPrefs: UserPreferencesRepository,
    onUnlocked: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var remainingLockoutSeconds by remember { mutableIntStateOf(0) }

    val lockoutUntil by userPrefs.lockoutUntilFlow.collectAsState(initial = 0L)
    val isBiometricEnabled by userPrefs.biometricEnabledFlow.collectAsState(initial = false)

    val activity = context as? FragmentActivity

    fun triggerNativeBiometric() {
        if (activity != null && isBiometricEnabled && BiometricHelper.canAuthenticate(context)) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                title = "Buka Ruang Rahasia",
                subtitle = "Verifikasi sidik jari atau biometrik perangkat",
                negativeButtonText = "Gunakan PIN",
                onSuccess = {
                    scope.launch {
                        userPrefs.resetFailedAttempts()
                        userPrefs.recordActivityLog("BIOMETRIC_SUCCESS", "Berhasil membuka Ruang Rahasia via Biometrik", "SECURITY")
                        onUnlocked()
                    }
                },
                onError = { _, errString ->
                    // Fallback to PIN silently
                },
                onFailed = {
                    errorMessage = "Biometrik tidak cocok"
                }
            )
        }
    }

    // Auto-prompt biometric if enabled
    LaunchedEffect(isBiometricEnabled) {
        if (isBiometricEnabled && BiometricHelper.canAuthenticate(context)) {
            triggerNativeBiometric()
        }
    }

    // Handle lockout countdown timer
    LaunchedEffect(lockoutUntil) {
        val now = System.currentTimeMillis()
        if (lockoutUntil > now) {
            remainingLockoutSeconds = ((lockoutUntil - now) / 1000).toInt()
            while (remainingLockoutSeconds > 0) {
                delay(1000)
                remainingLockoutSeconds--
            }
            userPrefs.resetFailedAttempts()
        }
    }

    fun submitPin() {
        if (remainingLockoutSeconds > 0) return
        if (enteredPin.isEmpty()) {
            errorMessage = "Silakan masukkan PIN terlebih dahulu"
            return
        }

        scope.launch {
            val isValid = userPrefs.verifyPin(enteredPin)
            if (isValid) {
                userPrefs.resetFailedAttempts()
                onUnlocked()
            } else {
                val (attempts, lockout) = userPrefs.recordFailedAttempt()
                enteredPin = ""
                if (lockout > System.currentTimeMillis()) {
                    errorMessage = "Terlalu banyak percobaan salah! Terkunci 30 detik."
                } else {
                    errorMessage = "PIN Salah! Percobaan ke-$attempts (Default Awal: 1234)"
                }
            }
        }
    }

    fun handleDigit(digit: String) {
        if (remainingLockoutSeconds > 0) return
        if (enteredPin.length < 8) {
            enteredPin += digit
            errorMessage = null
        }
    }

    fun handleDelete() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Nav & Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali ke Kalkulator")
                    }
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Autentikasi Ruang Rahasia",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Masukkan PIN atau gunakan sensor biometrik",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "(PIN Default Awal: 1234)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PIN Display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (enteredPin.isEmpty()) {
                            Text(
                                text = "• • • •",
                                fontSize = 28.sp,
                                letterSpacing = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = enteredPin.map { "•" }.joinToString(" "),
                                fontSize = 32.sp,
                                letterSpacing = 8.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (remainingLockoutSeconds > 0) {
                    Text(
                        text = "Terkunci sementara: tunggu ${remainingLockoutSeconds} detik",
                        color = DangerRed,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = DangerRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Keypad
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val digits = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )

                for (row in digits) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (digit in row) {
                            KeypadButton(
                                text = digit,
                                onClick = { handleDigit(digit) },
                                enabled = remainingLockoutSeconds == 0
                            )
                        }
                    }
                }

                // Bottom row: Biometric, 0, Backspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                if (isBiometricEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable {
                                if (isBiometricEnabled) {
                                    triggerNativeBiometric()
                                } else {
                                    Toast.makeText(context, "Biometrik belum diaktifkan di Ruang Rahasia > Pengaturan", Toast.LENGTH_SHORT).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = "Autentikasi Biometrik",
                            tint = if (isBiometricEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    KeypadButton(
                        text = "0",
                        onClick = { handleDigit("0") },
                        enabled = remainingLockoutSeconds == 0
                    )

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable { handleDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Backspace,
                            contentDescription = "Hapus",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Confirmation button
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { submitPin() },
                    enabled = enteredPin.isNotEmpty() && remainingLockoutSeconds == 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("confirm_password_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "KONFIRMASI PASSWORD & BUKA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (enteredPin.isNotEmpty()) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Batal & Kembali ke Kalkulator")
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
