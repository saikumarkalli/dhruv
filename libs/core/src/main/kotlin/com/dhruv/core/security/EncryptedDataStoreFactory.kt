package com.dhruv.core.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.InputStream
import java.io.OutputStream

private const val KEY_ALIAS_PREFIX = "dhruv_ds_"

/**
 * Creates a DataStore<Preferences> whose bytes are encrypted with a Keystore-backed AES-GCM key.
 * The encryption key never leaves the Android Keystore.
 */
object EncryptedDataStoreFactory {
    fun create(
        context: Context,
        fileName: String,
    ): DataStore<Preferences> {
        val alias = "$KEY_ALIAS_PREFIX$fileName"
        return DataStoreFactory.create(
            serializer = EncryptedPreferencesSerializer(alias),
            produceFile = { File(context.filesDir, "datastore/$fileName.preferences_pb_enc") },
        )
    }
}

private class EncryptedPreferencesSerializer(
    private val keystoreAlias: String,
) : Serializer<Preferences> {
    private val delegate = PreferencesSerializer

    override val defaultValue: Preferences get() = delegate.defaultValue

    // Corrupt/incompatible ciphertext (or first run) must yield defaults, not crash; the broad
    // catch + intentional swallow is the recovery path (logging plaintext failures here is unsafe).
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun readFrom(input: InputStream): Preferences {
        val encryptedBytes = input.readBytes()
        if (encryptedBytes.isEmpty()) return defaultValue
        return try {
            val plaintext = KeystoreHelper.decrypt(keystoreAlias, EncryptedBlob.fromByteArray(encryptedBytes))
            plaintext
                .inputStream()
                .source()
                .buffer()
                .use { delegate.readFrom(it) }
        } catch (e: Exception) {
            // Corrupt or first-run — return defaults rather than crash.
            defaultValue
        }
    }

    override suspend fun writeTo(
        t: Preferences,
        output: OutputStream,
    ) {
        val plainBytes = buildPlainBytes(t)
        val blob = KeystoreHelper.encrypt(keystoreAlias, plainBytes)
        output.write(blob.toByteArray())
    }

    private suspend fun buildPlainBytes(prefs: Preferences): ByteArray {
        val baos = java.io.ByteArrayOutputStream()
        baos.sink().buffer().use { delegate.writeTo(prefs, it) }
        return baos.toByteArray()
    }
}
