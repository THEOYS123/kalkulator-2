<?php
/**
 * NetSubnet Backend API Service (Non-SQL)
 * Browser admin website has been removed in favor of in-app Ruang Rahasia management.
 */

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');

echo json_encode([
    'status' => 'online',
    'service' => 'NetSubnet Non-SQL Cloud API Engine',
    'version' => '2.0.0',
    'database' => 'Atomic JSON File Storage (No SQL Required)',
    'endpoint' => 'api.php',
    'management' => 'Managed exclusively via Android App (Ruang Rahasia)',
    'timestamp' => time()
], JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);
