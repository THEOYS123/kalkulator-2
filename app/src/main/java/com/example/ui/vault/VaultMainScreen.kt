package com.example.ui.vault

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ActivityLogItem
import com.example.data.model.AdminStats
import com.example.data.model.AdminUserItem
import com.example.data.model.AppVaultItem
import com.example.data.model.CloudFileItem
import com.example.data.model.CloudQuotaInfo
import com.example.data.model.RemoteAppConfig
import com.example.data.model.VaultCategory
import com.example.data.model.VaultItem
import com.example.data.repository.AppVaultManager
import com.example.data.repository.CloudVaultClient
import com.example.data.repository.UserPreferencesRepository
import com.example.data.repository.VaultRepository
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultMainScreen(
    vaultRepo: VaultRepository,
    userPrefs: UserPreferencesRepository,
    cloudClient: CloudVaultClient,
    onLockVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appVaultManager = remember { AppVaultManager(context) }

    val vaultItems by vaultRepo.items.collectAsState()
    val authToken by userPrefs.authTokenFlow.collectAsState(initial = null)
    val authUsername by userPrefs.authUsernameFlow.collectAsState(initial = null)
    val adminToken by userPrefs.adminTokenFlow.collectAsState(initial = null)
    val adminUsername by userPrefs.adminUsernameFlow.collectAsState(initial = null)
    val serverUrl by userPrefs.serverUrlFlow.collectAsState(initial = UserPreferencesRepository.DEFAULT_SERVER_URL)
    val secretIpCode by userPrefs.secretIpCodeFlow.collectAsState(initial = UserPreferencesRepository.DEFAULT_SECRET_IP)
    val isBiometricEnabled by userPrefs.biometricEnabledFlow.collectAsState(initial = false)
    val displayAppName by userPrefs.displayAppNameFlow.collectAsState(initial = UserPreferencesRepository.DEFAULT_DISPLAY_APP_NAME)
    val appDescription by userPrefs.appDescriptionFlow.collectAsState(initial = UserPreferencesRepository.DEFAULT_APP_DESCRIPTION)
    val accentTheme by userPrefs.accentThemeFlow.collectAsState(initial = UserPreferencesRepository.DEFAULT_ACCENT_THEME)
    val openingMode by userPrefs.openingModeFlow.collectAsState(initial = UserPreferencesRepository.DEFAULT_OPENING_MODE)
    val lockedApps by userPrefs.lockedAppsFlow.collectAsState(initial = emptySet())
    val silencedApps by userPrefs.silencedAppsFlow.collectAsState(initial = emptySet())
    val activityLogs by userPrefs.activityLogsFlow.collectAsState(initial = emptyList())

    // Tabs: 0: Private Vault, 1: App Vault, 2: Cloud Sync, 3: Personalisasi, 4: Keamanan, 5: Aktivitas & Bantuan, 6: Panel Admin
    var selectedSection by remember { mutableIntStateOf(0) }
    val sectionTitles = listOf(
        "Private Vault",
        "App Vault",
        "Cloud Storage",
        "Personalisasi",
        "Keamanan",
        "Aktivitas",
        "Panel Admin"
    )

    // Private Vault state
    var selectedCategory by remember { mutableStateOf(VaultCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val selectedItemIds = remember { mutableStateListOf<String>() }
    val isSelectionMode = selectedItemIds.isNotEmpty()

    // Dialogs state
    var itemToPreview by remember { mutableStateOf<VaultItem?>(null) }
    var itemToRename by remember { mutableStateOf<VaultItem?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isUploadingToCloud by remember { mutableStateOf(false) }
    var uploadStatusMessage by remember { mutableStateOf<String?>(null) }

    // Quota info state
    var quotaInfo by remember { mutableStateOf<CloudQuotaInfo?>(null) }

    fun refreshQuota() {
        val token = authToken
        if (!token.isNullOrEmpty()) {
            scope.launch {
                val res = cloudClient.fetchQuota(serverUrl, token)
                if (res.isSuccess) {
                    quotaInfo = res.getOrNull()
                }
            }
        }
    }

    LaunchedEffect(authToken) {
        refreshQuota()
    }

    // SAF Import Launcher
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                var successCount = 0
                uris.forEach { uri ->
                    val result = vaultRepo.importFile(uri)
                    if (result.isSuccess) successCount++
                }
                userPrefs.recordActivityLog("FILE_IMPORT", "Mengimpor $successCount file ke brankas privat", "FILE")
                Toast.makeText(context, "$successCount file berhasil dienkripsi ke brankas!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Export single file launcher
    var pendingExportItem by remember { mutableStateOf<VaultItem?>(null) }
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val item = pendingExportItem
        if (uri != null && item != null) {
            scope.launch {
                val res = vaultRepo.exportFileToUri(item, uri)
                if (res.isSuccess) {
                    userPrefs.recordActivityLog("FILE_RESTORE", "Memulihkan '${item.name}' ke perangkat", "FILE")
                    Toast.makeText(context, "File '${item.name}' berhasil dipulihkan ke perangkat!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Gagal memulihkan file: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
                pendingExportItem = null
            }
        }
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    fun syncItemToCloud(item: VaultItem) {
        val token = authToken
        if (token.isNullOrEmpty()) {
            Toast.makeText(context, "Silakan login ke Akun Cloud terlebih dahulu di tab Cloud Storage", Toast.LENGTH_LONG).show()
            selectedSection = 2
            return
        }

        scope.launch {
            isUploadingToCloud = true
            uploadStatusMessage = "Mengunggah '${item.name}' ke cloud non-SQL..."
            try {
                val tempFile = vaultRepo.getDecryptedTempPreview(item)
                val uploadRes = cloudClient.uploadVaultItem(serverUrl, token, item, tempFile)
                tempFile.delete()

                if (uploadRes.isSuccess) {
                    val cloudId = uploadRes.getOrThrow()
                    vaultRepo.markCloudSynced(item.id, cloudId)
                    userPrefs.recordActivityLog("CLOUD_UPLOAD", "Mengunggah '${item.name}' ke cloud server terverifikasi", "CLOUD")
                    refreshQuota()
                    Toast.makeText(context, "Berhasil diunggah dan diverifikasi di Cloud!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Upload gagal: ${uploadRes.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isUploadingToCloud = false
                uploadStatusMessage = null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Cari file atau aplikasi...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (isSelectionMode) {
                        Text("${selectedItemIds.size} file dipilih", fontWeight = FontWeight.Bold)
                    } else {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Ruang Rahasia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text("AES-256", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = "Pusat Brankas & Pengelolaan Terpadu",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectedItemIds.clear() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Batal Pilih")
                        }
                    } else if (isSearchActive) {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Tutup Pencarian")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus Terpilih", tint = DangerRed)
                        }
                    } else {
                        IconButton(onClick = { isSearchActive = !isSearchActive }) {
                            Icon(Icons.Default.Search, contentDescription = "Cari")
                        }
                        IconButton(
                            onClick = onLockVault,
                            modifier = Modifier.testTag("lock_vault_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Kunci Brankas", tint = DangerRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (selectedSection == 0) {
                FloatingActionButton(
                    onClick = { importFileLauncher.launch(arrayOf("*/*")) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("import_file_fab")
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Impor File", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Top Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedSection,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                sectionTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedSection == index,
                        onClick = { selectedSection = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedSection == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (isUploadingToCloud) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                uploadStatusMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Section Router
            when (selectedSection) {
                0 -> PrivateVaultSection(
                    vaultRepo = vaultRepo,
                    vaultItems = vaultItems,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it },
                    searchQuery = searchQuery,
                    selectedItemIds = selectedItemIds,
                    isSelectionMode = isSelectionMode,
                    onPreviewItem = { itemToPreview = it },
                    onRenameItem = { itemToRename = it },
                    onExportItem = {
                        pendingExportItem = it
                        exportFileLauncher.launch(it.name)
                    },
                    onSyncItem = { syncItemToCloud(it) },
                    onDeleteItem = { item ->
                        scope.launch {
                            vaultRepo.deleteItem(item)
                            userPrefs.recordActivityLog("FILE_DELETE", "Menghapus file '${item.name}' dari brankas", "FILE")
                            Toast.makeText(context, "File '${item.name}' dihapus", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                1 -> AppVaultSection(
                    appVaultManager = appVaultManager,
                    userPrefs = userPrefs,
                    lockedApps = lockedApps,
                    silencedApps = silencedApps,
                    searchQuery = searchQuery
                )
                2 -> CloudSyncSection(
                    userPrefs = userPrefs,
                    cloudClient = cloudClient,
                    vaultRepo = vaultRepo,
                    serverUrl = serverUrl,
                    authToken = authToken,
                    authUsername = authUsername,
                    quotaInfo = quotaInfo,
                    onRefreshQuota = { refreshQuota() }
                )
                3 -> PersonalizationSection(
                    userPrefs = userPrefs,
                    cloudClient = cloudClient,
                    serverUrl = serverUrl,
                    displayAppName = displayAppName,
                    appDescription = appDescription,
                    accentTheme = accentTheme,
                    openingMode = openingMode
                )
                4 -> SecuritySection(
                    userPrefs = userPrefs,
                    secretIpCode = secretIpCode,
                    isBiometricEnabled = isBiometricEnabled
                )
                5 -> ActivitySection(
                    activityLogs = activityLogs,
                    onClearLogs = { scope.launch { userPrefs.clearActivityLogs() } }
                )
                6 -> AdminPanelSection(
                    userPrefs = userPrefs,
                    cloudClient = cloudClient,
                    serverUrl = serverUrl,
                    adminToken = adminToken,
                    adminUsername = adminUsername
                )
            }
        }
    }

    // Dialog: Delete confirmation
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Konfirmasi Hapus Terpilih") },
            text = {
                Text("Hapus ${selectedItemIds.size} file terpilih dari brankas privat secara permanen? Tindakan ini tidak dapat dibatalkan kecuali Anda memiliki salinan di Cloud.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val itemsToDelete = vaultItems.filter { selectedItemIds.contains(it.id) }
                            itemsToDelete.forEach { vaultRepo.deleteItem(it) }
                            userPrefs.recordActivityLog("FILE_BATCH_DELETE", "Menghapus ${itemsToDelete.size} file dari brankas", "FILE")
                            selectedItemIds.clear()
                            showDeleteConfirmDialog = false
                            Toast.makeText(context, "${itemsToDelete.size} file dihapus", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Hapus Permanen")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog: Rename
    if (itemToRename != null) {
        val target = itemToRename!!
        var newNameInput by remember { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { itemToRename = null },
            title = { Text("Ubah Nama File") },
            text = {
                OutlinedTextField(
                    value = newNameInput,
                    onValueChange = { newNameInput = it },
                    label = { Text("Nama File") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNameInput.isNotBlank()) {
                            scope.launch {
                                vaultRepo.renameItem(target, newNameInput.trim())
                                userPrefs.recordActivityLog("FILE_RENAME", "Mengubah nama file dari '${target.name}' ke '${newNameInput.trim()}'", "FILE")
                                itemToRename = null
                            }
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemToRename = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog: File Preview
    if (itemToPreview != null) {
        VaultFileViewerDialog(
            item = itemToPreview!!,
            vaultRepo = vaultRepo,
            onDismiss = { itemToPreview = null },
            onExport = {
                pendingExportItem = itemToPreview
                exportFileLauncher.launch(itemToPreview!!.name)
            },
            onSyncCloud = { syncItemToCloud(itemToPreview!!) }
        )
    }
}

// =========================================================================
// 0. PRIVATE VAULT TAB
// =========================================================================
@Composable
fun PrivateVaultSection(
    vaultRepo: VaultRepository,
    vaultItems: List<VaultItem>,
    selectedCategory: VaultCategory,
    onSelectCategory: (VaultCategory) -> Unit,
    searchQuery: String,
    selectedItemIds: List<String>,
    isSelectionMode: Boolean,
    onPreviewItem: (VaultItem) -> Unit,
    onRenameItem: (VaultItem) -> Unit,
    onExportItem: (VaultItem) -> Unit,
    onSyncItem: (VaultItem) -> Unit,
    onDeleteItem: (VaultItem) -> Unit
) {
    val filteredItems = vaultItems.filter { item ->
        val matchesCategory = selectedCategory == VaultCategory.ALL || item.category == selectedCategory
        val matchesSearch = searchQuery.isEmpty() || item.name.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Categories Chips
        ScrollableTabRow(
            selectedTabIndex = VaultCategory.values().indexOf(selectedCategory),
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            VaultCategory.values().forEach { cat ->
                val count = if (cat == VaultCategory.ALL) vaultItems.size else vaultItems.count { it.category == cat }
                Tab(
                    selected = selectedCategory == cat,
                    onClick = { onSelectCategory(cat) },
                    text = {
                        Text(
                            text = "${cat.displayName} ($count)",
                            fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Tidak ada file yang cocok dengan pencarian" else "Belum ada file di kategori ${selectedCategory.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tekan tombol '+ Impor File' di sudut bawah untuk mengamankan foto, video, ZIP, atau dokumen dari perangkat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else if (selectedCategory == VaultCategory.PHOTOS) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    VaultPhotoGridItem(
                        item = item,
                        vaultRepo = vaultRepo,
                        isSelected = selectedItemIds.contains(item.id),
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                val list = selectedItemIds as MutableList<String>
                                if (list.contains(item.id)) list.remove(item.id) else list.add(item.id)
                            } else {
                                onPreviewItem(item)
                            }
                        },
                        onLongClick = {
                            val list = selectedItemIds as MutableList<String>
                            if (!list.contains(item.id)) list.add(item.id)
                        }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    VaultFileListCard(
                        item = item,
                        isSelected = selectedItemIds.contains(item.id),
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                val list = selectedItemIds as MutableList<String>
                                if (list.contains(item.id)) list.remove(item.id) else list.add(item.id)
                            } else {
                                onPreviewItem(item)
                            }
                        },
                        onLongClick = {
                            val list = selectedItemIds as MutableList<String>
                            if (!list.contains(item.id)) list.add(item.id)
                        },
                        onExport = { onExportItem(item) },
                        onSyncCloud = { onSyncItem(item) },
                        onRename = { onRenameItem(item) },
                        onDelete = { onDeleteItem(item) }
                    )
                }
            }
        }
    }
}

// =========================================================================
// 1. APP VAULT TAB
// =========================================================================
@Composable
fun AppVaultSection(
    appVaultManager: AppVaultManager,
    userPrefs: UserPreferencesRepository,
    lockedApps: Set<String>,
    silencedApps: Set<String>,
    searchQuery: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var appsList by remember { mutableStateOf<List<AppVaultItem>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }
    var showSecurityExplainer by remember { mutableStateOf(false) }

    LaunchedEffect(lockedApps, silencedApps) {
        isLoadingApps = true
        appsList = appVaultManager.getInstalledLauncherApps(lockedApps, silencedApps)
        isLoadingApps = false
    }

    val filteredApps = appsList.filter {
        searchQuery.isEmpty() || it.label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // App Vault Header & Transparent Security Info Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("App Vault (Pengelolaan Aplikasi)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        IconButton(onClick = { showSecurityExplainer = !showSecurityExplainer }) {
                            Icon(Icons.Default.Info, contentDescription = "Panduan Keamanan Android", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Text(
                        text = "Kelola proteksi peluncuran aplikasi terpasang di perangkat Anda. Beri tanda kunci untuk mewajibkan autentikasi PIN sebelum membuka, atau atur kebijakan senyap notifikasi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AnimatedVisibility(visible = showSecurityExplainer) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Informasi Arsitektur Keamanan Android Asli:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Text("• App Lock: Memberikan perlindungan gerbang sebelum aplikasi diluncurkan dari Ruang Rahasia.", style = MaterialTheme.typography.bodySmall)
                                Text("• Batasan Sistem: Android tidak mengizinkan aplikasi non-sistem menyembunyikan aplikasi lain dari Pengaturan Sistem (Settings > Apps) tanpa akses root / profil MDM.", style = MaterialTheme.typography.bodySmall)
                                Text("• Android 15+ Private Space: Diperkenalkan di Android 15 untuk mengisolasi aplikasi di profil terpisah secara native.", style = MaterialTheme.typography.bodySmall)
                                Text("• Pembungkaman Notifikasi: Anda dapat langsung menuju ke kanal notifikasi sistem aplikasi terkait melalui tombol lonceng.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        if (isLoadingApps) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (filteredApps.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("Tidak ada aplikasi launcher ditemukan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(filteredApps, key = { it.packageName }) { appItem ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (appItem.isLocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (appItem.isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon App
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = appItem.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = appItem.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Notification Silent Toggle
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val newSilenced = !appItem.isSilenced
                                    userPrefs.toggleAppSilenced(appItem.packageName, newSilenced)
                                    if (newSilenced) {
                                        appVaultManager.openAppNotificationSettings(appItem.packageName)
                                        Toast.makeText(context, "Membuka setelan notifikasi sistem untuk ${appItem.label}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (appItem.isSilenced) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                                contentDescription = "Pengaturan Notifikasi",
                                tint = if (appItem.isSilenced) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Lock Toggle
                        IconButton(
                            onClick = {
                                scope.launch {
                                    userPrefs.toggleAppLock(appItem.packageName, !appItem.isLocked)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (appItem.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Kunci Aplikasi",
                                tint = if (appItem.isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Launch App Button
                        Button(
                            onClick = {
                                val launched = appVaultManager.launchApp(appItem.packageName)
                                if (!launched) {
                                    Toast.makeText(context, "Tidak dapat membuka aplikasi ini secara langsung", Toast.LENGTH_SHORT).show()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Buka", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 2. CLOUD SYNC & STORAGE TAB
// =========================================================================
@Composable
fun CloudSyncSection(
    userPrefs: UserPreferencesRepository,
    cloudClient: CloudVaultClient,
    vaultRepo: VaultRepository,
    serverUrl: String,
    authToken: String?,
    authUsername: String?,
    quotaInfo: CloudQuotaInfo?,
    onRefreshQuota: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var cloudFiles by remember { mutableStateOf<List<CloudFileItem>>(emptyList()) }
    var isLoadingCloudFiles by remember { mutableStateOf(false) }
    var serverStatusPing by remember { mutableStateOf<String?>(null) }
    var isPinging by remember { mutableStateOf(false) }

    fun loadCloudFiles() {
        if (!authToken.isNullOrEmpty()) {
            scope.launch {
                isLoadingCloudFiles = true
                val res = cloudClient.listCloudFiles(serverUrl, authToken)
                if (res.isSuccess) {
                    cloudFiles = res.getOrThrow()
                } else {
                    Toast.makeText(context, "Gagal memuat file cloud: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
                isLoadingCloudFiles = false
            }
        }
    }

    LaunchedEffect(authToken) {
        if (!authToken.isNullOrEmpty()) {
            loadCloudFiles()
        }
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cloud Quota & Connection Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Penyimpanan Cloud Non-SQL", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        IconButton(onClick = {
                            onRefreshQuota()
                            loadCloudFiles()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Segarkan")
                        }
                    }

                    if (authToken != null) {
                        Text(text = "Akun Aktif: $authUsername", fontWeight = FontWeight.Bold, color = SuccessGreen)

                        if (quotaInfo != null) {
                            val used = quotaInfo.usedBytes
                            val total = maxOf(1L, quotaInfo.totalBytes)
                            val pct = (used.toFloat() / total).coerceIn(0f, 1f)

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Kapasitas Terpakai:", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "${formatBytes(used)} / ${formatBytes(total)} (${String.format(Locale.US, "%.1f%%", pct * 100)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LinearProgressIndicator(progress = pct, modifier = Modifier.fillMaxWidth())
                                Text(
                                    text = "Total File Tersimpan: ${quotaInfo.fileCount} file",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        isPinging = true
                                        val res = cloudClient.fetchRemoteConfig(serverUrl)
                                        isPinging = false
                                        if (res.isSuccess) {
                                            serverStatusPing = "Koneksi Berhasil! Server Online (Versi ${res.getOrNull()?.configVersion})"
                                        } else {
                                            serverStatusPing = "Koneksi Gagal: ${res.exceptionOrNull()?.message}"
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isPinging) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                else Text("Tes Latensi Server", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        userPrefs.clearAuthSession()
                                        Toast.makeText(context, "Berhasil Keluar dari Akun Cloud", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Keluar (Logout)", fontSize = 12.sp)
                            }
                        }

                        serverStatusPing?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (it.contains("Berhasil")) SuccessGreen else DangerRed
                            )
                        }
                    } else {
                        Text(
                            text = "Anda belum terhubung ke Akun Cloud. Masuk atau daftarkan akun di bawah ini untuk mengaktifkan backup dan sinkronisasi lintas perangkat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Login / Register Form if not logged in
        if (authToken == null) {
            item {
                CloudAuthCard(
                    userPrefs = userPrefs,
                    cloudClient = cloudClient,
                    serverUrl = serverUrl,
                    onAuthSuccess = { onRefreshQuota() }
                )
            }
        }

        // List of files stored in cloud
        if (authToken != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Berkas di Server Cloud (${cloudFiles.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = { loadCloudFiles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Muat Ulang", fontSize = 12.sp)
                    }
                }
            }

            if (isLoadingCloudFiles) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (cloudFiles.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("Belum ada file di cloud. Unggah file dari Private Vault.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            } else {
                items(cloudFiles, key = { it.id }) { cFile ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cFile.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${formatBytes(cFile.size)} • ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(cFile.uploadedAt * 1000L))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Download / Restore to vault
                            IconButton(onClick = {
                                scope.launch {
                                    val tempFile = File(context.cacheDir, cFile.name)
                                    val dlRes = cloudClient.downloadCloudFile(serverUrl, authToken, cFile.id, tempFile)
                                    if (dlRes.isSuccess) {
                                        val importRes = vaultRepo.importFromLocalFile(tempFile, cFile.name, cFile.mimeType)
                                        tempFile.delete()
                                        if (importRes.isSuccess) {
                                            val importedItem = importRes.getOrThrow()
                                            vaultRepo.markCloudSynced(importedItem.id, cFile.id)
                                            userPrefs.recordActivityLog("CLOUD_DOWNLOAD", "Mengunduh file '${cFile.name}' dari cloud ke brankas lokal", "CLOUD")
                                            Toast.makeText(context, "File '${cFile.name}' berhasil dipulihkan ke brankas lokal!", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Gagal mengunduh: ${dlRes.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Icon(Icons.Default.CloudDownload, contentDescription = "Pulihkan ke Brankas", tint = MaterialTheme.colorScheme.primary)
                            }

                            // Delete from Cloud
                            IconButton(onClick = {
                                scope.launch {
                                    val delRes = cloudClient.deleteCloudFile(serverUrl, authToken, cFile.id)
                                    if (delRes.isSuccess) {
                                        userPrefs.recordActivityLog("CLOUD_DELETE", "Menghapus '${cFile.name}' dari cloud server", "CLOUD")
                                        loadCloudFiles()
                                        onRefreshQuota()
                                        Toast.makeText(context, "File berhasil dihapus dari cloud", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Gagal menghapus: ${delRes.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus dari Cloud", tint = DangerRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CloudAuthCard(
    userPrefs: UserPreferencesRepository,
    cloudClient: CloudVaultClient,
    serverUrl: String,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isRegisterMode by remember { mutableStateOf(false) }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var serverUrlInput by remember(serverUrl) { mutableStateOf(serverUrl) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = if (isRegisterMode) "Daftar Akun Cloud Baru" else "Masuk Akun Cloud",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = serverUrlInput,
                onValueChange = { serverUrlInput = it },
                label = { Text("URL Server API PHP") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("Password (Min 6 Karakter)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Text(errorMessage!!, color = DangerRed, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    if (usernameInput.isBlank() || passwordInput.isBlank()) {
                        errorMessage = "Username dan password tidak boleh kosong"
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        userPrefs.setServerUrl(serverUrlInput)
                        if (isRegisterMode) {
                            val res = cloudClient.register(serverUrlInput, usernameInput, passwordInput)
                            if (res.isSuccess) {
                                Toast.makeText(context, "Pendaftaran berhasil! Silakan login.", Toast.LENGTH_SHORT).show()
                                isRegisterMode = false
                            } else {
                                errorMessage = res.exceptionOrNull()?.message
                            }
                        } else {
                            val res = cloudClient.login(serverUrlInput, usernameInput, passwordInput)
                            if (res.isSuccess) {
                                val (token, _) = res.getOrThrow()
                                userPrefs.saveAuthSession(token, usernameInput)
                                onAuthSuccess()
                                Toast.makeText(context, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                            } else {
                                errorMessage = res.exceptionOrNull()?.message
                            }
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                else Text(if (isRegisterMode) "Daftar Akun Sekarang" else "Masuk Akun Cloud")
            }

            TextButton(
                onClick = {
                    isRegisterMode = !isRegisterMode
                    errorMessage = null
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(if (isRegisterMode) "Sudah punya akun? Masuk di sini" else "Belum punya akun? Buat akun baru")
            }
        }
    }
}

// =========================================================================
// 3. PERSONALIZATION & BRANDING TAB
// =========================================================================
@Composable
fun PersonalizationSection(
    userPrefs: UserPreferencesRepository,
    cloudClient: CloudVaultClient,
    serverUrl: String,
    displayAppName: String,
    appDescription: String,
    accentTheme: String,
    openingMode: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nameInput by remember(displayAppName) { mutableStateOf(displayAppName) }
    var descInput by remember(appDescription) { mutableStateOf(appDescription) }

    val themes = listOf(
        Pair("CYAN", "Cyber Cyan"),
        Pair("EMERALD", "Emerald Green"),
        Pair("GOLD", "Amber Gold"),
        Pair("INDIGO", "Deep Indigo")
    )

    val openingModes = listOf(
        Pair("NONE", "Tanpa Opening (Langsung ke Kalkulator)"),
        Pair("BANNER_IMAGE", "Banner Gambar Sambutan"),
        Pair("ANIMATION", "Animasi Greeting Modern")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tema & Warna Aksen Aplikasi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }

                    Text(
                        text = "Pilih palet warna yang akan digunakan di seluruh antarmuka aplikasi Android:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        themes.forEach { (code, label) ->
                            FilterChip(
                                selected = accentTheme.equals(code, ignoreCase = true),
                                onClick = {
                                    scope.launch {
                                        userPrefs.setAccentTheme(code)
                                        Toast.makeText(context, "Tema diubah ke $label", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pengaturan Opening (Layar Awal)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }

                    Text(
                        text = "Tentukan tampilan saat aplikasi pertama kali dibuka:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    openingModes.forEach { (modeCode, modeLabel) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (openingMode == modeCode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                                .clickable {
                                    scope.launch {
                                        userPrefs.setOpeningMode(modeCode)
                                        Toast.makeText(context, "Opening diatur: $modeLabel", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (openingMode == modeCode) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            } else {
                                Box(modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(modeLabel, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Judul & Deskripsi Tampilan Internal", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nama Tampilan Aplikasi di UI") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Deskripsi Aplikasi") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                userPrefs.setDisplayAppName(nameInput)
                                userPrefs.setAppDescription(descInput)
                                Toast.makeText(context, "Nama dan deskripsi tampilan disimpan", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simpan Perubahan Tampilan")
                    }
                }
            }
        }
    }
}

// =========================================================================
// 4. SECURITY & PIN TAB
// =========================================================================
@Composable
fun SecuritySection(
    userPrefs: UserPreferencesRepository,
    secretIpCode: String,
    isBiometricEnabled: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var pinFeedback by remember { mutableStateOf<String?>(null) }
    var secretIpInput by remember(secretIpCode) { mutableStateOf(secretIpCode) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Change PIN
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ubah PIN Ruang Rahasia", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }

                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = { currentPinInput = it },
                        label = { Text("PIN Lama") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { newPinInput = it },
                        label = { Text("PIN Baru (4-8 Digit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    pinFeedback?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = if (it.contains("berhasil")) SuccessGreen else DangerRed)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val validOld = userPrefs.verifyPin(currentPinInput)
                                if (!validOld) {
                                    pinFeedback = "PIN lama tidak sesuai!"
                                } else if (newPinInput.length < 4) {
                                    pinFeedback = "PIN baru minimal 4 digit!"
                                } else {
                                    userPrefs.setPin(newPinInput)
                                    pinFeedback = "PIN berhasil diperbarui!"
                                    currentPinInput = ""
                                    newPinInput = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simpan PIN Baru")
                    }
                }
            }
        }

        // Biometrics Switch
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buka dengan Biometrik", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = {
                                scope.launch {
                                    userPrefs.setBiometricEnabled(it)
                                    Toast.makeText(context, if (it) "Biometrik diaktifkan" else "Biometrik dinonaktifkan", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    Text(
                        text = "Menggunakan Android BiometricPrompt asli untuk verifikasi sidik jari atau pemindai wajah yang terdaftar di perangkat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Secret Trigger IP
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kode IP Pemicu Rahasia", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }

                    Text(
                        text = "Ketik alamat IP ini pada kolom input kalkulator utama, lalu tekan 'Hitung Subnet' untuk membuka Ruang Rahasia secara tersembunyi tanpa tombol rahasia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = secretIpInput,
                        onValueChange = { secretIpInput = it },
                        label = { Text("Alamat IP Pemicu") },
                        placeholder = { Text("192.168.99.99") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (secretIpInput.isNotBlank()) {
                                scope.launch {
                                    userPrefs.setSecretIpCode(secretIpInput)
                                    Toast.makeText(context, "Kode IP rahasia disimpan: $secretIpInput", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simpan Kode IP Pemicu")
                    }
                }
            }
        }
    }
}

// =========================================================================
// 5. ACTIVITY LOGS & DIAGNOSTICS TAB
// =========================================================================
@Composable
fun ActivitySection(
    activityLogs: List<ActivityLogItem>,
    onClearLogs: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Riwayat Aktivitas Ruang Rahasia (${activityLogs.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                if (activityLogs.isNotEmpty()) {
                    TextButton(onClick = onClearLogs) {
                        Text("Bersihkan Log", color = DangerRed)
                    }
                }
            }
        }

        if (activityLogs.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("Belum ada aktivitas tercatat", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(activityLogs, key = { it.id }) { logItem ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (logItem.type) {
                            "SECURITY" -> Icons.Default.Security
                            "FILE" -> Icons.Default.Description
                            "CLOUD" -> Icons.Default.Cloud
                            "APP_VAULT" -> Icons.Default.Apps
                            else -> Icons.Default.History
                        }
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(logItem.action, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(dateFormat.format(Date(logItem.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(logItem.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 6. ADMINISTRATOR PANEL TAB (TERAUTENTIKASI BACKEND)
// =========================================================================
@Composable
fun AdminPanelSection(
    userPrefs: UserPreferencesRepository,
    cloudClient: CloudVaultClient,
    serverUrl: String,
    adminToken: String?,
    adminUsername: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var adminUserInput by remember { mutableStateOf("admin") }
    var adminPassInput by remember { mutableStateOf("") }
    var isLoggingIn by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

    var adminStats by remember { mutableStateOf<AdminStats?>(null) }
    var auditLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingStats by remember { mutableStateOf(false) }

    // Remote config form
    var remoteAppName by remember { mutableStateOf("") }
    var remoteAnnouncement by remember { mutableStateOf("") }
    var remoteMaintenance by remember { mutableStateOf(false) }
    var remoteMaxUpload by remember { mutableIntStateOf(50) }
    var remoteDefaultQuota by remember { mutableIntStateOf(1024) }
    var isSavingConfig by remember { mutableStateOf(false) }

    // Telegram bot form
    var tgBotToken by remember { mutableStateOf("") }
    var tgChatId by remember { mutableStateOf("") }
    var tgApiBase by remember { mutableStateOf("https://api.telegram.org") }
    var tgEnable2fa by remember { mutableStateOf(false) }

    fun loadAdminData() {
        if (!adminToken.isNullOrEmpty()) {
            scope.launch {
                isLoadingStats = true
                val statsRes = cloudClient.adminFetchStats(serverUrl, adminToken)
                if (statsRes.isSuccess) {
                    adminStats = statsRes.getOrThrow()
                }

                val logsRes = cloudClient.adminFetchAuditLogs(serverUrl, adminToken)
                if (logsRes.isSuccess) {
                    auditLogs = logsRes.getOrThrow()
                }

                val cfgRes = cloudClient.fetchRemoteConfig(serverUrl)
                if (cfgRes.isSuccess) {
                    val cfg = cfgRes.getOrThrow()
                    remoteAppName = cfg.appName
                    remoteAnnouncement = cfg.announcement
                    remoteMaintenance = cfg.maintenanceMode
                    remoteMaxUpload = cfg.maxUploadSizeMb
                    remoteDefaultQuota = cfg.defaultUserQuotaMb
                }
                isLoadingStats = false
            }
        }
    }

    LaunchedEffect(adminToken) {
        if (!adminToken.isNullOrEmpty()) {
            loadAdminData()
        }
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    if (adminToken == null) {
        // Admin Login Form
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Otentikasi Administrator", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    Text(
                        text = "Masuk menggunakan kredensial administrator server untuk mengelola seluruh pengguna, konfigurasi remote, kuota, dan log audit backend.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = adminUserInput,
                        onValueChange = { adminUserInput = it },
                        label = { Text("Username Admin") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = adminPassInput,
                        onValueChange = { adminPassInput = it },
                        label = { Text("Password Admin (Default: Admin@1234)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (loginError != null) {
                        Text(loginError!!, color = DangerRed, style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isLoggingIn = true
                                loginError = null
                                val res = cloudClient.adminLogin(serverUrl, adminUserInput, adminPassInput)
                                if (res.isSuccess) {
                                    val (token, user) = res.getOrThrow()
                                    userPrefs.saveAdminSession(token, user)
                                    Toast.makeText(context, "Selamat datang, Administrator!", Toast.LENGTH_SHORT).show()
                                } else {
                                    loginError = res.exceptionOrNull()?.message
                                }
                                isLoggingIn = false
                            }
                        },
                        enabled = !isLoggingIn,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoggingIn) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                        else Text("Masuk Panel Administrator")
                    }
                }
            }
        }
    } else {
        // Logged-in Admin Dashboard
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Admin Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Panel Kontrol Administrator", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Admin: $adminUsername • Server Non-SQL", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
                    }
                    Button(
                        onClick = { scope.launch { userPrefs.clearAdminSession() } },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Keluar Admin", fontSize = 11.sp)
                    }
                }
            }

            // Stats Cards Grid
            if (adminStats != null) {
                val stats = adminStats!!
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Total Pengguna", style = MaterialTheme.typography.labelSmall)
                                    Text("${stats.totalUsers}", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("Terdaftar di sistem", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Aktif 24 Jam", style = MaterialTheme.typography.labelSmall)
                                    Text("${stats.activeDaily}", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = SuccessGreen)
                                    Text("Pengguna hari ini", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Penyimpanan Server", style = MaterialTheme.typography.labelSmall)
                                    Text(formatBytes(stats.totalStorageBytes), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AccentGold)
                                    Text("Total file privat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Total File Cloud", style = MaterialTheme.typography.labelSmall)
                                    Text("${stats.totalFilesCount}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("Semua pengguna", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Users Management List
                item {
                    Text("Daftar Pengguna Terdaftar (${stats.users.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }

                items(stats.users, key = { it.id }) { uItem ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(uItem.username, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Badge(containerColor = if (uItem.status == "active") SuccessGreen else DangerRed) {
                                    Text(uItem.status.uppercase(), color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(
                                text = "Kuota: ${formatBytes(uItem.usedBytes)} / ${formatBytes(uItem.storageQuotaBytes)} • ${uItem.fileCount} file",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (uItem.deviceModel.isNotBlank()) {
                                Text("Perangkat: ${uItem.deviceModel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val res = cloudClient.adminToggleUser(serverUrl, adminToken, uItem.id)
                                            if (res.isSuccess) {
                                                loadAdminData()
                                                Toast.makeText(context, "Status diubah", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(if (uItem.status == "active") "Suspend" else "Aktifkan", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            val res = cloudClient.adminDeleteUser(serverUrl, adminToken, uItem.id)
                                            if (res.isSuccess) {
                                                loadAdminData()
                                                Toast.makeText(context, "Akun dihapus", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Hapus", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Remote Configuration Editor
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Konfigurasi Global Server Jarak Jauh (Remote Config)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Perubahan di sini langsung berlaku untuk seluruh pengguna aplikasi tanpa perlu kompilasi ulang APK:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = remoteAppName,
                            onValueChange = { remoteAppName = it },
                            label = { Text("Nama Aplikasi Remote") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = remoteAnnouncement,
                            onValueChange = { remoteAnnouncement = it },
                            label = { Text("Teks Pengumuman Global") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Maintenance Mode", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = remoteMaintenance,
                                onCheckedChange = { remoteMaintenance = it }
                            )
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    isSavingConfig = true
                                    val newCfg = RemoteAppConfig(
                                        appName = remoteAppName,
                                        announcement = remoteAnnouncement,
                                        maintenanceMode = remoteMaintenance,
                                        maxUploadSizeMb = remoteMaxUpload,
                                        defaultUserQuotaMb = remoteDefaultQuota
                                    )
                                    val res = cloudClient.adminSaveConfig(serverUrl, adminToken, newCfg)
                                    isSavingConfig = false
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Konfigurasi server berhasil diperbarui ke versi ${res.getOrNull()}!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Gagal: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isSavingConfig,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSavingConfig) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                            else Text("Simpan Konfigurasi ke Server")
                        }
                    }
                }
            }

            // Telegram 2FA Settings
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Integrasi Bot Telegram Admin", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Konfigurasi token bot Telegram untuk notifikasi login dan keamanan darurat:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = tgBotToken,
                            onValueChange = { tgBotToken = it },
                            label = { Text("Telegram Bot Token") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = tgChatId,
                            onValueChange = { tgChatId = it },
                            label = { Text("Admin Chat ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                scope.launch {
                                    val res = cloudClient.adminSaveTelegramConfig(
                                        serverUrl = serverUrl,
                                        adminToken = adminToken,
                                        botToken = tgBotToken,
                                        chatId = tgChatId,
                                        apiBase = tgApiBase,
                                        enable2fa = tgEnable2fa,
                                        sendTest = true
                                    )
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Konfigurasi disimpan & pesan tes terkirim!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Gagal", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Simpan & Kirim Pesan Uji Coba Telegram")
                        }
                    }
                }
            }

            // Server Audit Logs
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Catatan Audit Server (50 Terakhir):", fontWeight = FontWeight.Bold, color = SuccessGreen, style = MaterialTheme.typography.labelMedium)
                        if (auditLogs.isEmpty()) {
                            Text("Belum ada log tercatat", color = Color.Gray, fontSize = 12.sp)
                        } else {
                            auditLogs.take(15).forEach { line ->
                                Text(line, color = SuccessGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VaultPhotoGridItem(
    item: VaultItem,
    vaultRepo: VaultRepository,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val tempFile = remember(item.id) {
        try {
            vaultRepo.getDecryptedTempPreview(item)
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        if (tempFile != null && tempFile.exists()) {
            AsyncImage(
                model = tempFile,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (item.isCloudSynced) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen)
                    .padding(2.dp)
            ) {
                Icon(Icons.Default.CloudDone, contentDescription = "Tersinkron Cloud", tint = Color.Black, modifier = Modifier.size(12.dp))
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Dipilih", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VaultFileListCard(
    item: VaultItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onExport: () -> Unit,
    onSyncCloud: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    val iconVector = when (item.category) {
        VaultCategory.PHOTOS -> Icons.Default.Image
        VaultCategory.VIDEOS -> Icons.Default.Movie
        VaultCategory.DOCUMENTS -> Icons.Default.Description
        VaultCategory.ARCHIVES -> Icons.Default.Archive
        else -> Icons.Default.Description
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${formatFileSize(item.fileSize)} • ${dateFormat.format(Date(item.createdAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.isCloudSynced) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Cloud Synced",
                            tint = SuccessGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (!isSelectionMode) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Pulihkan ke Perangkat") },
                            onClick = {
                                showMenu = false
                                onExport()
                            },
                            leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Unggah ke Cloud") },
                            onClick = {
                                showMenu = false
                                onSyncCloud()
                            },
                            leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Ubah Nama") },
                            onClick = {
                                showMenu = false
                                onRename()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus dari Brankas", color = DangerRed) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed) }
                        )
                    }
                }
            }
        }
    }
}
