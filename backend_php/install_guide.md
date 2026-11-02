# Panduan Pemasangan Backend API NetSubnet (Non-SQL)

Backend ini dirancang khusus untuk melayani aplikasi Android **NetSubnet Ruang Rahasia** tanpa memerlukan database SQL (MySQL, PostgreSQL, MariaDB, SQLite) dan tanpa website admin browser (seluruh pengelolaan dilakukan langsung melalui Ruang Rahasia pada aplikasi Android).

---

## 1. Persyaratan Server
- PHP versi 7.4, 8.0, 8.1, 8.2, atau 8.3
- Ekstensi PHP standar: `json`, `mbstring`, `fileinfo`, `curl`
- Izin tulis direktori (Write Permissions) pada folder `data/`

---

## 2. Cara Unggah ke Hosting (cPanel, XAMPP, VPS, atau Hosting Gratis)
1. Unggah seluruh isi folder `backend_php/` ke dalam folder public server Anda (contoh: `/public_html/` atau `/htdocs/`).
2. Pastikan file `.htaccess` di dalam folder `data/` tetap ada untuk mencegah akses langsung web ke data pengguna.
3. Berikan permission `755` atau `775` pada folder `data/` dan subfoldernya.

---

## 3. Struktur Direktori Data (Otomatis Dibuat)
```text
data/
├── .htaccess            (Proteksi akses langsung web)
├── config/              (Pengaturan server dinamis)
├── files/               (Metadata file pengguna)
├── logs/                (Catatan audit keamanan)
├── sessions/            (Token sesi mobile & admin)
├── storage/             (Penyimpanan file privat terenkripsi)
├── users/               (Data profil & kuota pengguna)
└── users_index.json     (Indeks pencarian cepat username)
```

---

## 4. Akun Administrator Default
- **Username:** `admin`
- **Password Default:** `Admin@1234`
- Administrator masuk langsung melalui tab **Panel Admin** di dalam **Ruang Rahasia** pada aplikasi Android untuk:
  - Memantau statistik pengguna (harian, mingguan, bulanan, kuota terpakai, riwayat login).
  - Mengubah konfigurasi global remote (nama aplikasi, pengumuman, batas upload, kuota default).
  - Mengelola akun pengguna (suspend, ubah kuota, hapus akun).
  - Mengonfigurasi notifikasi Telegram Bot 2FA.

---

## 5. Hubungkan Aplikasi Android
1. Buka aplikasi Android **NetSubnet**.
2. Masuk ke **Ruang Rahasia** (Ketik IP Pemicu Rahasia `192.168.99.99` pada kalkulator lalu hitung, atau gunakan PIN `1234`).
3. Buka tab **Pengaturan** / **Cloud**.
4. Masukkan URL server Anda (contoh: `https://domain-anda.com/api.php` atau `http://10.0.2.2:8000/api.php` untuk emulator lokal).
5. Buat akun atau login, dan data Anda akan tersinkronisasi secara aman.
