package com.adygyes.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for sync_metadata table (tombstones)
 * 
 * Used to track deleted/unpublished attractions for delta sync.
 * When an attraction is deleted or unpublished, a record is created
 * in sync_metadata so mobile clients can remove it from local cache.
 */
@Serializable
data class SyncMetadataDto(
    @SerialName("id")
    val id: String? = null,
    
    @SerialName("entity_type")
    val entityType: String = "attraction",
    
    @SerialName("entity_id")
    val entityId: String,
    
    @SerialName("action")
    val action: String? = null, // "DELETE" or "UNPUBLISH"
    
    @SerialName("deleted_at")
    val deletedAt: String? = null
)
