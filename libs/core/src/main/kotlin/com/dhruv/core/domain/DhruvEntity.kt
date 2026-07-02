package com.dhruv.core.domain

/**
 * Base contract for all syncable Dhruv entities. Source of truth: platform/contracts/DhruvEntity.kt.
 * Any change must land in platform/contracts first, then here.
 *
 * VAULT ENTITIES DO NOT IMPLEMENT THIS INTERFACE.
 */
interface DhruvEntity {
    val id: String // UUID, generated client-side
    val userId: String // "local" until Dhruv ID ships; must be INDEXED
    val createdAt: Long // epoch millis, informational
    val updatedAt: Long // epoch millis, informational/display
    val hlc: String // Hybrid Logical Clock stamp — authoritative for LWW ordering
    val isSynced: Boolean
    val isDeleted: Boolean // soft-delete UX flag; real erasure handled by tombstone GC
}
