package com.lumora.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Device-wide administrative PIN. It is intentionally separate from the per-profile PINs
 * owned by ParafusoParentalStore.
 *
 * A pre-2.0 global parental PIN remains readable from iptv_prefs. The migration is additive:
 * the old value and its lockout state are never removed, while new writes use a versioned
 * PBKDF2-HMAC-SHA256 record in this store's own preferences.
 */
internal class AdministrativePinStore(context: Context) {
    private val adminPrefs = context.getSharedPreferences(ADMIN_PREFS_NAME, Context.MODE_PRIVATE)
    private val legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun hasPin(): Boolean {
        migrateLegacyIfNeeded()
        return adminPrefs.contains(KEY_SCHEME) && !adminPrefs.getString(KEY_HASH, null).isNullOrBlank()
    }

    @Synchronized
    fun lockoutUntilMillis(): Long {
        migrateLegacyIfNeeded()
        return adminPrefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
    }

    /** Stores only the current PBKDF2 record. Returns false for an invalid administrative PIN. */
    @Synchronized
    fun setPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        writeCurrentFormat(pin)
        return true
    }

    /**
     * Verifies a PIN and preserves the legacy five-attempt, 30-second lockout behavior.
     * A correct legacy SHA-256 PIN is upgraded in place to the current format.
     */
    @Synchronized
    fun verifyAndRecordAttempt(pin: String, nowMillis: Long = System.currentTimeMillis()): AdministrativePinVerification {
        migrateLegacyIfNeeded()
        val lockoutUntil = adminPrefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        if (nowMillis < lockoutUntil) return AdministrativePinVerification.Locked(lockoutUntil, justLocked = false)

        if (matchesStoredPin(pin)) {
            if (adminPrefs.getString(KEY_SCHEME, null) == SCHEME_LEGACY_SHA256_V1) {
                writeCurrentFormat(pin)
            }
            adminPrefs.edit().remove(KEY_FAILURES).remove(KEY_LOCKOUT_UNTIL).apply()
            return AdministrativePinVerification.Verified
        }

        val failures = adminPrefs.getInt(KEY_FAILURES, 0) + 1
        return if (failures >= MAX_PIN_ATTEMPTS) {
            val until = nowMillis + PIN_LOCKOUT_MS
            adminPrefs.edit().putInt(KEY_FAILURES, 0).putLong(KEY_LOCKOUT_UNTIL, until).apply()
            AdministrativePinVerification.Locked(until, justLocked = true)
        } else {
            adminPrefs.edit().putInt(KEY_FAILURES, failures).apply()
            AdministrativePinVerification.Invalid(failures)
        }
    }

    private fun migrateLegacyIfNeeded() {
        if (adminPrefs.contains(KEY_SCHEME)) return

        val legacyValue = legacyPrefs.getString(LEGACY_PIN, null)?.takeIf { it.isNotBlank() } ?: return
        val editor = adminPrefs.edit()
            .putInt(KEY_FAILURES, legacyPrefs.getInt(LEGACY_FAILURES, 0))
            .putLong(KEY_LOCKOUT_UNTIL, legacyPrefs.getLong(LEGACY_LOCKOUT_UNTIL, 0L))

        if (isLegacyPlaintextPin(legacyValue)) {
            val record = currentRecord(legacyValue)
            editor
                .putString(KEY_SCHEME, SCHEME_PBKDF2_SHA256_V1)
                .putString(KEY_HASH, record.hash)
                .putString(KEY_SALT, record.salt)
                .putInt(KEY_ITERATIONS, PBKDF2_ITERATIONS)
        } else {
            editor
                .putString(KEY_SCHEME, SCHEME_LEGACY_SHA256_V1)
                .putString(KEY_HASH, legacyValue)
                .putString(KEY_SALT, legacySalt())
        }

        // The legacy preferences are deliberately not changed or removed. A repeated call sees
        // KEY_SCHEME and becomes a no-op, so interrupted application starts cannot duplicate data.
        editor.commit()
    }

    private fun matchesStoredPin(pin: String): Boolean {
        val hash = adminPrefs.getString(KEY_HASH, null) ?: return false
        return when (adminPrefs.getString(KEY_SCHEME, null)) {
            SCHEME_PBKDF2_SHA256_V1 -> {
                val salt = adminPrefs.getString(KEY_SALT, null) ?: return false
                val iterations = adminPrefs.getInt(KEY_ITERATIONS, PBKDF2_ITERATIONS)
                val expected = decode(hash) ?: return false
                val actual = derivePbkdf2(pin, decode(salt) ?: return false, iterations)
                MessageDigest.isEqual(expected, actual)
            }
            SCHEME_LEGACY_SHA256_V1 -> {
                val salt = adminPrefs.getString(KEY_SALT, null) ?: return false
                MessageDigest.isEqual(
                    hash.toByteArray(Charsets.UTF_8),
                    legacyHash(pin, salt).toByteArray(Charsets.UTF_8)
                )
            }
            else -> false
        }
    }

    private fun writeCurrentFormat(pin: String) {
        val record = currentRecord(pin)
        adminPrefs.edit()
            .putString(KEY_SCHEME, SCHEME_PBKDF2_SHA256_V1)
            .putString(KEY_HASH, record.hash)
            .putString(KEY_SALT, record.salt)
            .putInt(KEY_ITERATIONS, PBKDF2_ITERATIONS)
            .apply()
    }

    private fun currentRecord(pin: String): PinRecord {
        val salt = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)
        return PinRecord(
            hash = encode(derivePbkdf2(pin, salt, PBKDF2_ITERATIONS)),
            salt = encode(salt)
        )
    }

    private fun legacySalt(): String =
        legacyPrefs.getString(LEGACY_SALT, null) ?: java.util.UUID.randomUUID().toString().also { salt ->
            legacyPrefs.edit().putString(LEGACY_SALT, salt).apply()
        }

    private fun derivePbkdf2(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, HASH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun legacyHash(pin: String, salt: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest((salt + pin).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray? = runCatching {
        Base64.decode(value, Base64.NO_WRAP)
    }.getOrNull()

    private fun isValidPin(pin: String): Boolean = pin.length == PIN_LENGTH && pin.all(Char::isDigit)

    private fun isLegacyPlaintextPin(value: String): Boolean =
        value.length == PIN_LENGTH && value.all(Char::isDigit)

    private data class PinRecord(val hash: String, val salt: String)

    internal companion object {
        const val PIN_LOCKOUT_MS = 30_000L

        private const val ADMIN_PREFS_NAME = "parafuso_admin_security"
        private const val LEGACY_PREFS_NAME = "iptv_prefs"
        private const val LEGACY_PIN = "parental_pin"
        private const val LEGACY_SALT = "parental_pin_salt"
        private const val LEGACY_FAILURES = "pin_failures"
        private const val LEGACY_LOCKOUT_UNTIL = "pin_lockout_until"

        private const val KEY_SCHEME = "admin_pin_scheme"
        private const val KEY_HASH = "admin_pin_hash"
        private const val KEY_SALT = "admin_pin_salt"
        private const val KEY_ITERATIONS = "admin_pin_iterations"
        private const val KEY_FAILURES = "admin_pin_failures"
        private const val KEY_LOCKOUT_UNTIL = "admin_pin_lockout_until"

        private const val SCHEME_PBKDF2_SHA256_V1 = "pbkdf2-hmac-sha256-v1"
        private const val SCHEME_LEGACY_SHA256_V1 = "legacy-salted-sha256-v1"
        private const val PIN_LENGTH = 4
        private const val SALT_BYTES = 16
        private const val HASH_BITS = 256
        private const val PBKDF2_ITERATIONS = 210_000
        private const val MAX_PIN_ATTEMPTS = 5

        private val secureRandom = SecureRandom()
    }
}

internal sealed interface AdministrativePinVerification {
    data object Verified : AdministrativePinVerification
    data class Invalid(val failures: Int) : AdministrativePinVerification
    data class Locked(val untilMillis: Long, val justLocked: Boolean) : AdministrativePinVerification
}
