package com.adygyes.app.di.module

import android.content.Context
import com.adygyes.app.data.local.dao.AttractionDao
import com.adygyes.app.data.local.dao.ReviewDao
import com.adygyes.app.data.local.preferences.AppSettingsManager
import com.adygyes.app.data.local.preferences.PreferencesManager
import com.adygyes.app.data.remote.ReviewsRemoteDataSource
import com.adygyes.app.data.remote.SupabaseRemoteDataSource
import com.adygyes.app.data.remote.api.SupabaseApiService
import com.adygyes.app.data.sync.NetworkMonitor
import com.adygyes.app.data.sync.ReviewSyncService
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
    fun provideReviewsRemoteDataSource(
        apiService: SupabaseApiService
    ): ReviewsRemoteDataSource {
        return ReviewsRemoteDataSource(apiService)
    }
    
    @Provides
    @Singleton
    fun provideReviewSyncService(
        reviewsRemoteDataSource: ReviewsRemoteDataSource,
        reviewDao: ReviewDao
    ): ReviewSyncService {
        return ReviewSyncService(reviewsRemoteDataSource, reviewDao)
    }
    
    @Provides
    @Singleton
    fun provideSyncService(
        @ApplicationContext context: Context,
        remoteDataSource: SupabaseRemoteDataSource,
        attractionDao: AttractionDao,
        preferencesManager: PreferencesManager,
        appSettingsManager: AppSettingsManager,
        networkUseCase: com.adygyes.app.domain.usecase.NetworkUseCase,
        reviewSyncService: ReviewSyncService
    ): SyncService {
        return SyncService(
            context,
            remoteDataSource,
            attractionDao,
            preferencesManager,
            appSettingsManager,
            networkUseCase,
            reviewSyncService
        )
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
