package com.dhruv.finance.money

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * T054/research R6: receipts are attached to a transaction and viewable from its detail, but they
 * are **device-local only** this phase — no Supabase Storage upload. A picked image's bytes are
 * copied into this app's own private storage (`context.filesDir`, never a shared/public directory)
 * and referenced by a `file://` URI string stored in
 * [com.dhruv.finance.data.tracker.model.Transaction.receiptPath]. The UI must state this ("Stays on
 * this device") rather than let the user assume it synced like everything else on this screen.
 *
 * Plain Kotlin, no Compose — `TransactionFormScreen.kt` (attach) and `TransactionDetailScreen.kt`
 * (view) both consume it the same way.
 */
class ReceiptStore(private val context: Context) {
    private val receiptsDir: File
        get() = File(context.filesDir, RECEIPTS_DIR_NAME).apply { mkdirs() }

    /**
     * Copies [pickedUri]'s bytes (from a system image/document picker) into a new private file and
     * returns its `file://` URI as a plain string, ready to store as `receiptPath`. Returns `null`
     * if the source couldn't be opened/read — callers treat that as "no receipt attached" rather
     * than a fatal error, since a receipt is optional (FR-004).
     */
    fun attach(pickedUri: Uri): String? {
        val destination = File(receiptsDir, "${UUID.randomUUID()}.jpg")
        return try {
            val input = context.contentResolver.openInputStream(pickedUri) ?: return null
            input.use { source -> destination.outputStream().use { sink -> source.copyTo(sink) } }
            Uri.fromFile(destination).toString()
        } catch (e: IOException) {
            null
        }
    }

    /**
     * True when [receiptPath] still points at a file on this device. A stored path can go stale
     * after a data clear/reinstall (`filesDir` is not backed up for this app's tracker data —
     * PLATFORM.md §5 "Backup honesty" — receipts are no exception), so the detail screen checks
     * this before offering "View receipt" rather than opening a dead link.
     */
    fun exists(receiptPath: String?): Boolean {
        val path = receiptPath?.let { Uri.parse(it).path } ?: return false
        return File(path).exists()
    }

    /** Resolves a stored `file://` string back to a [Uri] for in-app display, or `null` when
     * [receiptPath] is absent or stale (see [exists]). */
    fun resolve(receiptPath: String?): Uri? = if (exists(receiptPath)) Uri.parse(receiptPath) else null

    /** Deletes the file backing [receiptPath], if any — used when a receipt is replaced or the
     * transaction it belongs to is permanently erased (`delete_my_data`/account deletion never
     * touches this directly; DPDP erasure happens server-side, but a removed receipt's local file
     * would otherwise orphan on-device storage). Best-effort: a missing/already-gone file is not an
     * error. */
    fun delete(receiptPath: String?): Boolean {
        val path = receiptPath?.let { Uri.parse(it).path } ?: return false
        val file = File(path)
        return !file.exists() || file.delete()
    }

    private companion object {
        const val RECEIPTS_DIR_NAME = "receipts"
    }
}
