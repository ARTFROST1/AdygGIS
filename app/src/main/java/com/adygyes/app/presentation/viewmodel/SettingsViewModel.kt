package com.adygyes.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adygyes.app.data.local.preferences.PreferencesManager
import com.adygyes.app.data.local.cache.CacheManager
import com.adygyes.app.data.local.locale.LocaleManager
import com.adygyes.app.domain.usecase.DataSyncUseCase
import com.adygyes.app.domain.usecase.NetworkUseCase
import com.adygyes.app.domain.usecase.NetworkStatus
import com.adygyes.app.domain.usecase.SyncProgress
import com.adygyes.app.domain.usecase.SyncInfo
import com.adygyes.app.domain.usecase.UpdateCheckResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the settings screen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val cacheManager: CacheManager,
    private val localeManager: LocaleManager,
    private val dataSyncUseCase: DataSyncUseCase,
    private val networkUseCase: NetworkUseCase,
    private val imageCacheManager: com.adygyes.app.data.local.cache.ImageCacheManager,
    private val attractionDao: com.adygyes.app.data.local.dao.AttractionDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    
    private val _syncProgress = MutableStateFlow<SyncProgress?>(null)
    val syncProgress: StateFlow<SyncProgress?> = _syncProgress.asStateFlow()
    
    private val _syncInfo = MutableStateFlow<SyncInfo?>(null)
    val syncInfo: StateFlow<SyncInfo?> = _syncInfo.asStateFlow()
    
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    
    private val _isClearing = MutableStateFlow(false)
    val isClearing: StateFlow<Boolean> = _isClearing.asStateFlow()
    
    private val _lastSyncTimestamp = MutableStateFlow<String?>(null)
    val lastSyncTimestamp: StateFlow<String?> = _lastSyncTimestamp.asStateFlow()
    
    init {
        loadSettings()
        loadSyncInfo()
        observeNetworkStatus()
        loadLastSyncTimestamp()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            // Load preferences using the actual PreferencesManager API
            preferencesManager.userPreferencesFlow.collect { preferences ->
                _uiState.value = SettingsUiState(
                    theme = when (preferences.themeMode.lowercase()) {
                        "dark" -> Theme.DARK
                        "light" -> Theme.LIGHT
                        else -> Theme.SYSTEM
                    },
                    language = when (preferences.language) {
                        LocaleManager.LANGUAGE_RUSSIAN -> Language.RUSSIAN
                        LocaleManager.LANGUAGE_ENGLISH -> Language.ENGLISH
                        else -> Language.RUSSIAN // Default to Russian
                    },
                    showUserLocation = preferences.autoCenterLocation,
                    pushNotifications = preferences.notificationEnabled,
                    appVersion = "1.0.0"
                )
            }
        }
    }
    
    fun setTheme(theme: Theme) {
        viewModelScope.launch {
            when (theme) {
                Theme.LIGHT -> preferencesManager.updateThemeMode("light")
                Theme.DARK -> preferencesManager.updateThemeMode("dark")
                Theme.SYSTEM -> preferencesManager.updateThemeMode("system")
            }
        }
    }
    
    fun setLanguage(language: Language) {
        viewModelScope.launch {
            val langCode = when (language) {
                Language.RUSSIAN -> LocaleManager.LANGUAGE_RUSSIAN
                Language.ENGLISH -> LocaleManager.LANGUAGE_ENGLISH
            }
            localeManager.setLanguage(langCode)
        }
    }
    
    fun setShowUserLocation(show: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateAutoCenterLocation(show)
        }
    }
    
    
    fun setPushNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateNotificationEnabled(enabled)
        }
    }
    
    
    private fun loadSyncInfo() {
        viewModelScope.launch {
            val info = dataSyncUseCase.getLastSyncInfo()
            _syncInfo.value = info
        }
    }
    
    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkUseCase.getNetworkStatus().collect { status ->
                _uiState.update { 
                    it.copy(isOnline = status is NetworkStatus.Connected)
                }
            }
        }
    }
    
    fun syncData() {
        viewModelScope.launch {
            if (_isSyncing.value) return@launch
            _isSyncing.value = true
            try {
                dataSyncUseCase.syncData().collect { progress ->
                    _syncProgress.value = progress

                    // Update sync info when completed
                    if (progress is SyncProgress.Completed || progress is SyncProgress.Error) {
                        loadSyncInfo()
                        loadLastSyncTimestamp()
                    }
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }
    
    fun checkForUpdates() {
        viewModelScope.launch {
            val result = dataSyncUseCase.checkForUpdates()
            // Handle update check result
            _uiState.update { 
                it.copy(
                    hasUpdatesAvailable = result is UpdateCheckResult.UpdateAvailable
                )
            }
        }
    }
    
    fun forceRefresh() {
        viewModelScope.launch {
            if (_isSyncing.value) return@launch
            _isSyncing.value = true
            _syncProgress.value = SyncProgress.Started
            try {
                val success = dataSyncUseCase.forceRefresh()
                _syncProgress.value = if (success) {
                    SyncProgress.Completed("Data refreshed successfully")
                } else {
                    SyncProgress.Error("Failed to refresh data")
                }
                loadSyncInfo()
                loadLastSyncTimestamp()
            } finally {
                _isSyncing.value = false
            }
        }
    }
    
    fun clearSyncProgress() {
        _syncProgress.value = null
    }
    
    private fun loadLastSyncTimestamp() {
        viewModelScope.launch {
            val timestamp = preferencesManager.getLastSyncTimestamp()
            _lastSyncTimestamp.value = timestamp
        }
    }
    
    /**
     * Perform delta sync with Supabase
     * Получает только изменения с момента последней синхронизации
     */
    fun performSync() {
        syncData()
    }
    
    /**
     * Clear all cache (Room + images + sync timestamps).
     * User needs to manually sync or restart app to reload data.
     * Очищает все данные. Для загрузки нужно вручную синхронизировать или перезапустить приложение.
     */
    fun clearCache() {
        viewModelScope.launch {
            try {
                _isClearing.value = true
                
                timber.log.Timber.d("🗑️ Starting cache clear...")
                
                // 1. Clear Room database
                timber.log.Timber.d("🗑️ Clearing Room database...")
                attractionDao.deleteAll()
                
                // 2. Clear image caches (memory + disk)
                timber.log.Timber.d("🗑️ Clearing image caches...")
                imageCacheManager.clearAllCache()
                
                // 3. Clear preferences (reset sync timestamp)
                timber.log.Timber.d("🗑️ Resetting sync timestamp...")
                cacheManager.clearAllCache()
                
                loadSyncInfo()
                loadLastSyncTimestamp()
                timber.log.Timber.d("✅ Cache cleared successfully")
            } catch (e: Exception) {
                timber.log.Timber.e(e, "❌ Failed to clear cache")
            } finally {
                _isClearing.value = false
            }
        }
    }
    
    /**
     * Get cache size info for display
     */
    suspend fun getCacheSize(): String {
        val cacheInfo = imageCacheManager.getCacheInfo()
        val totalSizeMB = (cacheInfo.diskSizeBytes + cacheInfo.memorySizeBytes) / (1024f * 1024f)
        return String.format("%.1f МБ", totalSizeMB)
    }
    
    /**
     * Handle version click
     */
    fun onVersionClick() {
        // Просто обработка клика по версии
    }
    
    enum class Theme(val displayName: String) {
        LIGHT("Light"),
        DARK("Dark"),
        SYSTEM("System Default")
    }
    
    enum class Language(val displayName: String) {
        RUSSIAN("Русский"),
        ENGLISH("English")
    }
    
    data class SettingsUiState(
        val theme: Theme = Theme.SYSTEM,
        val language: Language = Language.RUSSIAN,
        val showUserLocation: Boolean = true,
        val pushNotifications: Boolean = true,
        val appVersion: String = "1.0.0",
        val isOnline: Boolean = true,
        val hasUpdatesAvailable: Boolean = false
    )
}
