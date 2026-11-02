package com.example.data.model

data class AdminStats(
    val totalUsers: Int = 0,
    val activeDaily: Int = 0,
    val activeWeekly: Int = 0,
    val activeMonthly: Int = 0,
    val totalStorageBytes: Long = 0L,
    val totalFilesCount: Int = 0,
    val users: List<AdminUserItem> = emptyList()
)

data class AdminUserItem(
    val id: String,
    val username: String,
    val status: String,
    val createdAt: Long,
    val lastActive: Long,
    val usedBytes: Long,
    val storageQuotaBytes: Long,
    val fileCount: Int,
    val deviceModel: String,
    val clientVersion: String
)
