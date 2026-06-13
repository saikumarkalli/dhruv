package com.dhruv.core.domain

/**
 * Abstract base for Room entities that implement DhruvEntity.
 * Subclass and annotate with @Entity; add @Index(value = ["userId"]) on the @Entity annotation.
 */
abstract class BaseEntity(
    override val id: String,
    override val userId: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val hlc: String,
    override val isSynced: Boolean = false,
    override val isDeleted: Boolean = false,
) : DhruvEntity
