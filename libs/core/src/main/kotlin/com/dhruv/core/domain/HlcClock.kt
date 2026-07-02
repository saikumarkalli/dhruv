package com.dhruv.core.domain

import android.content.Context
import android.provider.Settings
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Hybrid Logical Clock for Last-Write-Wins conflict resolution (ADR-0004).
 *
 * Implements the Kulkarni-Demirbas algorithm:
 *   l' = max(wallClock, lastL)
 *   if l' == lastL then c' = lastC + 1 else c' = 0
 *
 * Stamp format: "${epochMillis}-${counter4digits}-${nodeId8chars}"
 * Example:      "1718273645123-0000-a1b2c3d4"
 */
object HlcClock {
    private val lastL = AtomicLong(0L)
    private val lastC = AtomicInteger(0)
    private var nodeId: String = "00000000"

    fun init(context: Context) {
        nodeId =
            (
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                    ?: "00000000"
            ).take(8).padEnd(8, '0')
    }

    @Synchronized
    fun now(): String {
        val wall = System.currentTimeMillis()
        val prevL = lastL.get()
        val newL = maxOf(wall, prevL)
        val newC = if (newL == prevL) lastC.incrementAndGet() else 0
        lastL.set(newL)
        lastC.set(newC)
        return "$newL-${newC.toString().padStart(4, '0')}-$nodeId"
    }

    /** Advance clock on receive (merge remote stamp). */
    @Synchronized
    fun receive(remote: String): String {
        val remoteL = remote.substringBefore('-').toLongOrNull() ?: return now()
        val wall = System.currentTimeMillis()
        val prevL = lastL.get()
        val newL = maxOf(wall, remoteL, prevL)
        val newC =
            when (newL) {
                remoteL -> lastC.get() + 1
                prevL -> lastC.get() + 1
                else -> 0
            }
        lastL.set(newL)
        lastC.set(newC)
        return "$newL-${newC.toString().padStart(4, '0')}-$nodeId"
    }
}
