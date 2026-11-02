package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.data.model.AppVaultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppVaultManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    suspend fun getInstalledLauncherApps(
        lockedPackages: Set<String>,
        silencedPackages: Set<String>
    ): List<AppVaultItem> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = packageManager.queryIntentActivities(launcherIntent, 0)
        val ownPackage = context.packageName

        val apps = mutableListOf<AppVaultItem>()
        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == ownPackage) continue // Skip our own app

            val label = info.loadLabel(packageManager).toString()
            val icon = info.loadIcon(packageManager)

            apps.add(
                AppVaultItem(
                    packageName = pkg,
                    label = label,
                    isLocked = lockedPackages.contains(pkg),
                    isSilenced = silencedPackages.contains(pkg),
                    icon = icon
                )
            )
        }

        // Urutkan alfabetis berdasarkan nama aplikasi
        apps.sortedBy { it.label.lowercase() }
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun openAppNotificationSettings(packageName: String) {
        try {
            val intent = Intent().apply {
                action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to app details
            try {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }
}
