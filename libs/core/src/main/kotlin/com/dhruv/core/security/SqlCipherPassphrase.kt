package com.dhruv.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.SecureRandom

private const val PREFS_NAME = "dhruv_cipher_prefs"
private const val KEY_ENCRYPTED_PASSPHRASE = "enc_passphrase"
private const val KEYSTORE_ALIAS = "dhruv_sqlcipher"
private const val PASSPHRASE_BYTE_LENGTH = 32

/**
 * Manages the SQLCipher database passphrase.
 *
 * On first call a 32-byte random passphrase is generated, encrypted with a Keystore-backed
 * AES-GCM key, and stored in SharedPreferences as Base64. The plaintext never touches disk.
 * The returned CharArray should be wiped by the caller after use.
 */
object SqlCipherPassphrase {
    fun getOrCreate(context: Context): CharArray {
        val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val stored = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null)
        if (stored != null) {
            val encryptedBytes = Base64.decode(stored, Base64.NO_WRAP)
            val plaintext = KeystoreHelper.decrypt(KEYSTORE_ALIAS, EncryptedBlob.fromByteArray(encryptedBytes))
            return plaintext.toCharArray()
        }

        // First run — generate and persist.
        val passphrase = ByteArray(PASSPHRASE_BYTE_LENGTH).also { SecureRandom().nextBytes(it) }
        val blob = KeystoreHelper.encrypt(KEYSTORE_ALIAS, passphrase)
        prefs
            .edit()
            .putString(KEY_ENCRYPTED_PASSPHRASE, Base64.encodeToString(blob.toByteArray(), Base64.NO_WRAP))
            .apply()

        return passphrase.toCharArray().also { passphrase.fill(0) }
    }
}

private fun ByteArray.toCharArray(): CharArray = CharArray(size) { this[it].toInt().toChar() }
