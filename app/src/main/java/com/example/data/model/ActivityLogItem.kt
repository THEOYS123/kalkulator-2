package com.example.data.model

data class ActivityLogItem(
    val id: String,
    val timestamp: Long,
    val action: String,
    val details: String,
    val type: String // "SECURITY", "FILE", "CLOUD", "APP_VAULT", "CONFIG"
)
