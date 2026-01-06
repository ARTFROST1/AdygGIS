package com.adygyes.app.data.remote.config

import com.adygyes.app.BuildConfig

/**
 * Supabase configuration for API access
 * 
 * Credentials are loaded from BuildConfig (set in local.properties).
 * - SUPABASE_URL: Project URL (e.g., https://xxxxx.supabase.co)
 * - SUPABASE_ANON_KEY: Public anon key (safe for client, protected by RLS)
 */
object SupabaseConfig {
    /**
     * Supabase project URL
     */
    val BASE_URL: String = BuildConfig.SUPABASE_URL
    
    /**
     * Supabase anon key for public read access
     * This key is safe to include in client apps (RLS protects data)
     */
    val ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY
    
    /**
     * REST API endpoint paths
     */
    object Endpoints {
        const val ATTRACTIONS = "rest/v1/attractions"
        const val SYNC_METADATA = "rest/v1/sync_metadata"
    }
    
    /**
     * HTTP header names for Supabase API
     */
    object Headers {
        const val API_KEY = "apikey"
        const val AUTHORIZATION = "Authorization"
        const val CONTENT_TYPE = "Content-Type"
        const val PREFER = "Prefer"
    }
    
    /**
     * Header values
     */
    object HeaderValues {
        const val APPLICATION_JSON = "application/json"
        const val BEARER_PREFIX = "Bearer "
        const val RETURN_REPRESENTATION = "return=representation"
    }
    
    /**
     * Check if Supabase is properly configured
     */
    fun isConfigured(): Boolean {
        return BASE_URL.isNotBlank() && ANON_KEY.isNotBlank()
    }
}
