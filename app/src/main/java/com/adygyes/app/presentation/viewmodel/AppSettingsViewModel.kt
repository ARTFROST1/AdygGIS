package com.adygyes.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adygyes.app.data.local.preferences.AppSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for screens that display app settings/contact information
 * 
 * Provides reactive access to app settings synced from Supabase Admin Panel.
 * Settings include contacts, store links, app info, and developer details.
 */
@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val appSettingsManager: AppSettingsManager
) : ViewModel() {
    
    /**
     * Current app settings as StateFlow
     * Automatically updates when settings are synced
     */
    val settings: StateFlow<AppSettingsManager.AppSettings> = appSettingsManager.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettingsManager.AppSettings()
        )
}
