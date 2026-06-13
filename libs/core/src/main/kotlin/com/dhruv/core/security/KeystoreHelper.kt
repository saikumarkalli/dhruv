package com.dhruv.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_TAG_LENGTH = 128

/**
 * Thin wrapper around the Android Keystore for AES-GCM operations.
 * Keys never leave the secure hardware when the device supports it.
 */
object KeystoreHelper {

    fun getOrCreateSecretKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).apply {
            init(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    }

    fun encrypt(alias: String, plaintext: ByteArray): EncryptedBlob {
        val key = getOrCreateSecretKey(alias)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedBlob(iv = cipher.iv, ciphertext = ciphertext)
    }

    fun decrypt(alias: String, blob: EncryptedBlob): ByteArray {
        val key = getOrCreateSecretKey(alias)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, blob.iv))
        }
        return cipher.doFinal(blob.ciphertext)
    }
}

data class EncryptedBlob(val iv: ByteArray, val ciphertext: ByteArray) {

    fun toByteArray(): ByteArray =
        byteArrayOf(iv.size.toByte()) + iv + ciphertext

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedBlob) return false
        return iv.contentEquals(other.iv) && ciphertext.contentEquals(other.ciphertext)
    }

    override fun hashCode(): Int = 31 * iv.contentHashCode() + ciphertext.contentHashCode()

    companion object {
        fun fromByteArray(bytes: ByteArray): EncryptedBlob {
            val ivSize = bytes[0].toInt() and 0xFF
            val iv = bytes.sliceArray(1..ivSize)
            val ciphertext = bytes.sliceArray((ivSize + 1) until bytes.size)
            return EncryptedBlob(iv, ciphertext)
        }
    }
}
