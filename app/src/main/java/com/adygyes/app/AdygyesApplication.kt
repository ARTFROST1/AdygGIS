package com.adygyes.app

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import okhttp3.OkHttpClient
import com.adygyes.app.data.local.locale.LocaleManager
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import com.yandex.mapkit.MapKitFactory
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Main Application class for AdygGIS app.
 * Responsible for app-wide initialization.
 */
@HiltAndroidApp
class AdygyesApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var localeManager: LocaleManager
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        initializeTimber()
        initializeEmoji()
        initializeMapKit()
        initializeLocale()
    }
    
    override fun attachBaseContext(base: Context?) {
        if (base == null) {
            super.attachBaseContext(base)
            return
        }
        
        // Get saved language preference synchronously from SharedPreferences
        val prefs = base.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedLanguage = prefs.getString("language", LocaleManager.DEFAULT_LANGUAGE) ?: LocaleManager.DEFAULT_LANGUAGE
        
        // Apply saved language preference
        val locale = java.util.Locale(savedLanguage)
        java.util.Locale.setDefault(locale)
        
        val configuration = android.content.res.Configuration(base.resources.configuration)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            configuration.setLocales(android.os.LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
        }
        
        val contextWithLocale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            base.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            base.resources.updateConfiguration(configuration, base.resources.displayMetrics)
            base
        }
        
        super.attachBaseContext(contextWithLocale)
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handle configuration changes if needed
    }

    /**
     * Initialize Apple-style emoji provider
     */
    private fun initializeEmoji() {
        try {
            EmojiManager.install(IosEmojiProvider())
            if (BuildConfig.DEBUG) {
                Timber.d("iOS Emoji provider initialized successfully")
            } else {
                android.util.Log.d("AdygyesApp", "iOS Emoji provider initialized")
            }
        } catch (e: Exception) {
            val errorMsg = "Failed to initialize iOS Emoji provider: ${e.message}"
            if (BuildConfig.DEBUG) {
                Timber.e(e, errorMsg)
            } else {
                android.util.Log.e("AdygyesApp", errorMsg, e)
            }
            // Don't crash the app, continue with system emojis
        }
    }

    /**
     * Initialize Yandex MapKit SDK
     */
    private fun initializeMapKit() {
        try {
            val apiKey = BuildConfig.YANDEX_MAPKIT_API_KEY
            if (apiKey.isNotEmpty() && apiKey != "YOUR_API_KEY_HERE") {
                MapKitFactory.setApiKey(apiKey)
                if (BuildConfig.DEBUG) {
                    Timber.d("MapKit initialized successfully with API key")
                } else {
                    android.util.Log.d("AdygyesApp", "MapKit initialized")
                }
            } else {
                val errorMsg = "Yandex MapKit API key not configured. Please add YANDEX_MAPKIT_API_KEY to local.properties"
                if (BuildConfig.DEBUG) {
                    Timber.w(errorMsg)
                } else {
                    android.util.Log.w("AdygyesApp", errorMsg)
                }
            }
        } catch (e: Exception) {
            val errorMsg = "Failed to initialize MapKit: ${e.message}"
            if (BuildConfig.DEBUG) {
                Timber.e(e, errorMsg)
            } else {
                android.util.Log.e("AdygyesApp", errorMsg, e)
            }
            // Don't crash the app, continue without MapKit
        }
    }

    /**
     * Initialize Timber for logging in debug builds
     */
    private fun initializeTimber() {
        try {
            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
                Timber.d("Adygyes Application Started")
            } else {
                // For release, plant a simple tree or no-op tree
                Timber.plant(object : Timber.Tree() {
                    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                        // No-op for release
                    }
                })
            }
        } catch (e: Exception) {
            // Silently fail if Timber initialization fails
            android.util.Log.e("AdygyesApp", "Failed to initialize Timber", e)
        }
    }
    
    /**
     * Initialize locale settings
     */
    private fun initializeLocale() {
        // Ensure SharedPreferences has language set
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (!prefs.contains("language")) {
            prefs.edit().putString("language", LocaleManager.DEFAULT_LANGUAGE).commit()
            if (BuildConfig.DEBUG) {
                Timber.d("Initialized default language: ${LocaleManager.DEFAULT_LANGUAGE}")
            }
        }
        
        applicationScope.launch {
            try {
                val currentLanguage = localeManager.currentLanguage.first()
                if (BuildConfig.DEBUG) {
                    Timber.d("Application initialized with language: $currentLanguage")
                } else {
                    android.util.Log.d("AdygyesApp", "Language: $currentLanguage")
                }
            } catch (e: Exception) {
                val errorMsg = "Failed to initialize locale: ${e.message}"
                if (BuildConfig.DEBUG) {
                    Timber.e(e, errorMsg)
                } else {
                    android.util.Log.e("AdygyesApp", errorMsg, e)
                }
                // Don't crash, use default locale
            }
        }
    }

    /**
     * Create Coil ImageLoader with custom configuration
     */
    override fun newImageLoader(): ImageLoader {
        // Custom OkHttpClient with headers for external images (Yandex, etc.)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "AdygGIS Android App")
                    .addHeader("Accept", "image/webp,image/png,image/jpeg,image/*,*/*")
                    .build()
                chain.proceed(request)
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .logger(if (BuildConfig.DEBUG) DebugLogger() else null)
            .build()
    }
}
