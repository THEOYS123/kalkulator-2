<?php
/**
 * AuthService - Manajemen Autentikasi Pengguna & Admin (Non-SQL)
 * Menggunakan password_hash (Argon2id/Bcrypt), token bearer untuk mobile Android,
 * sepenuhnya tanpa database SQL.
 */

namespace NetSubnet\Core;

class AuthService {
    private FileStorageEngine $storage;

    public function __construct(FileStorageEngine $storage) {
        $this->storage = $storage;
    }

    /**
     * Mendaftarkan akun pengguna mobile baru
     */
    public function registerUser(string $username, string $password, int $defaultQuotaMb = 1024): array {
        $cleanUsername = strtolower(trim($username));
        if (strlen($cleanUsername) < 3 || strlen($cleanUsername) > 30) {
            return ['success' => false, 'error' => 'Username harus antara 3 - 30 karakter alfanumerik.'];
        }

        if (strlen($password) < 6) {
            return ['success' => false, 'error' => 'Password minimal harus 6 karakter.'];
        }

        $userIndex = $this->storage->readJson('users_index.json', []);
        if (isset($userIndex[$cleanUsername])) {
            return ['success' => false, 'error' => 'Username sudah digunakan, silakan pilih nama lain.'];
        }

        $userId = 'usr_' . bin2hex(random_bytes(10));
        $userDoc = [
            'id' => $userId,
            'username' => $cleanUsername,
            'password_hash' => password_hash($password, PASSWORD_DEFAULT),
            'status' => 'active',
            'created_at' => time(),
            'last_active' => time(),
            'storage_quota_bytes' => $defaultQuotaMb * 1024 * 1024,
            'used_bytes' => 0,
            'file_count' => 0,
            'device_model' => '',
            'client_version' => ''
        ];

        // Tulis dokumen pengguna
        $this->storage->writeJson("users/{$userId}.json", $userDoc);

        // Update index
        $userIndex[$cleanUsername] = $userId;
        $this->storage->writeJson('users_index.json', $userIndex);

        $this->storage->logAudit('USER_REGISTER', $cleanUsername, ['user_id' => $userId]);

        return ['success' => true, 'user_id' => $userId];
    }

    /**
     * Login pengguna mobile dan menghasilkan token bearer
     */
    public function loginUser(string $username, string $password): array {
        $cleanUsername = strtolower(trim($username));
        $userIndex = $this->storage->readJson('users_index.json', []);

        if (!isset($userIndex[$cleanUsername])) {
            return ['success' => false, 'error' => 'Username atau password salah.'];
        }

        $userId = $userIndex[$cleanUsername];
        $userDoc = $this->storage->readJson("users/{$userId}.json");

        if (!$userDoc) {
            return ['success' => false, 'error' => 'Data pengguna tidak ditemukan.'];
        }

        if (($userDoc['status'] ?? 'active') !== 'active') {
            return ['success' => false, 'error' => 'Akun dinonaktifkan oleh administrator.'];
        }

        if (!password_verify($password, $userDoc['password_hash'])) {
            return ['success' => false, 'error' => 'Username atau password salah.'];
        }

        // Perbarui timestamp aktif terakhir
        $userDoc['last_active'] = time();
        $this->storage->writeJson("users/{$userId}.json", $userDoc);

        // Buat session token
        $token = bin2hex(random_bytes(32));
        $sessionDoc = [
            'token' => $token,
            'user_id' => $userId,
            'username' => $cleanUsername,
            'created_at' => time(),
            'expires_at' => time() + (30 * 86400), // 30 hari
            'ip' => $_SERVER['REMOTE_ADDR'] ?? '127.0.0.1',
            'is_admin' => false
        ];

        $this->storage->writeJson("sessions/{$token}.json", $sessionDoc);
        $this->storage->logAudit('USER_LOGIN', $cleanUsername, ['ip' => $sessionDoc['ip']]);

        return [
            'success' => true,
            'token' => $token,
            'user' => [
                'id' => $userId,
                'username' => $cleanUsername,
                'storage_quota' => $userDoc['storage_quota_bytes'],
                'used_bytes' => $userDoc['used_bytes'] ?? 0,
                'file_count' => $userDoc['file_count'] ?? 0
            ]
        ];
    }

    /**
     * Memverifikasi Bearer Token dari header HTTP
     */
    public function authenticateBearerToken(?string $authHeader): ?array {
        if (!$authHeader || !preg_match('/Bearer\s+(\S+)/i', $authHeader, $matches)) {
            return null;
        }

        $token = $matches[1];
        $sessionDoc = $this->storage->readJson("sessions/{$token}.json");

        if (!$sessionDoc) {
            return null;
        }

        if (time() > ($sessionDoc['expires_at'] ?? 0)) {
            $this->storage->deleteFile("sessions/{$token}.json");
            return null;
        }

        // Jika session bertipe admin, kembalikan sessionDoc
        if (!empty($sessionDoc['is_admin'])) {
            return $sessionDoc;
        }

        $userDoc = $this->storage->readJson("users/{$sessionDoc['user_id']}.json");
        if (!$userDoc || ($userDoc['status'] ?? 'active') !== 'active') {
            return null;
        }

        return $userDoc;
    }

    /**
     * Login Administrator dari Aplikasi Android (Ruang Rahasia)
     */
    public function adminLogin(string $username, string $password, array $config): array {
        $adminUser = $config['admin_username'] ?? 'admin';
        $adminHash = $config['admin_password_hash'] ?? '';

        if (trim($username) !== $adminUser) {
            return ['success' => false, 'error' => 'Kredensial administrator tidak valid.'];
        }

        $valid = empty($adminHash) ? ($password === 'Admin@1234') : password_verify($password, $adminHash);
        if (!$valid) {
            return ['success' => false, 'error' => 'Password administrator salah.'];
        }

        $adminToken = 'adm_' . bin2hex(random_bytes(28));
        $sessionDoc = [
            'token' => $adminToken,
            'user_id' => 'admin',
            'username' => $adminUser,
            'created_at' => time(),
            'expires_at' => time() + (7 * 86400),
            'ip' => $_SERVER['REMOTE_ADDR'] ?? '127.0.0.1',
            'is_admin' => true
        ];

        $this->storage->writeJson("sessions/{$adminToken}.json", $sessionDoc);
        $this->storage->logAudit('ADMIN_LOGIN', $adminUser, ['ip' => $sessionDoc['ip']]);

        return [
            'success' => true,
            'admin_token' => $adminToken,
            'username' => $adminUser
        ];
    }

    /**
     * Memverifikasi apakah request berasal dari Administrator
     */
    public function authenticateAdminToken(?string $authHeader): ?array {
        if (!$authHeader || !preg_match('/Bearer\s+(\S+)/i', $authHeader, $matches)) {
            return null;
        }

        $token = $matches[1];
        $sessionDoc = $this->storage->readJson("sessions/{$token}.json");

        if (!$sessionDoc || empty($sessionDoc['is_admin'])) {
            return null;
        }

        if (time() > ($sessionDoc['expires_at'] ?? 0)) {
            $this->storage->deleteFile("sessions/{$token}.json");
            return null;
        }

        return $sessionDoc;
    }

    /**
     * Memperbarui informasi heartbeat & aktivitas (anonim/telemetri teknis)
     */
    public function recordHeartbeat(string $userId, string $deviceModel, string $clientVersion): void {
        $userDoc = $this->storage->readJson("users/{$userId}.json");
        if ($userDoc) {
            $userDoc['last_active'] = time();
            if (!empty($deviceModel)) $userDoc['device_model'] = substr($deviceModel, 0, 50);
            if (!empty($clientVersion)) $userDoc['client_version'] = substr($clientVersion, 0, 20);
            $this->storage->writeJson("users/{$userId}.json", $userDoc);
        }
    }

    /**
     * Mengubah password pengguna
     */
    public function updatePassword(string $userId, string $oldPassword, string $newPassword): array {
        if (strlen($newPassword) < 6) {
            return ['success' => false, 'error' => 'Password baru minimal 6 karakter.'];
        }

        $userDoc = $this->storage->readJson("users/{$userId}.json");
        if (!$userDoc) {
            return ['success' => false, 'error' => 'Pengguna tidak ditemukan.'];
        }

        if (!password_verify($oldPassword, $userDoc['password_hash'])) {
            return ['success' => false, 'error' => 'Password lama tidak cocok.'];
        }

        $userDoc['password_hash'] = password_hash($newPassword, PASSWORD_DEFAULT);
        $this->storage->writeJson("users/{$userId}.json", $userDoc);
        $this->storage->logAudit('PASSWORD_CHANGE', $userDoc['username']);

        return ['success' => true, 'message' => 'Password akun berhasil diubah.'];
    }

    /**
     * Menghapus akun pengguna beserta semua file dan sesinya
     */
    public function deleteUserAccount(string $userId): bool {
        $userDoc = $this->storage->readJson("users/{$userId}.json");
        if (!$userDoc) return false;

        $username = $userDoc['username'];

        // Hapus file fisik di storage/
        $storageDir = $this->storage->getDataDir() . "/storage/{$userId}";
        if (is_dir($storageDir)) {
            $files = glob($storageDir . '/*');
            foreach ($files as $f) @unlink($f);
            @rmdir($storageDir);
        }

        // Hapus metadata di files/
        $filesMetaDir = $this->storage->getDataDir() . "/files/{$userId}";
        if (is_dir($filesMetaDir)) {
            $mFiles = glob($filesMetaDir . '/*');
            foreach ($mFiles as $mf) @unlink($mf);
            @rmdir($filesMetaDir);
        }

        // Hapus dari index
        $userIndex = $this->storage->readJson('users_index.json', []);
        unset($userIndex[$username]);
        $this->storage->writeJson('users_index.json', $userIndex);

        // Hapus dokumen user
        $this->storage->deleteFile("users/{$userId}.json");
        $this->storage->logAudit('USER_DELETE', $username, ['user_id' => $userId]);

        return true;
    }
}
