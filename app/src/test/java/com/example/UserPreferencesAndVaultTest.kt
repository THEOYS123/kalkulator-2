package com.example

import com.example.data.model.RemoteAppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class UserPreferencesAndVaultTest {

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun testPinHashingIntegrity() {
        val pin = "1234"
        val hash1 = hashPin(pin)
        val hash2 = hashPin(pin)
        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length) // SHA-256 produces 64 hex characters
    }

    @Test
    fun testRemoteAppConfigDefaults() {
        val config = RemoteAppConfig()
        assertEquals("NetSubnet", config.appName)
        assertFalse(config.maintenanceMode)
        assertEquals(50, config.maxUploadSizeMb)
        assertEquals(1024, config.defaultUserQuotaMb)
        assertEquals("CYAN", config.remoteTheme)
        assertEquals("NONE", config.openingMode)
    }
}
