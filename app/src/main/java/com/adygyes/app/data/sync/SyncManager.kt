package com.adygyes.app.data.sync

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for orchestrating sync operations
 * 
 * Responsibilities:
 * - Handles sync lifecycle (start, cancel, complete)
 * - Observes network changes and triggers sync on reconnect
 * - Provides sync state to UI via StateFlow
 * - Prevents concurrent sync operations
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncService: SyncService,
    private val networkMonitor: NetworkMonitor
) : DefaultLifecycleObserver {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    private var networkJob: Job? = null
    private var syncJob: Job? = null
    
    // Flag to track if initial sync has been done
    private var hasInitialSyncBeenAttempted = false
    
    /**
     * Start observing network changes when app is in foreground
     */
    override fun onStart(owner: LifecycleOwner) {
        startNetworkObserver()
    }
    
    /**
     * Stop observing when app goes to background
     */
    override fun onStop(owner: LifecycleOwner) {
        networkJob?.cancel()
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        networkJob?.cancel()
        syncJob?.cancel()
        scope.cancel()
    }
    
    /**
     * Start observing network and trigger sync on reconnect
     */
    private fun startNetworkObserver() {
        networkJob?.cancel()
        networkJob = scope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                Timber.d("📶 Network status: ${if (isOnline) "online" else "offline"}")
                
                if (isOnline && _syncState.value is SyncState.Idle) {
                    if (!hasInitialSyncBeenAttempted) {
                        Timber.d("📶 Network connected, starting initial sync...")
                        hasInitialSyncBeenAttempted = true
                        performSyncInternal()
                    }
                }
            }
        }
    }
    
    /**
     * Trigger sync (delta sync by default)
     * 
     * Call this from UI to manually trigger sync.
     */
    suspend fun performSync(): SyncResult {
        return performSyncInternal()
    }
    
    /**
     * Internal sync implementation
     */
    private suspend fun performSyncInternal(): SyncResult {
        if (_syncState.value is SyncState.Syncing) {
            Timber.d("⏳ Sync already in progress, skipping...")
            return SyncResult(success = false, errorMessage = "Sync already in progress")
        }
        
        _syncState.value = SyncState.Syncing
        
        return try {
            val result = syncService.performSync()
            
            _syncState.value = if (result.success) {
                SyncState.Success(result)
            } else {
                SyncState.Error(result.errorMessage ?: "Unknown error")
            }
            
            // Reset to idle after delay so UI can show result
            scheduleResetToIdle()
            
            result
        } catch (e: Exception) {
            val error = e.message ?: "Unknown error"
            _syncState.value = SyncState.Error(error)
            scheduleResetToIdle()
            SyncResult(success = false, errorMessage = error)
        }
    }
    
    /**
     * Force full sync (reload all data)
     * 
     * Use this when user explicitly requests refresh.
     */
    suspend fun forceFullSync(): SyncResult {
        if (_syncState.value is SyncState.Syncing) {
            return SyncResult(success = false, errorMessage = "Sync already in progress")
        }
        
        _syncState.value = SyncState.Syncing
        
        return try {
            val result = syncService.forceFullSync()
            
            _syncState.value = if (result.success) {
                SyncState.Success(result)
            } else {
                SyncState.Error(result.errorMessage ?: "Unknown error")
            }
            
            scheduleResetToIdle()
            
            result
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            scheduleResetToIdle()
            SyncResult(success = false, errorMessage = e.message)
        }
    }
    
    /**
     * Schedule reset to idle after showing result
     */
    private fun scheduleResetToIdle() {
        scope.launch {
            delay(3000) // Show result for 3 seconds
            if (_syncState.value !is SyncState.Syncing) {
                _syncState.value = SyncState.Idle
            }
        }
    }
    
    /**
     * Reset sync state to idle immediately
     */
    fun resetState() {
        _syncState.value = SyncState.Idle
    }
    
    /**
     * Check if network is available
     */
    fun isOnline(): Boolean = networkMonitor.isCurrentlyOnline()
}
