package com.adygyes.app.di.module

import android.content.Context
import com.adygyes.app.data.local.dao.AttractionDao
import com.adygyes.app.data.local.preferences.PreferencesManager
import com.adygyes.app.data.remote.SupabaseRemoteDataSource
import com.adygyes.app.data.remote.api.SupabaseApiService
import com.adygyes.app.data.sync.NetworkMonitor
import com.adygyes.app.data.sync.SyncManager
import com.adygyes.app.data.sync.SyncService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for sync-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {
    
    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context
    ): NetworkMonitor {
        return NetworkMonitor(context)
    }
    
    @Provides
    @Singleton
    fun provideSupabaseRemoteDataSource(
        apiService: SupabaseApiService
    ): SupabaseRemoteDataSource {
        return SupabaseRemoteDataSource(apiService)
    }
    
    @Provides
    @Singleton
    fun provideSyncService(
        remoteDataSource: SupabaseRemoteDataSource,
        attractionDao: AttractionDao,
        preferencesManager: PreferencesManager
    ): SyncService {
        return SyncService(remoteDataSource, attractionDao, preferencesManager)
    }
    
    @Provides
    @Singleton
    fun provideSyncManager(
        @ApplicationContext context: Context,
        syncService: SyncService,
        networkMonitor: NetworkMonitor
    ): SyncManager {
        return SyncManager(context, syncService, networkMonitor)
    }
}
