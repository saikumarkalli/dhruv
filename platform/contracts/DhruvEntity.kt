package com.dhruv.platform.contracts

/**
 * Base contract for all syncable Dhruv entities.
 *
 * Rules:
 *  - id is a UUID string, generated client-side.
 *  - userId is "local" until Dhruv ID (Firebase Auth) ships; it is INDEXED so the one-time
 *    migration that rewrites "local" -> real userId is cheap.
 *  - Soft-delete is a UX state only. A guaranteed hard-delete (DPDP 7-day erasure / tombstone GC)
 *    runs separately; see PLATFORM.md §5 and §8.
 *  - Conflict resolution is HLC-based Last-Write-Wins; `hlc` is the ordering stamp, NOT `updatedAt`.
 *  - VAULT ENTITIES DO NOT IMPLEMENT THIS INTERFACE.
 *
 * Any change to this contract must land here (platform/contracts) via PR FIRST,
 * then be implemented in :libs:core.
 */
interface DhruvEntity {
    val id: String          // UUID
    val userId: String      // "local" until Dhruv ID ships; indexed
    val createdAt: Long     // epoch millis, informational
    val updatedAt: Long     // epoch millis, informational/display
    val hlc: String         // Hybrid Logical Clock stamp — authoritative for LWW ordering
    val isSynced: Boolean
    val isDeleted: Boolean  // soft-delete UX flag; real erasure handled by tombstone GC
}
