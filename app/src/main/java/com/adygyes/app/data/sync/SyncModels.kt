package com.adygyes.app.data.sync

/**
 * Result of a sync operation
 */
data class SyncResult(
    val success: Boolean,
    val added: Int = 0,
    val updated: Int = 0,
    val deleted: Int = 0,
    val errorMessage: String? = null
) {
    val totalChanges: Int get() = added + updated + deleted
    val hasChanges: Boolean get() = totalChanges > 0
}

/**
 * Sync state for UI observation
 */
sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data class Success(val result: SyncResult) : SyncState()
    data class Error(val message: String) : SyncState()
}
