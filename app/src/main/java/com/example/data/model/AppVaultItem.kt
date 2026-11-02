package com.example.data.model

import android.graphics.drawable.Drawable

data class AppVaultItem(
    val packageName: String,
    val label: String,
    val isLocked: Boolean = false,
    val isSilenced: Boolean = false,
    val icon: Drawable? = null
)
