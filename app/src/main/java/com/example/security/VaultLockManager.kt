package com.example.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

class VaultLockManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("passguard_lock_prefs", Context.MODE_PRIVATE)

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var backgroundTimestamp: Long = 0L

    companion object {
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_AUTO_LOCK_TIMEOUT = "key_auto_lock_timeout"
        private const val DEFAULT_PIN = "1234"
    }

    init {
        // Initialize default PIN hash if not set
        if (!prefs.contains(KEY_PIN_HASH)) {
            setMasterPin(DEFAULT_PIN)
        }
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun getAutoLockTimeoutSeconds(): Int {
        return prefs.getInt(KEY_AUTO_LOCK_TIMEOUT, 60) // default 1 minute
    }

    fun setAutoLockTimeoutSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_AUTO_LOCK_TIMEOUT, seconds).apply()
    }

    fun setMasterPin(pin: String) {
        val hash = sha256(pin)
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, "") ?: ""
        return sha256(pin) == storedHash
    }

    fun unlock() {
        _isLocked.value = false
        backgroundTimestamp = 0L
    }

    fun lock() {
        _isLocked.value = true
    }

    fun onAppBackgrounded() {
        backgroundTimestamp = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        val timeoutSec = getAutoLockTimeoutSeconds()
        if (timeoutSec < 0) return // Never lock
        if (backgroundTimestamp > 0L) {
            val elapsedSec = (System.currentTimeMillis() - backgroundTimestamp) / 1000
            if (elapsedSec >= timeoutSec) {
                lock()
            }
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
