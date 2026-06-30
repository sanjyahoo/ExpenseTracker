package com.example.expensetracker.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages a per-installation database passphrase backed by the Android Keystore.
 *
 * Flow:
 *  1. On first launch, generate a random 32-byte passphrase.
 *  2. Encrypt it with a Keystore-backed AES/GCM key that never leaves secure hardware.
 *  3. Store the (encrypted passphrase + IV) in a private SharedPreferences file.
 *  4. On subsequent launches, decrypt and return the same passphrase.
 *
 * The raw passphrase is only ever held in memory for the brief moment Room needs it;
 * callers should zero it out immediately after use.
 */
object DatabaseKeyManager {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "expense_tracker_db_master_key"
    private const val PREFS_NAME = "db_key_store"
    private const val PREF_ENCRYPTED_PASSPHRASE = "enc_passphrase"
    private const val PREF_IV = "enc_iv"
    private const val AES_MODE = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128

    /**
     * Returns the database passphrase, creating and persisting it on first call.
     * The caller is responsible for zeroing [ByteArray.fill](0) after use.
     */
    fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedB64 = prefs.getString(PREF_ENCRYPTED_PASSPHRASE, null)
        val ivB64 = prefs.getString(PREF_IV, null)

        return if (encryptedB64 != null && ivB64 != null) {
            decryptPassphrase(
                Base64.decode(encryptedB64, Base64.DEFAULT),
                Base64.decode(ivB64, Base64.DEFAULT)
            )
        } else {
            // Generate a cryptographically random 32-byte passphrase
            val passphrase = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
            val (encrypted, iv) = encryptPassphrase(passphrase)
            prefs.edit {
                putString(PREF_ENCRYPTED_PASSPHRASE, Base64.encodeToString(encrypted, Base64.DEFAULT))
                putString(PREF_IV, Base64.encodeToString(iv, Base64.DEFAULT))
            }
            passphrase
        }
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        }
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false) // key is for data-at-rest, not per-use auth
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            .apply { init(spec) }
            .generateKey()
    }

    private fun encryptPassphrase(passphrase: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(AES_MODE).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKeystoreKey())
        }
        return Pair(cipher.doFinal(passphrase), cipher.iv)
    }

    private fun decryptPassphrase(encrypted: ByteArray, iv: ByteArray): ByteArray {
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        val cipher = Cipher.getInstance(AES_MODE).apply {
            init(Cipher.DECRYPT_MODE, getOrCreateKeystoreKey(), spec)
        }
        return cipher.doFinal(encrypted)
    }
}
