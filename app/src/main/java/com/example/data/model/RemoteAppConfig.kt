package com.example.data.model

data class RemoteAppConfig(
    val appName: String = "NetSubnet",
    val announcement: String = "",
    val maintenanceMode: Boolean = false,
    val maxUploadSizeMb: Int = 50,
    val defaultUserQuotaMb: Int = 1024,
    val remoteTheme: String = "CYAN",
    val openingMode: String = "NONE", // "NONE", "BANNER_IMAGE", "ANIMATION"
    val configVersion: Int = 2
)
