<?php
/**
 * NetSubnet REST API - Endpoint Resmi Aplikasi Android (Ruang Rahasia)
 * Menyediakan layanan Cloud Storage, Autentikasi, Sinkronisasi, Konfigurasi Global,
 * dan Panel Kontrol Administrator dalam Aplikasi.
 * 100% Bebas dari Database SQL (Arsitektur File JSON & flock Atomik).
 */

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/config.php';

$action = $_GET['action'] ?? '';
$authHeader = $_SERVER['HTTP_AUTHORIZATION'] ?? '';

// Helper pengirim response JSON
function jsonResponse(array $data, int $statusCode = 200): void {
    http_response_code($statusCode);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($data, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit;
}

// 1. Endpoint Konfigurasi Global (Dapat diambil tanpa login)
if ($action === 'config') {
    jsonResponse([
        'success' => true,
        'app_name' => $config['app_name'] ?? 'NetSubnet',
        'announcement' => $config['announcement'] ?? '',
        'maintenance_mode' => (bool)($config['maintenance_mode'] ?? false),
        'max_upload_size_mb' => (int)($config['max_upload_size_mb'] ?? 50),
        'default_user_quota_mb' => (int)($config['default_user_quota_mb'] ?? 1024),
        'remote_theme' => $config['remote_theme'] ?? 'CYAN',
        'opening_mode' => $config['opening_mode'] ?? 'NONE',
        'config_version' => (int)($config['config_version'] ?? 2),
        'server_time' => time()
    ]);
}

// Periksa mode maintenance (kecuali admin endpoints dan config)
if (!empty($config['maintenance_mode']) && !str_starts_with($action, 'admin_') && $action !== 'config') {
    jsonResponse(['success' => false, 'error' => 'Server sedang dalam pemeliharaan (Maintenance Mode).'], 503);
}

// 2. Registrasi Akun Pengguna
if ($action === 'register') {
    $rawInput = file_get_contents('php://input');
    $body = json_decode($rawInput, true) ?? $_POST;
    $username = $body['username'] ?? '';
    $password = $body['password'] ?? '';

    $res = $authService->registerUser($username, $password, (int)($config['default_user_quota_mb'] ?? 1024));
    if ($res['success']) {
        jsonResponse(['success' => true, 'message' => 'Registrasi akun berhasil. Silakan login.']);
    } else {
        jsonResponse(['success' => false, 'error' => $res['error']], 400);
    }
}

// 3. Login Akun Pengguna
if ($action === 'login') {
    $rawInput = file_get_contents('php://input');
    $body = json_decode($rawInput, true) ?? $_POST;
    $username = $body['username'] ?? '';
    $password = $body['password'] ?? '';

    $res = $authService->loginUser($username, $password);
    if ($res['success']) {
        jsonResponse([
            'success' => true,
            'token' => $res['token'],
            'username' => $res['user']['username'],
            'quota' => [
                'used_bytes' => $res['user']['used_bytes'],
                'total_bytes' => $res['user']['storage_quota'],
                'file_count' => $res['user']['file_count']
            ]
        ]);
    } else {
        jsonResponse(['success' => false, 'error' => $res['error']], 401);
    }
}

// 4. Login Administrator (Ruang Rahasia Android)
if ($action === 'admin_login') {
    $rawInput = file_get_contents('php://input');
    $body = json_decode($rawInput, true) ?? $_POST;
    $username = $body['username'] ?? '';
    $password = $body['password'] ?? '';

    $res = $authService->adminLogin($username, $password, $config);
    if ($res['success']) {
        jsonResponse([
            'success' => true,
            'admin_token' => $res['admin_token'],
            'username' => $res['username']
        ]);
    } else {
        jsonResponse(['success' => false, 'error' => $res['error']], 401);
    }
}

// =========================================================================
// ENDPOINT KHUSUS ADMINISTRATOR (Terproteksi Token Admin)
// =========================================================================
if (str_starts_with($action, 'admin_')) {
    $adminSession = $authService->authenticateAdminToken($authHeader);
    if (!$adminSession) {
        jsonResponse(['success' => false, 'error' => 'Akses ditolak: Memerlukan otentikasi administrator yang valid.'], 403);
    }

    // 5. Statistik Pengguna & Sistem
    if ($action === 'admin_stats') {
        $userIndex = $storage->readJson('users_index.json', []);
        $totalUsers = count($userIndex);
        $totalStorageBytes = 0;
        $totalFiles = 0;

        $now = time();
        $dailyThreshold = $now - 86400;
        $weeklyThreshold = $now - (7 * 86400);
        $monthlyThreshold = $now - (30 * 86400);

        $dailyActive = 0;
        $weeklyActive = 0;
        $monthlyActive = 0;

        $users = [];
        foreach ($userIndex as $uname => $uid) {
            $uDoc = $storage->readJson("users/{$uid}.json");
            if ($uDoc) {
                $lastActive = (int)($uDoc['last_active'] ?? $uDoc['created_at'] ?? 0);
                if ($lastActive >= $dailyThreshold) $dailyActive++;
                if ($lastActive >= $weeklyThreshold) $weeklyActive++;
                if ($lastActive >= $monthlyThreshold) $monthlyActive++;

                $uBytes = (int)($uDoc['used_bytes'] ?? 0);
                $uFiles = (int)($uDoc['file_count'] ?? 0);
                $totalStorageBytes += $uBytes;
                $totalFiles += $uFiles;

                $users[] = [
                    'id' => $uDoc['id'],
                    'username' => $uDoc['username'],
                    'status' => $uDoc['status'] ?? 'active',
                    'created_at' => (int)($uDoc['created_at'] ?? 0),
                    'last_active' => $lastActive,
                    'used_bytes' => $uBytes,
                    'storage_quota_bytes' => (int)($uDoc['storage_quota_bytes'] ?? 1073741824),
                    'file_count' => $uFiles,
                    'device_model' => $uDoc['device_model'] ?? '',
                    'client_version' => $uDoc['client_version'] ?? ''
                ];
            }
        }

        // Urutkan berdasarkan waktu aktif terbaru
        usort($users, fn($a, $b) => $b['last_active'] <=> $a['last_active']);

        jsonResponse([
            'success' => true,
            'stats' => [
                'total_users' => $totalUsers,
                'active_daily' => $dailyActive,
                'active_weekly' => $weeklyActive,
                'active_monthly' => $monthlyActive,
                'total_storage_bytes' => $totalStorageBytes,
                'total_files_count' => $totalFiles,
                'users' => $users
            ]
        ]);
    }

    // 6. Simpan Konfigurasi Global Remote
    if ($action === 'admin_save_config') {
        $rawInput = file_get_contents('php://input');
        $body = json_decode($rawInput, true) ?? $_POST;

        if (isset($body['app_name'])) $config['app_name'] = trim($body['app_name']);
        if (isset($body['announcement'])) $config['announcement'] = trim($body['announcement']);
        if (isset($body['maintenance_mode'])) $config['maintenance_mode'] = (bool)$body['maintenance_mode'];
        if (isset($body['max_upload_size_mb'])) $config['max_upload_size_mb'] = max(1, (int)$body['max_upload_size_mb']);
        if (isset($body['default_user_quota_mb'])) $config['default_user_quota_mb'] = max(10, (int)$body['default_user_quota_mb']);
        if (isset($body['remote_theme'])) $config['remote_theme'] = trim($body['remote_theme']);
        if (isset($body['opening_mode'])) $config['opening_mode'] = trim($body['opening_mode']);
        $config['config_version'] = ((int)($config['config_version'] ?? 1)) + 1;

        if (saveSystemConfig($storage, $config)) {
            $storage->logAudit('ADMIN_CONFIG_UPDATE', $adminSession['username'], ['version' => $config['config_version']]);
            jsonResponse([
                'success' => true,
                'message' => 'Konfigurasi global berhasil diperbarui.',
                'config_version' => $config['config_version']
            ]);
        } else {
            jsonResponse(['success' => false, 'error' => 'Gagal menyimpan konfigurasi server.'], 500);
        }
    }

    // 7. Toggle Status Pengguna (Aktifkan / Suspend)
    if ($action === 'admin_toggle_user') {
        $rawInput = file_get_contents('php://input');
        $body = json_decode($rawInput, true) ?? $_POST;
        $targetUserId = $body['user_id'] ?? '';

        $userDoc = $storage->readJson("users/{$targetUserId}.json");
        if (!$userDoc) {
            jsonResponse(['success' => false, 'error' => 'Pengguna tidak ditemukan.'], 404);
        }

        $newStatus = (($userDoc['status'] ?? 'active') === 'active') ? 'suspended' : 'active';
        $userDoc['status'] = $newStatus;
        $storage->writeJson("users/{$targetUserId}.json", $userDoc);
        $storage->logAudit('ADMIN_USER_STATUS_CHANGE', $adminSession['username'], ['target' => $userDoc['username'], 'status' => $newStatus]);

        jsonResponse([
            'success' => true,
            'message' => "Status pengguna '{$userDoc['username']}' berhasil diubah menjadi {$newStatus}.",
            'new_status' => $newStatus
        ]);
    }

    // 8. Ubah Kuota Pengguna Tertentu
    if ($action === 'admin_update_quota') {
        $rawInput = file_get_contents('php://input');
        $body = json_decode($rawInput, true) ?? $_POST;
        $targetUserId = $body['user_id'] ?? '';
        $quotaMb = max(10, (int)($body['quota_mb'] ?? 1024));

        $userDoc = $storage->readJson("users/{$targetUserId}.json");
        if (!$userDoc) {
            jsonResponse(['success' => false, 'error' => 'Pengguna tidak ditemukan.'], 404);
        }

        $userDoc['storage_quota_bytes'] = $quotaMb * 1024 * 1024;
        $storage->writeJson("users/{$targetUserId}.json", $userDoc);
        $storage->logAudit('ADMIN_QUOTA_UPDATE', $adminSession['username'], ['target' => $userDoc['username'], 'quota_mb' => $quotaMb]);

        jsonResponse([
            'success' => true,
            'message' => "Kuota pengguna '{$userDoc['username']}' diubah menjadi {$quotaMb} MB."
        ]);
    }

    // 9. Hapus Pengguna oleh Admin
    if ($action === 'admin_delete_user') {
        $rawInput = file_get_contents('php://input');
        $body = json_decode($rawInput, true) ?? $_POST;
        $targetUserId = $body['user_id'] ?? '';

        if ($authService->deleteUserAccount($targetUserId)) {
            $storage->logAudit('ADMIN_DELETE_USER', $adminSession['username'], ['user_id' => $targetUserId]);
            jsonResponse(['success' => true, 'message' => 'Pengguna dan seluruh file cloud miliknya berhasil dihapus.']);
        } else {
            jsonResponse(['success' => false, 'error' => 'Gagal menghapus pengguna atau pengguna tidak ditemukan.'], 404);
        }
    }

    // 10. Log Audit Server
    if ($action === 'admin_audit_logs') {
        $logFile = $storage->getDataDir() . '/logs/audit.log';
        $logs = [];
        if (file_exists($logFile)) {
            $lines = file($logFile, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
            $logs = array_reverse(array_slice($lines, -50));
        }
        jsonResponse(['success' => true, 'logs' => $logs]);
    }

    // 11. Konfigurasi Telegram Bot
    if ($action === 'admin_telegram_config') {
        $rawInput = file_get_contents('php://input');
        $body = json_decode($rawInput, true) ?? $_POST;

        if (isset($body['bot_token'])) $config['telegram_bot_token'] = trim($body['bot_token']);
        if (isset($body['chat_id'])) $config['telegram_admin_chat_id'] = trim($body['chat_id']);
        if (isset($body['api_base'])) $config['telegram_api_base'] = trim($body['api_base']);
        if (isset($body['enable_2fa'])) $config['enable_telegram_2fa'] = (bool)$body['enable_2fa'];

        saveSystemConfig($storage, $config);

        if (!empty($body['send_test'])) {
            $telegramService = new NetSubnet\Core\TelegramService($storage, $config);
            $testMsg = "🚀 <b>NetSubnet Ruang Rahasia Android</b>\n\nKoneksi Telegram Bot berhasil terhubung ke server!\n🕒 " . date('Y-m-d H:i:s');
            $testRes = $telegramService->sendMessage($testMsg);
            if (!$testRes['success']) {
                jsonResponse(['success' => false, 'error' => 'Pengaturan disimpan tetapi pesan uji coba gagal: ' . $testRes['error']], 400);
            }
        }

        jsonResponse(['success' => true, 'message' => 'Konfigurasi Telegram berhasil disimpan!']);
    }

    jsonResponse(['success' => false, 'error' => "Action admin '$action' tidak dikenali."], 404);
}

// =========================================================================
// ENDPOINT TERPROTEKSI TOKEN PENGGUNA (Bearer Token)
// =========================================================================
$currentUser = $authService->authenticateBearerToken($authHeader);
if (!$currentUser) {
    jsonResponse(['success' => false, 'error' => 'Akses ditolak: Token autentikasi tidak valid atau telah kedaluwarsa.'], 401);
}

$userId = $currentUser['id'] ?? $currentUser['user_id'];

// 12. Heartbeat & Telemetri Ringan
if ($action === 'heartbeat') {
    $rawInput = file_get_contents('php://input');
    $body = json_decode($rawInput, true) ?? $_POST;
    $deviceModel = $body['device_model'] ?? '';
    $clientVersion = $body['client_version'] ?? '';

    $authService->recordHeartbeat($userId, $deviceModel, $clientVersion);
    jsonResponse(['success' => true, 'timestamp' => time()]);
}

// 13. Ganti Password Pengguna
if ($action === 'change_password') {
    $rawInput = file_get_contents('php://input');
    $body = json_decode($rawInput, true) ?? $_POST;
    $oldPass = $body['old_password'] ?? '';
    $newPass = $body['new_password'] ?? '';

    $res = $authService->updatePassword($userId, $oldPass, $newPass);
    if ($res['success']) {
        jsonResponse(['success' => true, 'message' => $res['message']]);
    } else {
        jsonResponse(['success' => false, 'error' => $res['error']], 400);
    }
}

// 14. Hapus Akun Sendiri
if ($action === 'delete_account') {
    if ($authService->deleteUserAccount($userId)) {
        jsonResponse(['success' => true, 'message' => 'Akun dan semua file cloud Anda telah berhasil dihapus.']);
    } else {
        jsonResponse(['success' => false, 'error' => 'Gagal menghapus akun.'], 500);
    }
}

// 15. Kuota Penyimpanan Pengguna
if ($action === 'quota') {
    $userDoc = $storage->readJson("users/{$userId}.json");
    jsonResponse([
        'success' => true,
        'username' => $userDoc['username'],
        'quota' => [
            'used_bytes' => $userDoc['used_bytes'] ?? 0,
            'total_bytes' => $userDoc['storage_quota_bytes'] ?? 1073741824,
            'file_count' => $userDoc['file_count'] ?? 0
        ]
    ]);
}

// 16. Daftar File Cloud Pengguna
if ($action === 'list_files') {
    $userFilesDir = $storage->getDataDir() . "/files/{$userId}";
    $files = [];

    if (is_dir($userFilesDir)) {
        $metaFiles = glob($userFilesDir . '/*.json');
        foreach ($metaFiles as $metaPath) {
            $meta = json_decode(@file_get_contents($metaPath), true);
            if ($meta) {
                $files[] = [
                    'id' => $meta['id'],
                    'name' => $meta['name'],
                    'original_name' => $meta['original_name'],
                    'mime_type' => $meta['mime_type'],
                    'size' => $meta['size'],
                    'uploaded_at' => $meta['uploaded_at'],
                    'checksum' => $meta['checksum']
                ];
            }
        }
    }

    usort($files, fn($a, $b) => $b['uploaded_at'] <=> $a['uploaded_at']);
    jsonResponse(['success' => true, 'files' => $files]);
}

// 17. Upload File ke Cloud
if ($action === 'upload') {
    if (!isset($_FILES['file']) || $_FILES['file']['error'] !== UPLOAD_ERR_OK) {
        jsonResponse(['success' => false, 'error' => 'File tidak ditemukan dalam form upload atau terjadi kesalahan transfer.'], 400);
    }

    $uploadedFile = $_FILES['file'];
    $fileSize = (int)$uploadedFile['size'];
    $originalName = basename($uploadedFile['name']);
    $displayName = $_POST['name'] ?? $originalName;
    $mimeType = $_POST['mime_type'] ?? mime_content_type($uploadedFile['tmp_name']) ?: 'application/octet-stream';

    // Validasi batas ukuran file per unggahan
    $maxBytes = ((int)($config['max_upload_size_mb'] ?? 50)) * 1024 * 1024;
    if ($fileSize > $maxBytes) {
        jsonResponse(['success' => false, 'error' => "Ukuran file ({$fileSize} bytes) melebihi batas maksimal {$config['max_upload_size_mb']} MB."], 400);
    }

    // Validasi kuota pengguna
    $userDoc = $storage->readJson("users/{$userId}.json");
    $currentUsed = (int)($userDoc['used_bytes'] ?? 0);
    $maxQuota = (int)($userDoc['storage_quota_bytes'] ?? 1073741824);

    if (($currentUsed + $fileSize) > $maxQuota) {
        jsonResponse(['success' => false, 'error' => 'Kapasitas penyimpanan cloud akun Anda sudah penuh.'], 400);
    }

    // Buat direktori penyimpanan privat pengguna
    $userStorageDir = $storage->getDataDir() . "/storage/{$userId}";
    if (!is_dir($userStorageDir)) {
        @mkdir($userStorageDir, 0750, true);
    }

    $fileId = 'cld_' . bin2hex(random_bytes(12));
    $destBinaryPath = $userStorageDir . "/{$fileId}.dat";

    if (!move_uploaded_file($uploadedFile['tmp_name'], $destBinaryPath)) {
        jsonResponse(['success' => false, 'error' => 'Gagal memindahkan file ke penyimpanan privat server.'], 500);
    }

    $checksum = md5_file($destBinaryPath);

    // Simpan metadata file
    $fileMeta = [
        'id' => $fileId,
        'user_id' => $userId,
        'name' => $displayName,
        'original_name' => $originalName,
        'mime_type' => $mimeType,
        'size' => $fileSize,
        'checksum' => $checksum,
        'uploaded_at' => time()
    ];

    $storage->writeJson("files/{$userId}/{$fileId}.json", $fileMeta);

    // Perbarui kuota dan jumlah file pengguna
    $userDoc['used_bytes'] = $currentUsed + $fileSize;
    $userDoc['file_count'] = ((int)($userDoc['file_count'] ?? 0)) + 1;
    $storage->writeJson("users/{$userId}.json", $userDoc);

    $storage->logAudit('FILE_UPLOAD', $currentUser['username'] ?? $userId, ['file_id' => $fileId, 'size' => $fileSize]);

    jsonResponse([
        'success' => true,
        'message' => 'File berhasil disimpan di Cloud terverifikasi.',
        'file_id' => $fileId,
        'checksum' => $checksum
    ]);
}

// 18. Download File dari Cloud
if ($action === 'download') {
    $fileId = $_GET['id'] ?? '';
    if (empty($fileId)) {
        jsonResponse(['success' => false, 'error' => 'Parameter ID file diperlukan.'], 400);
    }

    $fileMeta = $storage->readJson("files/{$userId}/{$fileId}.json");
    if (!$fileMeta) {
        jsonResponse(['success' => false, 'error' => 'File tidak ditemukan atau Anda tidak memiliki akses.'], 404);
    }

    $binaryPath = $storage->getDataDir() . "/storage/{$userId}/{$fileId}.dat";
    if (!file_exists($binaryPath)) {
        jsonResponse(['success' => false, 'error' => 'File fisik tidak ditemukan di server.'], 404);
    }

    header('Content-Description: File Transfer');
    header('Content-Type: ' . ($fileMeta['mime_type'] ?: 'application/octet-stream'));
    header('Content-Disposition: attachment; filename="' . addslashes($fileMeta['name']) . '"');
    header('Expires: 0');
    header('Cache-Control: must-revalidate');
    header('Pragma: public');
    header('Content-Length: ' . filesize($binaryPath));
    readfile($binaryPath);
    exit;
}

// 19. Hapus File dari Cloud
if ($action === 'delete') {
    $fileId = $_GET['id'] ?? '';
    if (empty($fileId)) {
        jsonResponse(['success' => false, 'error' => 'Parameter ID file diperlukan.'], 400);
    }

    $fileMeta = $storage->readJson("files/{$userId}/{$fileId}.json");
    if (!$fileMeta) {
        jsonResponse(['success' => false, 'error' => 'File tidak ditemukan.'], 404);
    }

    $fileSize = (int)($fileMeta['size'] ?? 0);
    $binaryPath = $storage->getDataDir() . "/storage/{$userId}/{$fileId}.dat";

    @unlink($binaryPath);
    $storage->deleteFile("files/{$userId}/{$fileId}.json");

    $userDoc = $storage->readJson("users/{$userId}.json");
    if ($userDoc) {
        $userDoc['used_bytes'] = max(0, ((int)($userDoc['used_bytes'] ?? 0)) - $fileSize);
        $userDoc['file_count'] = max(0, ((int)($userDoc['file_count'] ?? 0)) - 1);
        $storage->writeJson("users/{$userId}.json", $userDoc);
    }

    $storage->logAudit('FILE_DELETE', $currentUser['username'] ?? $userId, ['file_id' => $fileId]);

    jsonResponse(['success' => true, 'message' => 'File berhasil dihapus dari cloud.']);
}

// Aksi tidak dikenali
jsonResponse(['success' => false, 'error' => "Action '$action' tidak valid."], 404);
